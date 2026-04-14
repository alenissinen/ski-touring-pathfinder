package application;

/**
 * Application-wide constants, this class can not be initialized. All variables
 * are {@code public static final} and accessed directly.
 */
public class Constants {
    private Constants() {
    }

    /** World scale in meters per grid unit */
    public static final float WORLD_SCALE = 2.0f;

    /** Size of one chunk in grid units */
    public static final int CHUNK_SIZE = 125;

    /**
     * Vertices along one chunk edge: {@code CHUNK_SIZE + 1} so neighboring chunks
     * share a boundary column/row and no one-cell gap appears between meshes.
     */
    public static final int CHUNK_VERTEX_DIM = CHUNK_SIZE + 1;

    /**
     * Perspective far plane (meters). Must exceed farthest loaded terrain from the
     * camera (e.g. render distance × {@link #CHUNK_SIZE} × {@link #WORLD_SCALE}
     * plus margin)
     * or distant chunks clip out while still loaded.
     */
    public static final float CAMERA_FAR_PLANE = 50_000f;

    /** How many A* iterations to run each frame */
    public static final int ASTAR_ITERATIONS = 1000;

    /**
     * Slope angle threshold in degrees, everything above this has a penalty applied
     */
    public static final float SLOPE_THRESHOLD = 27.5f;

    /**
     * Cost multiplier applied to steps where slope exceeds {@link #SLOPE_THRESHOLD}
     */
    public static final float SLOPE_PENALTY = 3.5f;

    /** How many lines each file parser thread handles */
    public static final int FILE_CHUNK_SIZE = 100;
}
