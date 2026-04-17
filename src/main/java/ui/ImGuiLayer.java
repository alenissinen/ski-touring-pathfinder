package ui;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.RenderMode;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImInt;
import pathfinding.Node;
import rendering.Renderer;

// TODO: refactor application ui logic to its own class
/**
 * Handles ImGui layer on top of window and integrates it with OpenGL and GLFW.
 * Also responsible for drawing the main application ui.
 */
public class ImGuiLayer {
    private static final Logger logger = LoggerFactory.getLogger(ImGuiLayer.class);

    /** GLFW window handle */
    private final long windowHandle;

    /** ImGui GLFW bindings */
    private final ImGuiImplGlfw ImGuiGLFW = new ImGuiImplGlfw();

    /** ImGui OpenGL bindings */
    private final ImGuiImplGl3 ImGuiGL3 = new ImGuiImplGl3();

    /** ImGui window width */
    private float width;

    /** ImGui window height */
    private float height;

    /** ImGui window x position */
    private float posX = 0;

    /** ImGui window y position */
    private float posY = 0;

    /**
     * Whether this is the first frame (used to set initial size/position only once)
     */
    private boolean firstFrame = true;

    /** Maximum FPS value for the plot */
    private float maxFps;

    /** Store fps values */
    private float[] fpsBuffer = new float[80];

    /** Current fps buffer index */
    private int fpsBufferIndex = 0;

    /** Curret path */
    private List<Node> path;

    /** Cell size */
    private float cellSize;

    /** Height map min elevation */
    private float minElev;

    /** Height map max elevation */
    private float maxElev;

    /** Renderer instance */
    private Renderer renderer;

    /** Selected rendering mode */
    private ImInt selected = new ImInt(0);

    /** Store rendering mode names as strings */
    private static final String[] modes = Arrays.stream(RenderMode.values()).map(Enum::name).toArray(String[]::new);

    /**
     * Initalizes ImGuiLayer
     * 
     * @param windowHandle GLFW window handle
     */
    public ImGuiLayer(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    /**
     * Initializes ImGuiLayer with a specific window size and max fps for the plot
     *
     * @param windowHandle GLFW window handle
     * @param width        UI width in pixels
     * @param height       UI height in pixels
     * @param maxFps       Maximum FPS value for the plot
     * @param minElev      Minimum elevation
     * @param maxElev      Maximum elevation
     */
    public ImGuiLayer(long windowHandle, float width, float height, float maxFps, float minElev, float maxElev,
            Renderer renderer) {
        this.windowHandle = windowHandle;
        this.width = width;
        this.height = height;
        this.maxFps = maxFps;
        this.minElev = minElev;
        this.maxElev = maxElev;
        this.renderer = renderer;
    }

    /** Initializes ImGui */
    public void init() {
        ImGui.createContext();

        // Enable docking
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);

        // Initalize backends
        ImGuiGLFW.init(this.windowHandle, true);
        ImGuiGL3.init("#version 410");

        // Register callbacks

        logger.info("ImGui initialized");
    }

    /** Draws the application user interface */
    public void drawUI() {
        if (this.firstFrame) {
            ImGui.setNextWindowSize(width, height, ImGuiCond.Once);
            ImGui.setNextWindowPos(posX, posY, ImGuiCond.Once);
            this.firstFrame = false;
        }

        ImGui.begin("Info");

        this.drawFpsPlot();

        if (this.path != null) {
            // Display important information of the path
            float pathElevGain = this.elevationGain();
            float pathLengthKm = path.size() * this.cellSize / 1000;

            // Munter method
            float minTime = ((pathElevGain / 100.0f) + pathLengthKm) / 5;
            float maxTime = ((pathElevGain / 100.0f) + pathLengthKm) / 3;

            ImGui.text("Current path length: " + pathLengthKm * 1000 + "m");
            ImGui.text("Elevation gain: " + pathElevGain + "m");
            ImGui.separator();
            ImGui.text(String.format("Estimated time: %.2fh - %.2fh", minTime, maxTime));
            ImGui.text(String.format("(%.0f - %.0f minutes)", minTime * 60, maxTime * 60));

            // Plot elevation change on the route
            float[] elevationBuffer = new float[this.path.size()];

            for (int i = 0; i < this.path.size(); i++)
                elevationBuffer[i] = this.path.get(i).getY();

            ImGui.plotLines("", elevationBuffer, elevationBuffer.length, 0, "Elevation change",
                    this.minElev - 10f,
                    this.maxElev + 15f,
                    this.width - 10, 80);
        }

        // List all modes
        if (ImGui.combo("Render mode", this.selected, ImGuiLayer.modes)) {
            // Update render mode based on index
            this.renderer.setRenderMode(RenderMode.values()[this.selected.get()]);
        }

        // Update position variables
        this.posX = ImGui.getWindowPosX();
        this.posY = ImGui.getWindowPosY();
        this.width = ImGui.getWindowWidth();
        this.height = ImGui.getWindowHeight();

        ImGui.end();
    }

    /**
     * Updates the FPS buffer with a new value
     * 
     * @param fps New FPS value to add to the buffer
     */
    public void setFPS(float fps) {
        this.fpsBuffer[this.fpsBufferIndex] = fps;
        this.fpsBufferIndex = (this.fpsBufferIndex + 1) % this.fpsBuffer.length;
    }

    /**
     * Sets current path.
     * 
     * @param path Current route/path between two nodes
     */
    public void setPath(List<Node> path) {
        this.path = path;
    }

    /**
     * Sets current height map cell size for path length calculation
     * 
     * @param cellSize Height map cell size value
     */
    public void setCellSize(float cellSize) {
        this.cellSize = cellSize;
    }

    /**
     * Draws a line plot of the FPS values stored in the buffer
     */
    private void drawFpsPlot() {
        ImGui.plotLines("", this.fpsBuffer, this.fpsBuffer.length, this.fpsBufferIndex,
                String.format("FPS: %.1f", this.fpsBuffer[this.fpsBufferIndex]), 0.0f, this.maxFps, this.width - 10,
                80);
    }

    /**
     * Calculates total elevation gain
     */
    private float elevationGain() {
        float totalGain = 0;

        for (int i = 1; i < this.path.size(); i++) {
            float delta = this.path.get(i).getY() - this.path.get(i - 1).getY();
            if (delta > 0)
                totalGain += delta;
        }

        return totalGain;
    }

    /** Begins new ImGui frame */
    public void newFrame() {
        ImGuiGLFW.newFrame();
        ImGui.newFrame();
    }

    /** Ends current frame and renders it */
    public void endFrame() {
        ImGui.render();
        ImGuiGL3.renderDrawData(ImGui.getDrawData());
    }

    /** Destroys ImGui and frees its resources */
    public void destroy() {
        ImGuiGL3.dispose();
        ImGuiGLFW.dispose();
        ImGui.destroyContext();

        logger.info("ImGui disposed");
    }
}
