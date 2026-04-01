package application;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import exceptions.HeightMapParseException;
import input.InputHandler;
import rendering.Renderer;
import terrain.ChunkManager;
import terrain.HeightMap;
import rendering.Camera;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import org.joml.Vector3f;

/**
 * Main application class responsible for the program lifecycle. Manages
 * initialization,
 * main loop and cleanup.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    /** Store start time to calculate how long intialization took */
    private static final long STARTUP_TIME = System.currentTimeMillis();

    /** Window object */
    private final Window window;

    /** Application config */
    private final Config config;

    /** Renderer object */
    private Renderer renderer;

    /** Input handler */
    private InputHandler inputHandler;

    /**
     * Constructor to create new application
     * 
     * @param config Application config (see {@link Config})
     */
    public Application(Config config) {
        this.config = config;
        this.window = new Window(config);
    }

    /**
     * Starting point of the application
     */
    public void run() {
        // Initialize GLFW and start render loop
        try {
            init();
        } catch (HeightMapParseException | IllegalStateException e) {
            e.printStackTrace();
        }

        loop();

        // Clean resources before shutting down
        cleanUp();
    }

    /**
     * Setups necessary callbacks, initializes GLFW, and creates the window
     */
    public void init() throws HeightMapParseException, IllegalStateException {
        // Create an error callback, prints error message in stderr
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            logger.error("Failed to initialize GLFW");
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        logger.info("GLFW initialized");

        // Create GLFW window
        this.window.create();

        GL.createCapabilities();

        // Create north and south height maps and merge them
        HeightMap north = HeightMap.fromAsciiFile("/T5311B.asc");
        HeightMap south = HeightMap.fromAsciiFile("/T5311A.asc");
        HeightMap merged = HeightMap.merge(north, south);

        logger.info("Height maps created and merged");

        Camera camera = new Camera(new Vector3f(), 68.0f, (float) this.config.getWidth() / this.config.getHeight(),
                2.0f);
        ChunkManager chunkManager = new ChunkManager(merged, config.getRenderDistance());
        this.renderer = new Renderer(camera, chunkManager, null, this.window);
        this.inputHandler = new InputHandler(this.window.getHandle(), camera, merged, null);

        logger.info("Renderer and InputHandler instances instantiated");
    }

    /**
     * Main loop of the application
     */
    public void loop() {
        logger.info("Application loop started, initialization took {}ms", System.currentTimeMillis() - STARTUP_TIME);

        // LWJGl detects the current context and creates GLCapabilities instance
        // and makes the OpenGL bindings available
        GL.createCapabilities();

        // Set background color to black
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Set face culling and wireframe
        glEnable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        // Convert target fps to target frametime
        float targetFrameTime = 1.0f / config.getTargetFps();

        // Keep track of the last time when something was rendered
        float lastTime = (float) glfwGetTime();

        // Run rendering loop until user closes the window
        // Render new frame -> swap buffers (display the frame) -> poll for window
        // events
        while (!glfwWindowShouldClose(window.getHandle())) {
            // Difference in time between current time and last time something was rendered
            double deltaTime = glfwGetTime() - lastTime;

            if (deltaTime >= targetFrameTime) {
                renderer.render(deltaTime);
                glfwSwapBuffers(window.getHandle());
                glfwPollEvents();
            }
        }
    }

    /**
     * Releases all resources on application shutdown.
     * Disposes OpenGL resources, destroys the GLFW window and terminates GLFW.
     */
    public void cleanUp() {
        this.renderer.cleanUp();
        this.window.cleanUp();

        logger.info("Application resources removed from RAM and VRAM");
    }
}
