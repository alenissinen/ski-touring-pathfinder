package terrain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
    }

    /**
     * Creates a {@link HeightMap} object from ASCII height map
     * 
     * @param filePath Path of the ASCII height map
     * @return HeightMap object of a height map
     */
    public static HeightMap fromAsciiFile(String filePath) throws HeightMapParseException {
        // Read file from resources folder as a stream of bytes
        InputStream in = HeightMap.class.getResourceAsStream(filePath);
        if (in == null) {
            throw new HeightMapParseException("File not found", filePath);
        }

        // Turn input stream into a buffered reader
        try (
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader buffered = new BufferedReader(reader);) {
            // Parse header values
            int width = parseHeader(buffered.readLine(), Integer::parseInt, filePath);
            int height = parseHeader(buffered.readLine(), Integer::parseInt, filePath);
            double xLL = parseHeader(buffered.readLine(), Double::parseDouble, filePath);
            double yLL = parseHeader(buffered.readLine(), Double::parseDouble, filePath);
            double cellSize = parseHeader(buffered.readLine(), Double::parseDouble, filePath);
            buffered.readLine(); // Skip NODATA value

            float[][] data = new float[height][width];

            // Loop through each data row in the ascii file
            for (int n = 0; n < height; n++) {
                String line = buffered.readLine();
                if (line == null)
                    break;

                // Split row by spacebar (\\s+) and read into an array
                String[] values = line.trim().split("\\s+");

                // Parse each value in the row and add to the data array
                for (int m = 0; m < width; m++) {
                    data[n][m] = Float.parseFloat(values[m]);
                }
            }

            return new HeightMap(data, width, height, xLL, yLL, cellSize);
        } catch (IOException e) {
            throw new HeightMapParseException("Failed to read file", e, filePath);
        }
    }

    /**
     * @return Elevation (y) in meters
     */
    public float getElevation(int x, int z) {
        return this.data[z][x];
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
        // If chosen cell is an edge cell we return 0
        // TODO: implement angle calculation for edge cells
        if (x <= 0 || z <= 0 || x >= this.width - 1 || z >= this.height - 1) {
            return 0.0;
        }

        // Surface scanning window, 3x3 array where the middle cell is the cell for
        // which the slope is being calculated
        float a = this.data[z - 1][x - 1], b = this.data[z - 1][x], c = this.data[z - 1][x + 1],
                d = this.data[z][x - 1], f = this.data[z][x + 1], g = this.data[z + 1][x - 1],
                h = this.data[z + 1][x], i = this.data[z + 1][x + 1];

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
     * Parses a single line from ASCII height map file and returns the header value
     * as type T
     */
    private static <T> T parseHeader(String line, java.util.function.Function<String, T> parser, String filePath)
            throws HeightMapParseException {
        try {
            String parsedLine = line.trim().split("\\s+")[1];
            return parser.apply(parsedLine);
        } catch (Exception e) {
            throw new HeightMapParseException("Invalid header" + line, e, filePath);
        }
    }

    /** Merges two heightmaps which share the same xllcorner value */
    public static HeightMap merge(HeightMap north, HeightMap south) throws IllegalArgumentException {
        // Height maps cant be of different width
        if (north.width != south.width) {
            throw new IllegalArgumentException("Grid column amount mismatch");
        }

        // Cell size must be the same for proper visualization
        if (north.cellSize != south.cellSize) {
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

        return new HeightMap(data, width, height, xLL, yLL, cellSize);
    }
}
