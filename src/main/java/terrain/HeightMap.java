package terrain;

import exceptions.HeightMapParseException;

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
     * @param cellSize cellsize value from ASCII data (meters)
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
    public static HeightMap fromAsciiFile(String filePath) throws HeightMapParseException {}

    /**
     * @return Elevation (y) in meters
     */
    public float getElevation(int x, int z) {}

    /**
     * @return Width (columns) of data grid
     */
    public int getWidth() {}

    /**
     * @return Height (rows) of data grid
     */
    public int getHeight() {}

    /**
     * @param x Grid X position
     * @param z Grid Z positon
     * @return Slope angle in degrees at given point
     */
    public float getSlopeAngle(int x, int z) {}

    /** Merges two heightmaps which share the same xllcorner value */
    public static HeightMap merge(HeightMap north, HeightMap south) throws IllegalArgumentException {}
}
