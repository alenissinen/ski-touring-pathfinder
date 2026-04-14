package input;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.lwjgl.opengl.GL11C.glViewport;

import application.Constants;
import application.MouseMode;
import pathfinding.AStar;
import pathfinding.Node;
import rendering.Camera;
import rendering.Shader;
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

    /** Shader instance for uniform values */
    private final Shader shader;

    /** Set of currently pressed keys for movement */
    private final Set<Integer> pressedKeys;

    /** Start point coordinates */
    private float startX, startZ;

    /** Goal point coordinates */
    private float goalX, goalZ;

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
    public InputHandler(long windowHandle, Camera camera, HeightMap heightMap, AStar aStar, Shader shader) {
        this.windowHandle = windowHandle;
        this.camera = camera;
        this.heightMap = heightMap;
        this.aStar = aStar;
        this.shader = shader;

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

        glfwSetWindowSizeCallback(this.windowHandle, (_window, _newWidth, _newHeight) -> {
            this.onWindowResize();
        });

        logger.info("GLFW window size callback set");

        glfwSetCursorPosCallback(this.windowHandle, (_window, xpos, ypos) -> {
            this.onMouseMove(xpos, ypos);
        });

        logger.info("GLFW cursor position callback set");

        glfwSetMouseButtonCallback(this.windowHandle, (_window, button, action, _mods) -> {
            this.onMouseClick(button, action);
        });

        logger.info("GLFW mouse button click callback set");
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
        // logger.debug("{}, {}, {}", this.camera.getMouseMode(), button, action);
        if (this.camera.getMouseMode() == MouseMode.FLIGHT)
            return;

        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            // Create buffers
            DoubleBuffer xPos = BufferUtils.createDoubleBuffer(1);
            DoubleBuffer yPos = BufferUtils.createDoubleBuffer(1);
            IntBuffer width = BufferUtils.createIntBuffer(1);
            IntBuffer height = BufferUtils.createIntBuffer(1);
            IntBuffer winW = BufferUtils.createIntBuffer(1);
            IntBuffer winH = BufferUtils.createIntBuffer(1);

            glfwGetCursorPos(this.windowHandle, xPos, yPos);
            glfwGetFramebufferSize(this.windowHandle, width, height);
            glfwGetWindowSize(this.windowHandle, winW, winH);

            int fbW = width.get(0);
            int fbH = height.get(0);
            int windowW = Math.max(1, winW.get(0));
            int windowH = Math.max(1, winH.get(0));

            float mouseX = (float) (xPos.get(0) * fbW / (double) windowW);
            float mouseY = (float) (yPos.get(0) * fbH / (double) windowH);

            int w = fbW;
            int h = fbH;

            // Calculate grid coordinates
            float[] res = this.resolveGridCoordinates(mouseX, mouseY, w, h);
            if (res == null)
                return;

            if (!this.startSelected) {
                this.startX = res[0];
                this.startZ = res[1];
                this.startSelected = true;
            } else {
                this.goalX = res[0];
                this.goalZ = res[1];
                this.startSelected = false;

                logger.debug("Starting pathfinding | start = ({}, {}), goal = ({}, {})", this.startX, this.startZ,
                        this.goalX, this.goalZ);

                // Initialize the A* iterator
                this.aStar.init(this.startX, this.startZ, this.goalX, this.goalZ);
            }

            this.shader.bind();
            this.shader.setVec2("uSelectedGrid", new Vector2f(res));
            this.shader.unbind();
        }
    }

    /**
     * Handles window resizing to calculate new projection matrix and updates the
     * viewport
     */
    private void onWindowResize() {
        // Create buffers
        IntBuffer fbW = BufferUtils.createIntBuffer(1);
        IntBuffer fbH = BufferUtils.createIntBuffer(1);

        // Fetch frameBuffer data
        glfwGetFramebufferSize(this.windowHandle, fbW, fbH);

        int w = fbW.get(0);
        int h = fbH.get(0);

        if (w <= 0 || h <= 0)
            return;

        // Set new viewport size
        this.camera.onResize(w, h);
        glViewport(0, 0, w, h);
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
    private float[] resolveGridCoordinates(float mouseX, float mouseY, int w, int h) {
        // Calculate NDC
        float ndcX = (mouseX / w) * 2.0f - 1.0f;
        float ndcY = 1.0f - (mouseY / h) * 2.0f;

        // https://antongerdelan.net/opengl/raycasting.html
        Matrix4f invProj = new Matrix4f(this.camera.getProjectionMatrix()).invert();
        Vector4f nearEye = invProj.transform(new Vector4f(ndcX, ndcY, -1.0f, 1.0f));
        Vector4f farEye = invProj.transform(new Vector4f(ndcX, ndcY, 1.0f, 1.0f));
        nearEye.div(nearEye.w());
        farEye.div(farEye.w());
        Vector3f dirEye = new Vector3f(farEye.x, farEye.y, farEye.z).sub(nearEye.x, nearEye.y, nearEye.z)
                .normalize();
        Vector4f rayWorld = new Matrix4f(this.camera.getViewMatrix()).invert()
                .transform(new Vector4f(dirEye.x, dirEye.y, dirEye.z, 0.0f));
        Vector3f rayDirection = rayWorld.xyz(new Vector3f()).normalize();

        float dist = Constants.CAMERA_FAR_PLANE;
        Vector3f camPos = this.camera.getPosition();

        /*
         * Ray march through the terrain to find the intersection point.
         * Step size scales with distance to avoid tunneling close to the camera.
         */
        final float minStep = Constants.WORLD_SCALE * 0.25f;
        for (float n = 0; n < dist; n += Math.max(minStep, n * 0.01f)) {
            float worldX = camPos.x + rayDirection.x * n;
            float worldY = camPos.y + rayDirection.y * n;
            float worldZ = camPos.z + rayDirection.z * n;

            // Convert world position to heightmap grid coordinates
            int gridX = (int) Math.floor(worldX / Constants.WORLD_SCALE);
            int gridZ = (int) Math.floor(worldZ / Constants.WORLD_SCALE);

            if (!this.heightMap.isLogicalOnGrid(gridX, gridZ))
                continue;

            // Use bilinear interpolation for smoother surface height sampling
            float surfaceY = this.heightMap.interpolateElevation(worldX, worldZ);
            if (worldY <= surfaceY) {
                float gridY = this.heightMap.getElevation(gridX, gridZ);
                logger.debug("Intersection found at: {}, {}, {}", gridX, gridY, gridZ);
                return new float[] { gridX, gridZ };
            }
        }

        return null;
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
