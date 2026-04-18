package terrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.joml.Math.lerp;

import java.io.FileNotFoundException;
import java.util.List;

import application.Constants;
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
     * Public constructor for direct instantiation and testing.
     * Production code should use {@link #fromAsciiFile(String)} instead!
     * 
     * @param data     Height grid
     * @param width    Grid column amount
     * @param height   Grid row amount
     * @param xLL      xllcorner value from ASCII data
     * @param yLL      yllcorner value from ASCII data
     * @param cellSize cellsize value from ASCII data (meters)
     */
    public HeightMap(
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

        logger.info("Height map instantiated: width {}, height {}, xLL {}, yLL {}, cellSize {}", width, height, xLL,
                yLL, cellSize);
    }

    /**
     * Creates a {@link HeightMap} object from ASCII height map
     * 
     * @param filePath Path of the ASCII height map
     * @return HeightMap object of a height map
     */
    public static HeightMap fromAsciiFile(String filePath) throws HeightMapParseException, FileNotFoundException {
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
     * Returns whether the given logical grid coordinates fall within the heightmap
     * bounds without clamping. (0, 0) is in the middle of the screen instead of
     * top-left.
     *
     * @param logicalX Logical grid X coordinate
     * @param logicalZ Logical grid Z coordinate
     * @return {@code true} if the coordinates are within bounds, {@code false}
     *         otherwise
     */
    public boolean isLogicalOnGrid(int logicalX, int logicalZ) {
        int arrayX = logicalX + (this.width / 2);
        int arrayZ = logicalZ + (this.height / 2);
        return arrayX >= 0 && arrayX < this.width && arrayZ >= 0 && arrayZ < this.height;
    }

    /**
     * Returns the interpolated elevation at the given world coordinates using
     * bilinear interpolation between the four surrounding heightmap vertices.
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Interpolated elevation
     * @see <a href=
     *      "https://en.wikipedia.org/wiki/Bilinear_interpolation">Wikipedia -
     *      Bilinear Interpolation</a>
     */
    public float interpolateElevation(float worldX, float worldZ) {
        // Convert world coordinates to grid coordinates
        float gridX = worldX / Constants.WORLD_SCALE;
        float gridZ = worldZ / Constants.WORLD_SCALE;

        // Grid cell origin
        int cellX = (int) Math.floor(gridX);
        int cellZ = (int) Math.floor(gridZ);

        // Offset values within the grid
        float offsetX = gridX - cellX;
        float offsetZ = gridZ - cellZ;

        // Sample the four corners of the grid cell
        float bottomLeft = getElevation(cellX, cellZ);
        float bottomRight = getElevation(cellX + 1, cellZ);
        float topLeft = getElevation(cellX, cellZ + 1);
        float topRight = getElevation(cellX + 1, cellZ + 1);

        // Bilinear interpolation
        return lerp(
                lerp(bottomLeft, bottomRight, offsetX),
                lerp(topLeft, topRight, offsetX),
                offsetZ);
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
     * @return Cell size
     */
    public double getCellSize() {
        return this.cellSize;
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

        return slope_degrees;
    }

    /**
     * Merges multiple heightmaps which neighbour each other in some way
     *
     * @param maps List of height maps to merge
     * @return Merged height map
     */
    public static HeightMap merge(List<HeightMap> maps) throws IllegalArgumentException {
        // Check for empty list or list with only 1 map
        if (maps == null || maps.isEmpty()) {
            throw new IllegalArgumentException("Height map list cant be empty");
        } else if (maps.size() == 1) {
            return maps.get(0);
        }

        // Validate cell size and find bounds
        double cellSize = maps.get(0).cellSize;
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        long totalPixels = 0;

        for (HeightMap hm : maps) {
            if (hm.cellSize != cellSize) {
                throw new IllegalArgumentException("Grid cell size mismatch");
            }

            minX = Math.min(minX, hm.xLL);
            minY = Math.min(minY, hm.yLL);

            // Calculate the top-right corner of this specific tile
            double tileMaxX = hm.xLL + (hm.width * cellSize);
            double tileMaxY = hm.yLL + (hm.height * cellSize);

            maxX = Math.max(maxX, tileMaxX);
            maxY = Math.max(maxY, tileMaxY);

            totalPixels += (long) hm.width * hm.height;
        }

        // Calculate dimensions
        int totalWidth = (int) Math.round((maxX - minX) / cellSize);
        int totalHeight = (int) Math.round((maxY - minY) / cellSize);

        // If the total area of the tiles doesn't match the bounding box area, there's a
        // gap or overlap
        if (totalPixels != (long) totalWidth * totalHeight) {
            logger.error("HeightMap merge failed: Gaps or overlaps detected in the provided tiles");
            throw new IllegalArgumentException("The provided heightmaps do not form a perfect gapless rectangle");
        }

        // Merged data array and tracker array
        float[][] data = new float[totalHeight][totalWidth];
        boolean[][] filled = new boolean[totalHeight][totalWidth];

        // Copy data
        for (HeightMap hm : maps) {
            // Calculate pixel offsets
            int colOffset = (int) Math.round((hm.xLL - minX) / cellSize);

            // Distance from the northern most row
            int rowOffset = (int) Math.round((maxY - (hm.yLL + (hm.height * cellSize))) / cellSize);

            for (int r = 0; r < hm.height; r++) {
                int targetRow = rowOffset + r;

                // Safety check for alignment
                if (filled[targetRow][colOffset]) {
                    throw new IllegalArgumentException("Overlap detected at row " + targetRow + " col " + colOffset);
                }

                System.arraycopy(hm.data[r], 0, data[targetRow], colOffset, hm.width);

                // Mark as filled
                for (int c = 0; c < hm.width; c++) {
                    filled[targetRow][colOffset + c] = true;
                }
            }
        }

        logger.info("Successfully merged {} heightmaps into a {}x{} grid", maps.size(), totalWidth, totalHeight);

        return new HeightMap(data, totalWidth, totalHeight, minX, minY, cellSize);
    }
}