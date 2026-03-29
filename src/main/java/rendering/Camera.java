package rendering;

import application.MouseMode;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

/**
 * Handles camera movement and orientation over the terrain, maintains the
 * view matrix used in rendering.
 *
 * <p>Movement is controlled with keyboard and mouse:</p>
 * <ul>
 *     <li>{@code WASD} - forward/backward/left/right</li>
 *     <li>{@code Q/E} - up/down</li>
 *     <li>{@code Mouse} - look around ({@link MouseMode#FLIGHT})</li>
 * </ul>
 */
public class Camera {
    /** Current position of the camera in world space */
    private final Vector3f position;

    /** Direction the camera is looking at */
    private final Vector3f front;

    /** Up vector of the camera, usually {@code (0, 1, 0)} */
    private final Vector3f up;

    /** Horizontal rotation angle in degrees */
    private float yaw;

    /** Vertical rotation angle in degrees */
    private float pitch;

    /** Camera movement speed in m/s */
    private float moveSpeed;

    /** Vertical field of view in degrees */
    private float fov;

    /** Aspect ratio of the viewport (width / height) */
    private float aspectRatio;

    /** View matrix calculated from position and orientation */
    private final Matrix4f viewMatrix;

    /** Projection matrix calculated from fov and aspect ratio */
    private final Matrix4f projectionMatrix;

    /** Current mouse interaction mode */
    private MouseMode mouseMode = MouseMode.FLIGHT;

    /**
     * Constructs a new {@code Camera} at the given positon.
     *
     * @param position Initial position in world space
     * @param fov Vertical field of view in degrees
     * @param aspectRatio Viewport width/height
     * @param moveSpeed Movement speed in m/s
     */
    public Camera(Vector3f position, float fov, float aspectRatio, float moveSpeed) {}

    /**
     * Updates camera position based on keyboard input and elapsed time.
     * Must be called once per rendered frame!
     *
     * @param deltaTime Time in seconds since last render
     * @param pressedKeys Set of currently pressed keys
     */
    public void update(float deltaTime, Set<Integer> pressedKeys) {}

    /**
     * Rotates camera based on mouse movement.
     * If {@link #mouseMode} is set to {@link MouseMode#SELECTION} mouse movement
     * doesn't affect camera.
     *
     * @param deltaX Mouse movement in the X-axis
     * @param deltaY Mouse movement in the Y-axis
     */
    public void rotate(float deltaX, float deltaY) {}

    /**
     * Recalculates projection matrix if window is resized.
     *
     * @param width New viewport width
     * @param height New viewport height
     */
    public void onResize(int width, int height) {}

    /**
     * Switches mouse interaction mode and updates GLFW cursor.
     * <ul>
     *     <li>{@link MouseMode#FLIGHT} - cursor is hidden and mouse movement rotates camera.</li>
     *     <li>{@link MouseMode#SELECTION} - cursor is visible and mouse is used for point selection.</li>
     * </ul>
     *
     * @param mouseMode New mouse mode
     * @param windowHandle GLFW window handle
     */
    public void setMouseMode(MouseMode mouseMode, long windowHandle) {}

    /**
     * Recalculates view matrix from current position and orientation.
     * Called when camera moves or rotates.
     */
    private void updateViewMatrix() {}

    /**
     * @return Current view matrix
     */
    public Matrix4f getViewMatrix() {}

    /**
     * @return Current projection matrix
     */
    public Matrix4f getProjectionMatrix() {}

    /**
     * @return Current camera position in world space
     */
    public Vector3f getPosition() {}

    /**
     * @return Current mouse interaction mode
     */
    public MouseMode getMouseMode() {}
}
