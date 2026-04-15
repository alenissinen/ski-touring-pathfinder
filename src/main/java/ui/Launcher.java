package ui;

import application.Config;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;

import org.lwjgl.glfw.GLFWVidMode;

/**
 * Initial launcher for the application, shows only ImGui with temporary window
 */
public class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    /** GLFW window handle */
    private long window;

    /** Actual config which is used to launch application */
    private Config resultConfig = null;

    /** Did the user launch the application */
    private boolean shouldLaunch = false;

    /** ImGui instance */
    private ImGuiLayer ui;

    // Settings the user edits
    private int[] renderDist = { 24 };
    private int[] moveSpeed = { 250 };
    private int[] width = { 1280 };
    private int[] height = { 720 };
    private int[] fps = { 240 };

    /** Opens the launcher */
    public Config open() {
        init();
        loop();
        cleanup();
        return shouldLaunch ? resultConfig : null;
    }

    /** Initializes GLFW window and ImGui */
    private void init() {
        if (!glfwInit())
            throw new IllegalStateException("GLFW failed");

        // Create small window
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        window = glfwCreateWindow(600, 400, "Pathfinder Launcher", 0, 0);

        // Show window
        glfwMakeContextCurrent(this.window);
        GL.createCapabilities();
        glfwShowWindow(this.window);

        // Get the resolution of the primary monitor
        GLFWVidMode monitorRes = glfwGetVideoMode(glfwGetPrimaryMonitor());

        // Center the window
        glfwSetWindowPos(
                window,
                (monitorRes.width() - 600) / 2,
                ((monitorRes.height() - 400) / 2));

        logger.info("Launcher window created");

        // Initialize ImGui
        this.ui = new ImGuiLayer(this.window);
        this.ui.init();

        logger.info("Launcher ImGui context created");
    }

    /** Renders the launcher window and ImGui value sliders */
    private void loop() {
        while (!glfwWindowShouldClose(window) && !shouldLaunch) {
            glfwPollEvents();
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            this.ui.newFrame();

            ImGui.begin("Application Config", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
            ImGui.setWindowSize(600, 400);
            ImGui.setWindowPos(0, 0);

            ImGui.sliderInt("Render Distance (chunks)", this.renderDist, 1, 128);
            ImGui.sliderInt("Movement Speed (m/s)", this.moveSpeed, 0, 1000);
            ImGui.sliderInt("Window width", this.width, 1, 1920);
            ImGui.sliderInt("Window height", this.height, 1, 1080);
            ImGui.sliderInt("Target FPS", this.fps, 1, 1000);

            if (ImGui.button("Launch Application", -1, 50)) {
                // Build the actual config
                this.resultConfig = new Config.Builder()
                        .renderDistance(this.renderDist[0])
                        .movementSpeed((int) this.moveSpeed[0])
                        .title("Ski Touring Pathfinder")
                        .width(this.width[0]).height(this.height[0])
                        .major(4).minor(1)
                        .targetFps(this.fps[0])
                        .build();

                logger.info("Application config created");

                this.shouldLaunch = true;
            }

            ImGui.end();
            this.ui.endFrame();
            glfwSwapBuffers(this.window);
        }
    }

    /** Destroys launcher, but keeps the glfw context so it can be reused */
    private void cleanup() {
        this.ui.destroy();
        glfwDestroyWindow(this.window);
        logger.info("Launcher destroyed");
    }
}
