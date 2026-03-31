package application;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;

import exceptions.HeightMapParseException;
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
    /** Window object */
    private final Window window;

    /** Application config */
    private final Config config;

    /** Renderer object */
    private Renderer renderer;

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
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Create GLFW window
        this.window.create();

        GL.createCapabilities();

        // Create north and south height maps and merge them
        HeightMap north = HeightMap.fromAsciiFile("/T5311B.asc");
        HeightMap south = HeightMap.fromAsciiFile("/T5311A.asc");
        HeightMap merged = HeightMap.merge(north, south);

        Camera camera = new Camera(new Vector3f(), 68.0f, this.config.getWidth() / this.config.getHeight(), 2.0f);
        ChunkManager chunkManager = new ChunkManager(merged, 8);
        this.renderer = new Renderer(camera, chunkManager, null);
    }

    /**
     * Main loop of the application
     */
    public void loop() {
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
        renderer.cleanUp();
        glfwDestroyWindow(window.getHandle());
        glfwTerminate();
    }
}
