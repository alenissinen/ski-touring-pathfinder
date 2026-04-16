package application;

import org.lwjgl.glfw.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Responsible for creating and handling the GLFW window
 */
public class Window {
    private static final Logger logger = LoggerFactory.getLogger(Window.class);

    /** Native GLFW Window handle */
    private long window;

    /** Configuration object containing window and application properties */
    private Config config;

    /**
     * Constructs a new {@code Window} with the given configuration
     * 
     * @param config The configuration to use for this window
     */
    public Window(Config config) {
        this.config = config;
    }

    /**
     * @return GLFW window handle
     */
    public long getHandle() {
        return window;
    }

    /**
     * Creates and initializes the GLFW window
     * 
     * @throws RuntimeException if window creation fails
     */
    public void create() {
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Window stays hidden until shown
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // Window is resizable
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, config.getOpenGlMajor()); // Set OpenGL major version
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, config.getOpenGlMinor()); // Set OpenGL minor version
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // Force modern API and remove deprecated
                                                                       // features

        // Create window (NULL is just 0L but used for clarity)
        window = glfwCreateWindow(config.getWidth(), config.getHeight(), config.getTitle(), NULL, NULL);
        if (window == NULL) {
            logger.error("Failed to create GLFW window");
            throw new RuntimeException("Failed to create GLFW window");
        }

        center();

        // Make the window current OpenGL context
        glfwMakeContextCurrent(window);

        // Enable raw mouse position support if it is supported
        if (GLFW.glfwRawMouseMotionSupported()) {
            glfwSetInputMode(this.window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }

        // Make the window visible
        glfwShowWindow(window);

        logger.info("Window created");
    }

    /**
     * Centers the window
     */
    public void center() {
        // Get the resolution of the primary monitor
        GLFWVidMode monitorRes = glfwGetVideoMode(glfwGetPrimaryMonitor());

        // Center the window
        glfwSetWindowPos(
                this.window,
                (monitorRes.width() - this.config.getWidth()) / 2,
                ((monitorRes.height() - this.config.getHeight()) / 2));

        logger.info("Window centered");
    }

    /**
     * Clean GLFW resources
     */
    public void cleanUp() {
        glfwDestroyWindow(this.window);
        glfwTerminate();
        logger.info("GLFW terminated");
    }
}
