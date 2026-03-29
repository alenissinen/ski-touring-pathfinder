package application;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import rendering.Renderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Main application class responsible for the program lifecycle. Manages initialization,
 * main loop and cleanup.
 */
public class Application {
    /** Window object */
    private final Window window;

    /** Application config */
    private final Config config;

    /** Renderer object */
    private final Renderer renderer = new Renderer();

    /**
     * Constructor to create new application
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
        init();
        loop();

        // Clean resources before shutting down
        cleanUp();
    }

    /**
     * Setups necessary callbacks, initializes GLFW, and creates the window
     */
    public void init() {
        // Create an error callback, prints error message in stderr
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        // Create GLFW window
        this.window.create();
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

        // Convert target fps to target frametime
        float targetFrameTime = 1.0f / config.getTargetFps();

        // Keep track of the last time when something was rendered
        float lastTime = (float) glfwGetTime();

        // Run rendering loop until user closes the window
        // Render new frame -> swap buffers (display the frame) -> poll for window events
        while (!glfwWindowShouldClose(window.getHandle())) {
            // Difference in time between current time and last time something was rendered
            float deltaTime = ((float) glfwGetTime() - lastTime);

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
