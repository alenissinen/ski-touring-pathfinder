package terrain;

/**
 * Stores elevation data parsed from ASCII raster files and provides methods
 * to access height values by coordinate
 */
public class HeightMap {
    /** Elevation values in meters, indexed as [row][col] */
    private float[][] data;

    /** Width (columns) and height (rows) of the data grid */
    private int width, height;

    /** X coordinate of the lower-left corner (ETRS-TM35FIN coordinate system) */
    private double xLL;

    /** Y coordinate of the lower-left corner (ETRS-TM35FIN coordinate system) */
    private double yLL;

    /** Distance between data points in meters */
    private double cellSize;

    /**
     * Private constructor for loadFromFile to use
     * @param data Height grid
     * @param width Grid column amount
     * @param height Grid row amount
     * @param xLL xllcorner value from ASCII data
     * @param yLL yllcorner value from ASCII data
     * @param cellSize cellsize value from ASCII data
     */
    private HeightMap(
            float[][] data,
            int width,
            int height,
            double xLL,
            double yLL,
            double cellSize
    ) {}

    /**
     * Creates a {@link HeightMap} object from ASCII height map
     * @param filePath Path of the ASCII height map
     * @return HeightMap object of a height map
     */
    public static HeightMap loadFromFile(String filePath) {}

    /**
     * Returns height (y) on given x,z coordinate
     * @return height (y) in meters
     */
    public float getHeight(int x, int z) {}

    /** Returns width (columns) of data grid */
    public float getWidth() {}

    /** Returns height (rows) of data grid */
    public float getHeight() {}

    /** Returns slope angle in degrees at given point */
    public float getSlopeAngle(int x, int z) {}

    /** Merges two heightmaps which share the same xllcorner value */
    public static HeightMap merge(HeightMap north, HeightMap south) {}
}
