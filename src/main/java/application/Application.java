package application;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Main wrapper around mandatory functions such as window handling, render loop etc.
 */
public class Application {
    /** Window object */
    private Window window;
    /** Application config */
    private Config config;

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
     * Rendering loop of the application
     */
    public void loop() {
        // LWJGl detects the current context and creates GLCapabilities instance
        // and makes the OpenGL bindings available
        GL.createCapabilities();

        // Set color to black
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Run rendering loop until user closes the window
        while (!glfwWindowShouldClose(window.getHandle())) {
            // Clear framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Swap front and back buffers (show new frame)
            glfwSwapBuffers(window.getHandle());

            // Poll for any window events
            glfwPollEvents();
        }
    }
}
