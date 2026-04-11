package terrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import exceptions.HeightMapParseException;

/**
 * Stores elevation data parsed from ASCII raster files and provides methods
 * to access height values by coordinate
 */
public class HeightMap {
    private static final Logger logger = LoggerFactory.getLogger(HeightMap.class);

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

    /** Lowest elevation value in the height map */
    private float dataMinM = Float.NaN;

    /** Highest elevation value in the height map */
    private float dataMaxM = Float.NaN;

    /**
     * Package-private constructor for direct instantiation and testing.
     * Production code should use {@link #fromAsciiFile(String)} instead!
     * 
     * @param data     Height grid
     * @param width    Grid column amount
     * @param height   Grid row amount
     * @param xLL      xllcorner value from ASCII data
     * @param yLL      yllcorner value from ASCII data
     * @param cellSize cellsize value from ASCII data (meters)
     */
    HeightMap(
            float[][] data,
            int width,
            int height,
            double xLL,
            double yLL,
            double cellSize) {
        this.data = data;
        this.width = width;
        this.height = height;
        this.xLL = xLL;
        this.yLL = yLL;
        this.cellSize = cellSize;

        logger.info("Height map instantiated: width {}, height {}\n\txLL {}, yLL {}, cellSize {}", width, height, xLL,
                yLL, cellSize);
    }

    /**
     * Creates a {@link HeightMap} object from ASCII height map
     * 
     * @param filePath Path of the ASCII height map
     * @return HeightMap object of a height map
     */
    public static HeightMap fromAsciiFile(String filePath) throws HeightMapParseException {
        HeightMapParser parser = new HeightMapParser(filePath);
        return parser.parse();
    }

    /**
     * Elevation at logical grid coordinates, the same space as {@link Chunk}
     * uses via {@code offsetX + column} / {@code offsetZ + row}, not raw array
     * indices or world meters.
     *
     * @param x logical X (east–west grid step)
     * @param z logical Z (north–south grid step)
     * @return elevation in metres
     */
    public float getElevation(int x, int z) {
        int arrayX = x + (this.width / 2);
        int arrayZ = z + (this.height / 2);

        arrayX = Math.max(0, Math.min(arrayX, this.width - 1));
        arrayZ = Math.max(0, Math.min(arrayZ, this.height - 1));

        return this.data[arrayZ][arrayX];
    }

    /**
     * @return Width (columns) of data grid
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return Height (rows) of data grid
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Minimum elevation in the raw raster (metres)
     */
    public float getDataMinElevation() {
        computeElevationBounds();
        return this.dataMinM;
    }

    /**
     * Maximum elevation in the raw raster (metres)
     */
    public float getDataMaxElevation() {
        computeElevationBounds();
        return this.dataMaxM;
    }

    /**
     * Computes minimum and maximum elevation values from the height map data. For
     * now used for shading in fragment shader.
     */
    private void computeElevationBounds() {
        if (!Float.isNaN(this.dataMinM)) {
            return;
        }

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (int z = 0; z < this.height; z++) {
            for (int x = 0; x < this.width; x++) {
                float v = this.data[z][x];
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        this.dataMinM = min;
        this.dataMaxM = max;
        logger.info("Height map data elevation range: {}m - {}m", min, max);
    }

    /**
     * Calculate slope angle in given (x, z) point.
     * 
     * @param x Grid X position
     * @param z Grid Z positon
     * @return Slope angle in degrees at given point
     * @see <a
     *      href=
     *      "https://pro.arcgis.com/en/pro-app/latest/tool-reference/spatial-analyst/how-slope-works.htm">ArcGIS
     *      Pro article of slopes</a>
     */
    public double getSlopeAngle(int x, int z) {
        // Edge cases
        int ax = x + (this.width / 2);
        int az = z + (this.height / 2);
        if (ax <= 0 || az <= 0 || ax >= this.width - 1 || az >= this.height - 1) {
            return 0.0;
        }

        // Surface scanning window, 3x3 array where the middle cell is the cell for
        // which the slope is being calculated
        float a = this.data[az - 1][ax - 1], b = this.data[az - 1][ax], c = this.data[az - 1][ax + 1],
                d = this.data[az][ax - 1], f = this.data[az][ax + 1], g = this.data[az + 1][ax - 1],
                h = this.data[az + 1][ax], i = this.data[az + 1][ax + 1];

        // Since the header parser doesn't support NODATA values weight is always 4
        // (1+2*1+1)
        // and it would resort to (c+2f+i)/4*4 so we can skip it
        double dzdx = ((c + 2 * f + i) - (a + 2 * d + g)) / (8 * this.cellSize);
        double dzdy = ((g + 2 * h + i) - (a + 2 * b + c)) / (8 * this.cellSize);

        // Calculate the magnitude of the gradient vector and convert to degrees
        double rise_run = Math.sqrt(dzdx * dzdx + dzdy * dzdy);
        double slope_degrees = Math.toDegrees(Math.atan(rise_run));

        logger.info("Slope angle at ({}, {}) calculated: {}", x, z, slope_degrees);
        return slope_degrees;
    }

    /** Merges two heightmaps which share the same xllcorner value */
    public static HeightMap merge(HeightMap north, HeightMap south) throws IllegalArgumentException {
        // Height maps cant be of different width
        if (north.width != south.width) {
            logger.error("Grid column amount mismatch");
            throw new IllegalArgumentException("Grid column amount mismatch");
        }

        // Cell size must be the same for proper visualization
        if (north.cellSize != south.cellSize) {
            logger.error("Grid cell size mismatch");
            throw new IllegalArgumentException("Grid cell size mismatch");
        }

        int width = north.width;
        int height = north.height + south.height;
        double cellSize = north.cellSize;
        double xLL = north.xLL; // Both grids share the same xllcorner value
        double yLL = south.yLL; // Merged grid must use the yllcorner value of southern grid

        float[][] data = new float[height][width];

        // Copy north grid into merged grid
        for (int n = 0; n < north.height; n++) {
            System.arraycopy(north.data[n], 0, data[n], 0, north.width);
        }

        // Copy south grid into merged grid
        for (int n = 0; n < south.height; n++) {
            System.arraycopy(south.data[n], 0, data[north.height + n], 0, south.width);
        }

        logger.info("Height maps merged");
        return new HeightMap(data, width, height, xLL, yLL, cellSize);
    }
}
