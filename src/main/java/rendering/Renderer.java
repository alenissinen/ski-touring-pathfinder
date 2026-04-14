package rendering;

import application.Application;
import application.Window;
import pathfinding.AStar;
import pathfinding.Node;
import terrain.Chunk;
import terrain.ChunkManager;
import terrain.HeightMap;

import static org.lwjgl.opengl.GL11.*;

import java.util.List;

import org.joml.Matrix4f;
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

    /**
     * Create static model matrix for now since the grid is already in world
     * coordinates this is not needed for now.
     */
    private static final Matrix4f model = new Matrix4f().identity();

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
        this.visitedTex = new Texture(this.heightMap.getWidth(), heightMap.getHeight());
        this.camera.setMouseMode(this.window.getHandle());
        logger.info("Renderer initiated and shader program created");
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

        HeightMap heightMap = this.chunkManager.getHeightMap();
        this.shader.setFloat("uElevationMin", heightMap.getDataMinElevation());
        this.shader.setFloat("uElevationMax", heightMap.getDataMaxElevation());

        // Render A*
        renderPath();

        // Clear framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Render terrain
        renderTerrain();

        // Unbind programs
        this.visitedTex.unbind();
        this.shader.unbind();
    }

    /**
     * Renders all visible terrain chunks provided by {@link ChunkManager}.
     */
    private void renderTerrain() {
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

                // Set texel color to 1.0 (used for A* stage detection in fragment shader)
                this.visitedTex.setTexel(texX, texZ, (byte) 255);
            }

            this.visitedTex.upload();
        } else if (!this.aStar.isRunning() && this.aStar.getPath() != null) {
            // Render completed path
            List<Node> path = this.aStar.getPath();
            this.visitedTex.clear();

            for (Node node : path) {
                int texX = (int) node.getX() + width;
                int texZ = (int) node.getZ() + height;

                // Set texel color to ~0.5 (used for A* stage detection in fragment shader)
                this.visitedTex.setTexel(texX, texZ, (byte) 127);
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
     * Releases all OpenGL resources held by the renderer.
     * Called by {@link Application#cleanUp()} on shutdown.
     */
    public void cleanUp() {
        if (this.visitedTex != null) {
            this.visitedTex.dispose();
            logger.info("Renderer texture resources cleaned up");
        }

        if (this.shader != null) {
            this.shader.dispose();
            logger.info("Renderer shader resources cleaned up");
        }
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
}
