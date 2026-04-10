package input;

import static org.lwjgl.glfw.GLFW.*;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.lwjgl.opengl.GL11C.glViewport;

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
     * Set of supported movement keys, supports a few extra keys for dvorak enjoyers
     * :)
     */
    private static final Set<Integer> MOVEMENT_KEYS = Set.of(
            GLFW_KEY_W, GLFW_KEY_A, GLFW_KEY_S, GLFW_KEY_D,
            GLFW_KEY_Q, GLFW_KEY_E, GLFW_KEY_O, GLFW_KEY_L,
            GLFW_KEY_U, GLFW_KEY_SEMICOLON, GLFW_KEY_P, GLFW_KEY_J);

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
        glfwSetKeyCallback(this.windowHandle, (_window, key, _scancode, action, _mods) -> {
            this.onKeyEvent(key, action);
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
        // ESC closes the window
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
            glfwSetWindowShouldClose(this.windowHandle, true);

        if (key == GLFW_KEY_TAB && action == GLFW_RELEASE)
            this.camera.setMouseMode(this.windowHandle);

        // Add pressed keys to set if the current key is supported
        if (action == GLFW_PRESS && MOVEMENT_KEYS.contains(key) && !pressedKeys.contains(key))
            pressedKeys.add(key);

        // Remove key from pressed key set if the key is released
        if (action == GLFW_RELEASE && pressedKeys.contains(key))
            pressedKeys.remove(key);
    }

    /**
     * Handles mouse movement events.
     * 
     * @param mouseX Current mouse X position
     * @param mouseY Current mouse Y position
     */
    private void onMouseMove(double mouseX, double mouseY) {
        // Update initial mouse position to prevent large delta values
        if (this.lastMouseX < 0 || this.lastMouseY < 0) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return;
        }

        this.camera.rotate(this.lastMouseX - mouseX, this.lastMouseY - mouseY);

        // Set last mouse position variables to current values
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
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
     * Handles window resizing to calculate new projection matrix and updates the
     * viewport
     * 
     * @param newWidth  New window width
     * @param newHeight New window height
     */
    private void onWindowResize(int newWidth, int newHeight) {
        this.camera.onResize(newWidth, newHeight);
        glViewport(0, 0, newWidth, newHeight);
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
     * The main loop passes this to {@link Camera#update} each frame.
     *
     * @return Set of currently pressed key codes
     */
    public Set<Integer> getPressedKeys() {
        return this.pressedKeys;
    }
}
