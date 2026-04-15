package ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import imgui.ImGui;
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

    /**
     * Initalizes ImGuiLayer
     * 
     * @param windowHandle GLFW window handle
     */
    public ImGuiLayer(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    /** Initializes ImGui */
    public void init() {
        ImGui.createContext();

        // Enable docking
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);

        // Initalize backends
        ImGuiGLFW.init(this.windowHandle, true);
        ImGuiGL3.init("#version 410");

        logger.info("ImGui initialized");
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
