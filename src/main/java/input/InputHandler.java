package input;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;

import java.nio.DoubleBuffer;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.MouseMode;
import pathfinding.AStar;
import rendering.Camera;
import terrain.HeightMap;

/**
 * Creates and handles all input callbacks for mouse and keyboard events.
 * Registers GLFW callback for processing user input, for example
 * camera movement and control. Also handles point selection for
 * pathfinding.
 * 
 * <p>
 * Movement is controlled with keyboard and mouse:
 * </p>
 * <ul>
 * <li>{@code WASD} - forward/backward/left/right</li>
 * <li>{@code Q/E} - up/down</li>
 * <li>{@code Mouse} - look around ({@link MouseMode#FLIGHT})</li>
 * </ul>
 * 
 * <p>
 * Point selection in {@link MouseMode#SELECTION}
 * </p>
 * <ul>
 * <li>First {@code Left Click} - select starting point</li>
 * <li>Second {@code Left Click} - select destination point and start
 * pathfinding</li>
 * </ul>
 * 
 * <p>
 * Point selection work by finding the nearest {@link HeightMap} grid point
 * under the mouse cursor.
 * </p>
 */
public class InputHandler {
    private static final Logger logger = LoggerFactory.getLogger(InputHandler.class);

    /** GLFW window handle */
    private final long windowHandle;

    /** Camera instance for movement updates and mouse mode switching */
    private final Camera camera;

    /** Height map reference for point selection */
    private final HeightMap heightMap;

    /** A* instance for pathfinding */
    private final AStar aStar;

    /** Set of currently pressed keys for movement */
    private final Set<Integer> pressedKeys;

    /** Start point coordinates */
    private int startX, startZ;

    /** Goal point coordinates */
    private int goalX, goalZ;

    /**
     * Whether the start point has been selected, resets after pathfinding is
     * initiated
     */
    private boolean startSelected = false;

    /** Last mouse cursor position for calculating movement deltas */
    private double lastMouseX = -1, lastMouseY = -1;

    /**
     * Counstructs a new {@code InputHandler} and registers GLFW callbacks.
     * 
     * @param windowHandle GLFW window handle
     * @param camera       Active camera
     * @param heightMap    Height map for point selection
     * @param aStar        A* instance for pathfinding
     */
    public InputHandler(long windowHandle, Camera camera, HeightMap heightMap, AStar aStar) {
        this.windowHandle = windowHandle;
        this.camera = camera;
        this.heightMap = heightMap;
        this.aStar = aStar;

        // Initialize other variables
        this.pressedKeys = new HashSet<Integer>();

        // Register callbacks
        this.registerCallbacks();
    }

    /** Registers GLFW callbacks for input handling */
    private void registerCallbacks() {
        // ESC closes the window
        glfwSetKeyCallback(this.windowHandle, (_window, key, _scancode, action, _mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(this.windowHandle, true);
            }
        });

        logger.info("GLFW key callback set");

        glfwSetWindowSizeCallback(windowHandle, (_window, newWidth, newHeight) -> {
            this.onWindowResize(newWidth, newHeight);
        });

        logger.info("GLFW window size callback set");

        glfwSetCursorPosCallback(windowHandle, (_window, xpos, ypos) -> {
            this.onMouseMove(xpos, ypos);
        });

        logger.info("GLFW cursor position callback set");
    }

    /**
     * Handles keyboard events. Maintains {@link #pressedKeys} and mouse mode
     * switching.
     * 
     * @param key    GLFW key code
     * @param action GLFW action code ({@code GLFW_PRESS}, {@code GLFW_RELEASE})
     */
    private void onKeyEvent(int key, int action) {
    }

    /**
     * Handles mouse movement events.
     * 
     * @param mouseX Current mouse X position
     * @param mouseY Current mouse Y position
     */
    private void onMouseMove(double mouseX, double mouseY) {
        this.camera.rotate(this.lastMouseX - mouseX, this.lastMouseY - mouseY);

        // Set last mouse position variables to current values
        this.lastMouseX = (int) mouseX;
        this.lastMouseY = (int) mouseY;
    }

    /**
     * Handles mouse click events for point selection in
     * {@link MouseMode#SELECTION}.
     * 
     * @param button GLFW mouse button code
     * @param action GLFW action code ({@code GLFW_PRESS}, {@code GLFW_RELEASE})
     */
    private void onMouseClick(int button, int action) {
    }

    /**
     * Handles window resizing to calculate new projection matrix
     * 
     * @param newWidth  New window width
     * @param newHeight New window height
     */
    private void onWindowResize(int newWidth, int newHeight) {
        this.camera.onResize(newWidth, newHeight);
    }

    /**
     * Resolves the current cursor position to the nearest height map grid
     * coordinates
     * using current view and projection matrices.
     * 
     * @param mouseX Current mouse X position
     * @param mouseY Current mouse Y position
     * @return Grid coordinates as an array or {@code null} if no valid point is
     *         found
     */
    private int[] resolveGridCoordinates(double mouseX, double mouseY) {
        // TODO: remove placeholder
        return new int[0];
    }

    /**
     * Returns the set of currently pressed GLFW key codes.
     * Sent to {@link Camera#update} every frame.
     * 
     * @return Set of currently pressed key codes
     */
    public Set<Integer> getPressedKeys() {
        return this.pressedKeys;
    }
}
