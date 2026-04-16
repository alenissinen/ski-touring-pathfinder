package ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

/**
 * Handles ImGui layer on top of window and integrates it with OpenGL and GLFW.
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
     */
    public ImGuiLayer(long windowHandle, float width, float height, float maxFps) {
        this.windowHandle = windowHandle;
        this.width = width;
        this.height = height;
        this.maxFps = maxFps;
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

        ImGui.begin("Settings");

        this.drawFpsPlot();

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
     * Draws a line plot of the FPS values stored in the buffer
     */
    private void drawFpsPlot() {
        ImGui.plotLines("", this.fpsBuffer, this.fpsBuffer.length, this.fpsBufferIndex,
                String.format("FPS: %.1f", this.fpsBuffer[this.fpsBufferIndex]), 0.0f, this.maxFps, this.width - 10,
                80);
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
