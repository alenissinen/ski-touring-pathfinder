package rendering;

import application.Application;
import pathfinding.AStar;
import terrain.ChunkManager;

import static org.lwjgl.opengl.GL11.*;

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
    public Renderer(Camera camera, ChunkManager chunkManager, AStar aStar) {
        this.camera = camera;
        this.chunkManager = chunkManager;
        this.aStar = aStar;
        this.init();
    }

    /**
     * Initializes the shader program. Must be called after OpenGL context
     * is created and before first call to {@link #render(float)}!
     */
    public void init() {
        this.shader = new Shader("/chunk.vert", "/chunk.frag");
        logger.info("Renderer initiated and shader program created");
    }

    /**
     * Renders a single frame, called once per iteration of the application loop.
     * 
     * @param deltaTime Time in seconds since the last render
     */
    public void render(double deltaTime) {
        // Bind active shader program
        this.shader.bind();

        // Calculate MVP
        Matrix4f MVP = this.buildMVP(model);
        this.shader.setMat4("MVP", MVP);

        // Clear framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderTerrain();
        renderPath();

        // Unbind shader program
        this.shader.unbind();
    }

    /**
     * Renders all visible terrain chunks provided by {@link ChunkManager}.
     */
    private void renderTerrain() {
    }

    /**
     * Renders the current path provided by {@link AStar#getPath()}.
     */
    private void renderPath() {
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
    }
}
