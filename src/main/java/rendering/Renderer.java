package rendering;

import application.Application;
import static org.lwjgl.opengl.GL11.*;

/**
 * Main renderer responsible for drawing each frame.
 * Handles all OpenGL draw calls including such as terrain, path and camera.
 */
public class Renderer {
    public Renderer() {}

    /**
     * Renders a single frame, called once per iteration of the application loop
     */
    public void render() {
        // Clear framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Releases all OpenGL resources held by the renderer.
     * Called by {@link Application#cleanUp()} on shutdown.
     */
    public void cleanUp() {}
}
