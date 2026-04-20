package rendering;

import application.Application;
import application.RenderMode;
import application.Window;
import pathfinding.AStar;
import pathfinding.Node;
import terrain.Chunk;
import terrain.ChunkManager;
import terrain.HeightMap;

import static org.lwjgl.opengl.GL11.*;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main renderer responsible for drawing each frame.
 * Handles all OpenGL draw calls including such as terrain, path and camera.
 * 
 * <p>
 * Uses a single {@link Shader} program for both terrain and path rendering.
 * Path is rendered
 * in a separate color defined by the {@code pathColor} uniform.
 * </p>
 */
public class Renderer {
    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);

    /** Shader program for rendering terrain and path */
    private Shader shader;

    /** Camera providing the view and projection matrices */
    private final Camera camera;

    /** Chunk manager providing visible terrain chunks every frame */
    private final ChunkManager chunkManager;

    /** A* instance for pathfinding */
    private final AStar aStar;

    /** Window instance */
    private final Window window;

    /** Height map instance */
    private final HeightMap heightMap;

    /**
     * OpenGL texture to store nodes in current path (uniform values aren't suitable
     * for this much data)
     */
    private Texture visitedTex;

    /** Pre-computed slope-angle heatmap texture */
    private Texture heatmapTex;

    /**
     * Create static model matrix for now since the grid is already in world
     * coordinates this is not needed for now.
     */
    private static final Matrix4f model = new Matrix4f().identity();

    /** Rendering mode */
    private RenderMode renderMode = RenderMode.NORMAL;

    /** Is wireframe rendering is enabled */
    private boolean wireframe = false;

    /** Sun azimuth angle */
    private int sunAzimuth = 180;

    /** Sun elevation angle */
    private int sunElevation = 30;

    /**
     * Constructs a new {@code Renderer}.
     * 
     * @param camera       Camera providing view and projection matrices
     * @param chunkManager Chunk manager providing visible terrain chunks
     * @param aStar        A* instance for current path
     */
    public Renderer(Camera camera, ChunkManager chunkManager, AStar aStar, Window window, HeightMap heightMap) {
        this.camera = camera;
        this.chunkManager = chunkManager;
        this.aStar = aStar;
        this.window = window;
        this.heightMap = heightMap;
        this.init();
    }

    /**
     * Initializes the shader program. Must be called after OpenGL context
     * is created and before first call to {@link #render}!
     */
    public void init() {
        this.shader = new Shader("/chunk.vert", "/chunk.frag");
        this.visitedTex = new Texture(this.heightMap.getWidth(), this.heightMap.getHeight());
        this.heatmapTex = new Texture(this.heightMap.getWidth(), this.heightMap.getHeight());
        this.buildHeatmapTexture();
        this.camera.setMouseMode(this.window.getHandle());
        logger.info("Renderer initiated and shader program created");
    }

    /**
     * Computes slope angles for every cell in the heightmap and stores them
     * into the heatmap texture: 0 = green (0-25 degrees), 85 = yellow (25-35
     * degrees),
     * 170 = red (35-45 degrees), 255 = black (45-inf degrees).
     */
    private void buildHeatmapTexture() {
        // Calculate coordinates
        int w = this.heightMap.getWidth();
        int h = this.heightMap.getHeight();
        int halfW = w / 2;
        int halfH = h / 2;

        // Clear the texture buffer
        this.heatmapTex.clear();

        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                int logicalX = x - halfW;
                int logicalZ = z - halfH;
                double angle = this.heightMap.getSlopeAngle(logicalX, logicalZ);

                // Store the slope angle in the texture
                byte value;
                if (angle < 25.0)
                    value = (byte) 0;
                else if (angle < 35.0)
                    value = (byte) 85;
                else if (angle < 45.0)
                    value = (byte) 170;
                else
                    value = (byte) 255;

                this.heatmapTex.setTexel(x, z, value);
            }
        }

        this.heatmapTex.upload();
        logger.info("Heatmap texture built");
    }

    /**
     * Renders a single frame, called once per iteration of the application loop.
     */
    public void render() {
        // Bind active shader program
        this.shader.bind();

        // Upload chunks to GPU
        this.chunkManager.update(this.camera);

        // Calculate MVP
        Matrix4f MVP = this.buildMVP(model);
        this.shader.setMat4("uMVP", MVP);

        // Set wireframe uniform
        this.shader.setInt("uWireframe", this.wireframe ? 1 : 0);

        HeightMap heightMap = this.chunkManager.getHeightMap();
        this.shader.setFloat("uElevationMin", heightMap.getDataMinElevation());
        this.shader.setFloat("uElevationMax", heightMap.getDataMaxElevation());

        // Set lighting uniform, and calculate lighting direction from sun position
        float azimuthRadians = (float) Math.toRadians(this.sunAzimuth);
        float elevationRadians = (float) Math.toRadians(this.sunElevation);

        // Convert spherical coordinates to cartesian
        float x = (float) (Math.cos(elevationRadians) * Math.sin(azimuthRadians));
        float y = (float) Math.sin(elevationRadians);
        float z = (float) (Math.cos(elevationRadians) * Math.cos(azimuthRadians));

        // Flip the vector so the direction is from surface to sky
        Vector3f lighting = new Vector3f(x, y, z).normalize().mul(-1);
        this.shader.setVec3("uLightPos", lighting);

        // Set camera position uniform
        this.shader.setVec3("uCameraPos", this.camera.getPosition());

        // Render A*
        renderPath();

        // Bind heatmap texture to pos 2
        this.heatmapTex.bind(2);
        this.shader.setInt("uHeatmapTex", 2);

        // Clear framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Apply wireframe mode only for terrain rendering
        if (this.wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glDisable(GL_CULL_FACE);
        }

        // Render terrain
        renderTerrain();

        // Reset to fill so ImGui renders correctly
        if (this.wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glEnable(GL_CULL_FACE);
        }

        // Unbind programs
        this.visitedTex.unbind();
        this.heatmapTex.unbind();
        this.shader.unbind();
    }

    /**
     * Renders all visible terrain chunks provided by {@link ChunkManager}.
     */
    private void renderTerrain() {
        // Set render mode uniform value to inform the fragment shader how the terrain
        // should be rendered
        this.shader.setInt("uRenderMode", this.renderMode.ordinal());

        for (Chunk chunk : this.chunkManager.getLoadedChunks()) {
            chunk.render();
        }
    }

    /**
     * Renders the current A* iterator stage
     */
    private void renderPath() {
        int width = this.heightMap.getWidth() / 2;
        int height = this.heightMap.getHeight() / 2;

        // Update visted texture if A* iterator is active
        if (this.aStar.isRunning()) {
            this.visitedTex.clear();
            for (Node node : this.aStar.getUnModifiableOpenSet()) {
                int texX = (int) node.getX() + width;
                int texZ = (int) node.getZ() + height;

                // Set texel color to ~0.33 (used for A* stage detection in fragment shader)
                this.visitedTex.setTexel(texX, texZ, (byte) 85);
            }

            this.visitedTex.upload();
        } else if (!this.aStar.isRunning() && this.aStar.getPath() != null) {
            // Render completed path
            List<Node> path = this.aStar.getPath();
            this.visitedTex.clear();

            for (Node node : path) {
                int texX = (int) node.getX() + width;
                int texZ = (int) node.getZ() + height;

                // Set texel color to 1.0 (used for A* stage detection in fragment shader)
                this.visitedTex.setTexel(texX, texZ, (byte) 255);
            }

            this.visitedTex.upload();
        }

        // Bind visited texture to pos 1 and set uniform
        this.visitedTex.bind(1);
        this.shader.setInt("uVisitedTex", 1);
    }

    /**
     * Builds the model-view-projection matrix for a given model transformation.
     * 
     * @param model Model matrix of the object being rendered
     * @return Combined MVP matrix ({@code projection * view * model})
     */
    private Matrix4f buildMVP(Matrix4f model) {
        // Create new matrix because JOML modifies the original matrix
        return new Matrix4f(this.camera.getProjectionMatrix())
                .mul(this.camera.getViewMatrix())
                .mul(model);
    }

    /**
     * @return Camera used for view/projection and movement
     */
    public Camera getCamera() {
        return this.camera;
    }

    /**
     * @return Current shader instance
     */
    public Shader getShader() {
        return this.shader;
    }

    /**
     * Sets active render mode
     * 
     * @param mode Which render mode to use
     */
    public void setRenderMode(RenderMode mode) {
        this.renderMode = mode;
    }

    /**
     * Sets sun azimuth degrees
     * 
     * @param azimuth Sun azimuth angle
     */
    public void setSunAzimuth(int azimuth) {
        this.sunAzimuth = azimuth;
    }

    /**
     * Sets sun elevation degrees
     * 
     * @param elevation Sun elevation angle
     */
    public void setSunElevation(int elevation) {
        this.sunElevation = elevation;
    }

    /**
     * Sets wireframe mode
     * 
     * @param wf Wireframe on/off (true/false)
     */
    public void setWireframe(boolean wf) {
        this.wireframe = wf;
    }

    /**
     * Releases all OpenGL resources held by the renderer.
     * Called by {@link Application#cleanUp()} on shutdown.
     */
    public void cleanUp() {
        if (this.visitedTex != null) {
            this.visitedTex.dispose();
            logger.info("Visited texture cleaned up");
        }

        if (this.heatmapTex != null) {
            this.heatmapTex.dispose();
            logger.info("Heatmap texture cleaned up");
        }

        if (this.shader != null) {
            this.shader.dispose();
            logger.info("Renderer shader resources cleaned up");
        }
    }
}
