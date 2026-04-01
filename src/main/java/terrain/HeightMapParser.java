package terrain;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import exceptions.HeightMapParseException;

public class HeightMapParser {
    private static final Logger logger = LoggerFactory.getLogger(HeightMapParser.class);

    /** ASCII file path */
    private final String filePath;

    /** How many lines each thread handles */
    private static final int FILE_CHUNK_SIZE = 100;

    /**
     * Construct new height map parser
     * 
     * @param filePath ASCII file path
     */
    public HeightMapParser(String filePath) {
        this.filePath = filePath;
    }

    public HeightMap parse() throws HeightMapParseException {
        InputStream in = HeightMap.class.getResourceAsStream(filePath);

        if (in == null) {
            logger.error("File not found: {}", filePath);
            throw new HeightMapParseException("File not found", filePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Parse header values
            int width = parseHeader(reader.readLine(), Integer::parseInt, filePath);
            int height = parseHeader(reader.readLine(), Integer::parseInt, filePath);
            double xLL = parseHeader(reader.readLine(), Double::parseDouble, filePath);
            double yLL = parseHeader(reader.readLine(), Double::parseDouble, filePath);
            double cellSize = parseHeader(reader.readLine(), Double::parseDouble, filePath);
            reader.readLine(); // Skip NODATA value

            // Create data array and list for tasks
            float[][] data = new float[height][width];
            List<Future<?>> tasks = new ArrayList<>();

            // Read rows and divide to parts
            for (int row = 0; row < height; row += FILE_CHUNK_SIZE) {
                final int firstRow = row;
                final int lastRow = Math.min(row + FILE_CHUNK_SIZE, height);

                // Read chunks to memory
                String[] lines = new String[lastRow - firstRow];
                for (int i = 0; i < lines.length; i++) {
                    lines[i] = reader.readLine();
                }

                // Create new virtual thread for current chunk
                tasks.add(executor.submit(() -> {
                    for (int r = 0; r < lines.length; r++) {
                        if (lines[r] != null) {
                            parseLine(lines[r], data[firstRow + r], width);
                        }
                    }
                }));
            }

            // Wait for each future to complete
            for (Future<?> task : tasks) {
                task.get();
                logger.info("HeightMapParser ({}) task ({}) status: {}", this.filePath, task.hashCode(), task.state());
            }

            logger.info("Height map parsing complete: {}x{}", width, height);
            return new HeightMap(data, width, height, xLL, yLL, cellSize);
        } catch (Exception e) {
            logger.error("Error parsing height map: {}", filePath, e);
            throw new HeightMapParseException("Parsing failed", e, filePath);
        }
    }

    /**
     * Parse line by tokenizing it.
     * 
     * @param line    Line to parse
     * @param storage Array to store the parsed value in
     * @param width   Row width
     */
    private void parseLine(String line, float[] storage, int width) {
        StringTokenizer st = new StringTokenizer(line);
        for (int i = 0; i < width && st.hasMoreTokens(); i++) {
            storage[i] = Float.parseFloat(st.nextToken());
        }
    }

    /**
     * Parses a single line from ASCII height map file and returns the header value
     * as type T
     */
    private static <T> T parseHeader(String line, java.util.function.Function<String, T> parser, String filePath)
            throws HeightMapParseException {
        try {
            String parsedLine = line.trim().split("\\s+")[1];

            logger.info("Header parsed: line {}, value {}", line, parsedLine);
            return parser.apply(parsedLine);
        } catch (Exception e) {
            logger.error("Invalid ascii file header: {}", line);
            throw new HeightMapParseException("Invalid header" + line, e, filePath);
        }
    }
}
