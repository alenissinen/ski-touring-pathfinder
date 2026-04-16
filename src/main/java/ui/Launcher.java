package ui;

import application.Config;
import application.Window;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;

import org.lwjgl.opengl.GL;

/**
 * Initial launcher for the application, shows only ImGui with temporary window
 */
public class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    /** Window instance */
    private Window window;

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
    private void init() throws IllegalStateException {
        // Initialize GLFW
        if (!glfwInit()) {
            logger.error("Failed to initialize GLFW");
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Create window
        this.window = new Window(
                new Config.Builder().title("Configuration").width(600).height(400).major(4).minor(1).build());
        this.window.create();

        GL.createCapabilities();

        logger.info("Launcher window created");

        // Initialize ImGui
        this.ui = new ImGuiLayer(this.window.getHandle());
        this.ui.init();

        logger.info("Launcher ImGui context created");
    }

    /** Renders the launcher window and ImGui value sliders */
    private void loop() {
        while (!glfwWindowShouldClose(this.window.getHandle()) && !this.shouldLaunch) {
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
            glfwSwapBuffers(this.window.getHandle());
        }
    }

    /** Destroys launcher, but keeps the glfw context so it can be reused */
    private void cleanup() {
        this.ui.destroy();
        glfwDestroyWindow(this.window.getHandle());
        logger.info("Launcher destroyed");
    }
}
