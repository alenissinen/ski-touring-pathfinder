package ui;

import application.Config;
import application.Window;
import exceptions.HeightMapParseException;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiWindowFlags;
import terrain.HeightMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

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

    /** Loaded height map filenames */
    private List<String> mapNames = new ArrayList<>();

    // Settings the user edits
    private int[] renderDist = { 24 };
    private int[] moveSpeed = { 250 };
    private int[] width = { 1280 };
    private int[] height = { 720 };
    private int[] fps = { 240 };
    private float[] fov = { 70 };
    private List<HeightMap> maps = new ArrayList<>();

    /** Opens the launcher */
    public Config open() {
        this.init();
        this.loop();
        this.cleanUp();
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
                new Config.Builder().title("Configuration").width(600).height(400).build());
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
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
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
            ImGui.sliderFloat("FOV (degrees)", this.fov, 1, 120);

            if (ImGui.button("Load height map", -1, 50)) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    // Create filter buffer
                    PointerBuffer filters = stack.mallocPointer(1);
                    filters.put(stack.UTF8("*.asc"));
                    filters.flip();

                    String result = TinyFileDialogs.tinyfd_openFileDialog(
                            "Select height map file",
                            System.getProperty("user.dir"),
                            filters,
                            "ASCII Files (.asc)",
                            false);

                    logger.info("File chosen: {}", result);
                    this.mapNames.add(result);

                    try {
                        this.maps.add(HeightMap.fromAsciiFile(result));
                    } catch (HeightMapParseException e) {
                        logger.error("Failed to parse height map: {}", e.getCause());
                    } catch (FileNotFoundException e) {
                        // Dead code
                    }
                }
            }

            // List all loaded height map filenames
            for (int i = 0; i < this.mapNames.size(); i++) {
                // Only show filename not full path
                String fullPath = this.mapNames.get(i);
                String fileName = new java.io.File(fullPath).getName();

                ImGui.pushID(i);
                ImGui.text(fileName);
                ImGui.sameLine();
                ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.1f, 0.1f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.1f, 0.1f, 1.0f);

                if (ImGui.button("Remove")) {
                    this.mapNames.remove(i);
                    this.maps.remove(i);

                    // Adjust index since the list shifted
                    i--;

                    logger.info("Removed height map: {}", fileName);
                }

                ImGui.popStyleColor(2);
                ImGui.popID();
            }

            // Check whether any maps have been loaded
            boolean mapsLoaded = !this.maps.isEmpty();

            // Disable button if no maps have been loaded
            if (!mapsLoaded) {
                ImGui.beginDisabled(true);
            }

            // Make the button green
            ImGui.pushStyleColor(ImGuiCol.Button, 0.1f, 0.5f, 0.1f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.2f, 0.7f, 0.2f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.1f, 0.4f, 0.1f, 1.0f);

            if (ImGui.button("Launch Application", 285f, 50)) {

                // Build the actual config
                this.resultConfig = new Config.Builder()
                        .renderDistance(this.renderDist[0])
                        .movementSpeed((int) this.moveSpeed[0])
                        .title("Ski Touring Pathfinder")
                        .width(this.width[0]).height(this.height[0])
                        .major(4).minor(1)
                        .targetFps(this.fps[0])
                        .fov(this.fov[0])
                        .heightMap(HeightMap.merge(this.maps))
                        .build();

                logger.info("Application config created");

                this.shouldLaunch = true;
            }

            ImGui.popStyleColor(3);

            if (!mapsLoaded) {
                ImGui.endDisabled();
            }

            // Add tooltip below cursor to notify the user that app can't be launched
            if (!mapsLoaded && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
                ImGui.setNextWindowPos(ImGui.getMousePosX(), ImGui.getMousePosY() + 25);
                ImGui.beginTooltip();
                ImGui.text("You must load at least 1 height map!");
                ImGui.endTooltip();
            }

            ImGui.sameLine(10, 295);

            // Make the button red
            ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.1f, 0.1f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.8f, 0.1f, 0.1f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.5f, 0.1f, 0.1f, 1.0f);

            if (ImGui.button("Exit", 285f, 50)) {
                logger.info("User exited launcher");
                break;
            }

            ImGui.popStyleColor(3);
            ImGui.end();
            this.ui.endFrame();
            glfwSwapBuffers(this.window.getHandle());
        }
    }

    /** Destroys launcher, but keeps the glfw context so it can be reused */
    private void cleanUp() {
        this.ui.destroy();
        glfwDestroyWindow(this.window.getHandle());
        logger.info("Launcher destroyed");
    }
}
