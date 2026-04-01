package rendering;

import application.MouseMode;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Set;

/**
 * Handles camera movement and orientation over the terrain, maintains the
 * view matrix used in rendering.
 *
 * <p>
 * Movement is controlled with keyboard and mouse:
 * </p>
 * <ul>
 * <li>{@code WASD} - forward/backward/left/right</li>
 * <li>{@code Q/E} - up/down</li>
 * <li>{@code Mouse} - look around ({@link MouseMode#FLIGHT})</li>
 * </ul>
 */
public class Camera {
    private static final Logger logger = LoggerFactory.getLogger(Camera.class);

    /** Current position of the camera in world space */
    private final Vector3f position;

    /** Direction the camera is looking at */
    private Vector3f front;

    /** Up vector */
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
    private Matrix4f viewMatrix;

    /** Projection matrix calculated from fov and aspect ratio */
    private Matrix4f projectionMatrix;

    /** Current mouse interaction mode */
    private MouseMode mouseMode;

    /**
     * Constructs a new {@code Camera} at the given positon.
     *
     * @param position    Initial position in world space
     * @param fov         Vertical field of view in degrees
     * @param aspectRatio Viewport width/height
     * @param moveSpeed   Movement speed in m/s
     */
    public Camera(Vector3f position, float fov, float aspectRatio, float moveSpeed) {
        this.position = position;
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.moveSpeed = moveSpeed;

        // Initialize other variables
        this.front = new Vector3f(0.0f, 0.0f, 1.0f);
        this.up = new Vector3f(0.0f, 1.0f, 0.0f);
        this.yaw = 90.0f;
        this.pitch = 0.0f;
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        this.mouseMode = MouseMode.FLIGHT;

        // Calculate actual front vector based on initial pitch and yaw values
        this.updateFrontVector();

        // Calculate matrices
        this.updateViewMatrix();
        this.updateProjectionMatrix();

        logger.info("Initial view matrix: {}", this.viewMatrix.toString());
        logger.info("Camera created at {}", position.toString());
    }

    /**
     * Updates camera position based on keyboard input and elapsed time.
     * Must be called once per rendered frame!
     *
     * @param deltaTime   Time in seconds since last render
     * @param pressedKeys Set of currently pressed keys
     */
    public void update(float deltaTime, Set<Integer> pressedKeys) {
        // Only update if camera has moved
        if (!pressedKeys.isEmpty())
            this.updateViewMatrix();
    }

    /**
     * Rotates camera based on mouse movement.
     * If {@link #mouseMode} is set to {@link MouseMode#SELECTION} mouse movement
     * doesn't affect camera.
     *
     * @param deltaX Mouse movement in the X-axis
     * @param deltaY Mouse movement in the Y-axis
     */
    public void rotate(float deltaX, float deltaY) {
        if (this.mouseMode != MouseMode.FLIGHT)
            return;

        // TODO: add mouse sensitivity
        this.yaw += deltaX * 0.5;
        this.pitch -= deltaY * 0.5;

        // Limit pitch to [-89, 89] degrees or very bad things happen!
        this.pitch = Math.max(-89.0f, Math.min(89.0f, this.pitch));

        updateFrontVector();
        updateViewMatrix();
    }

    /**
     * Recalculates projection matrix if window is resized.
     *
     * @param width  New viewport width
     * @param height New viewport height
     */
    public void onResize(int width, int height) {
        this.aspectRatio = (float) width / height;
        this.updateProjectionMatrix();
    }

    /**
     * Switches mouse interaction mode and updates GLFW cursor.
     * <ul>
     * <li>{@link MouseMode#FLIGHT} - cursor is hidden and mouse movement rotates
     * camera.</li>
     * <li>{@link MouseMode#SELECTION} - cursor is visible and mouse is used for
     * point selection.</li>
     * </ul>
     *
     * @param mouseMode    New mouse mode
     * @param windowHandle GLFW window handle
     */
    public void setMouseMode(MouseMode mouseMode, long windowHandle) {
        if (mouseMode == null) {
            logger.error("Failed to set mouse mode");
            return;
        }

        this.mouseMode = mouseMode;
        switch (mouseMode) {
            case MouseMode.FLIGHT:
                glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                break;
            case MouseMode.SELECTION:
                glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                break;
            default:
                break;
        }
    }

    /**
     * Calculates the front vector from yaw and pitch angles. The rotation matrix
     * can be simplifed a lot since roll is not implemented.<br>
     * 
     * a=yaw, b=pitch, y=roll<br>
     * x = cos(a)*sin(b)*cos(y) + sin(a)*sin(y)<br>
     * y = sin(a)*sin(b)*cos(y) - cos(a)*sin(y)<br>
     * z = cos(b)*cos(y)<br>
     * <br>
     * 
     * Because we dont support roll, y is always 0 thus cos(y)=1, sin(y)=0<br>
     * x = cos(a)*sin(b)*1 + sin(a)*0 = cos(a)*sin(b)<br>
     * y = sin(a)*sin(b)*1 - cos(a)*0 = sin(a)*sin(b)<br>
     * z = cos(b)*1 = cos(b)<br>
     * <br>
     * 
     * These calculations use z as the up vector but OpenGL uses y. Whene we use
     * elevation angle instead of zenith we have to apply -90 degree offset.<br>
     * 
     * sin(b)=cos(90-b)<br>
     * x = cos(a)*cos(b)<br>
     * y = sin(b)<br>
     * z = sin(a)*cos(b)<br>
     * 
     * @see <a href=
     *      "https://en.wikipedia.org/wiki/Rotation_matrix#General_rotations">Wikipedia-
     *      Rotation matrix</a>
     */
    private void updateFrontVector() {
        float x = (float) (Math.cos(Math.toRadians(this.yaw)) * Math.cos(Math.toRadians(this.pitch)));
        float y = (float) Math.sin(Math.toRadians(this.pitch));
        float z = (float) (Math.sin(Math.toRadians(this.yaw)) * Math.cos(Math.toRadians(this.pitch)));

        this.front = new Vector3f(x, y, z).normalize();
    }

    /**
     * Recalculates view matrix from current position and orientation.
     * Called when camera moves or rotates.
     */
    private void updateViewMatrix() {
        this.viewMatrix = new Matrix4f().lookAt(this.position, new Vector3f(this.position).add(this.front),
                this.up);
    }

    /**
     * Calculates projection matrix which turns view space to clip space (the name
     * is quite self explanatory: it adds perspective).
     */
    private void updateProjectionMatrix() {
        this.projectionMatrix = new Matrix4f().perspective(
                (float) Math.toRadians(this.fov),
                this.aspectRatio,
                0.1f,
                1000f);
    }

    /**
     * @return Current view matrix
     */
    public Matrix4f getViewMatrix() {
        return this.viewMatrix;
    }

    /**
     * @return Current projection matrix
     */
    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    /**
     * @return Current camera position in world space
     */
    public Vector3f getPosition() {
        return this.position;
    }

    /**
     * @return Current camera front vector
     */
    public Vector3f getFront() {
        return this.front;
    }

    /**
     * @return Current mouse interaction mode
     */
    public MouseMode getMouseMode() {
        return this.mouseMode;
    }
}
