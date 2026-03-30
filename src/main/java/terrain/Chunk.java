package terrain;

import application.Constants;

/**
 * Represents a 125x125 section of the merged {@link HeightMap}.
 * Manages its own OpenGL resources (VBO, VAO, EBO) and handles data sharing
 * to and from the GPU.
 *
 * <p>
 * Chunks are identified by their grid position (x,z)
 * </p>
 *
 * <p>
 * Each chunk holds a reference to the full {@link HeightMap} with an offset
 * instead of creating a copy, to save memory and provide access to neighboring
 * chunks
 * </p>
 *
 * <p>
 * Must be cleaned up with {@link #dispose()} when no longer needed to free
 * GPU resources. Implements {@link AutoCloseable} to support
 * try-with-resources.
 * </p>
 */
public class Chunk implements AutoCloseable {
    /** Reference to the full heightmap */
    private final HeightMap heightMap;

    /** Grid position of this chunk */
    private final int chunkX, chunkZ;

    /** Offset into the heightmap grid where this chunk starts */
    private final int offsetX, offsetZ;

    /** OpenGL vertex array object handle */
    private int vao;

    /** OpenGL vertex buffer object handle (positions) */
    private int vbo;

    /** OpenGL element buffer object handle (indices) */
    private int ebo;

    /** Number of indices in the EBO */
    private int indexCount;

    /** Has GPU resources been uploaded */
    private boolean uploaded;

    /**
     * Constructs a new {@code Chunk} at the given grid position.
     * Call {@link #upload()} to generate and send mesh data to the GPU.
     *
     * @param heightMap Height map to read elevation data from
     * @param chunkX    Grid X position of this chunk
     * @param chunkZ    Grid Z position of this chunk
     */
    public Chunk(HeightMap heightMap, int chunkX, int chunkZ) {
        this.heightMap = heightMap;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.offsetX = chunkX * Constants.CHUNK_SIZE;
        this.offsetZ = chunkZ * Constants.CHUNK_SIZE;
    }

    /**
     * @return Grid X position of this chunk.
     */
    public int getChunkX() {
        return chunkX;
    }

    /**
     * @return Grid Z position of this chunk.
     */
    public int getChunkZ() {
        return chunkZ;
    }

    /**
     * @return Whether this chunk has been uploaded to the GPU.
     */
    public boolean isUploaded() {
        return uploaded;
    }

    /**
     * Generates mesh data from the heightmap and uploads it to the GPU.
     * Creates the VAO, VBO and EBO. Should be only called once!
     *
     * <p>
     * Vertices are {@code (x, y, z)} floats where {@code y} is the
     * elevation value from the {@link HeightMap}.
     * </p>
     */
    public void upload() {
    }

    /**
     * Calls OpenGL draw functions for this chunk.
     * {@link #upload()} must be called before rendering!
     */
    public void render() {
    }

    /**
     * Frees all OpenGL resources (VAO, VBO, EBO) from the GPU.
     * Must be called when the chunk is no longer needed!
     *
     * <p>
     * Use try-with-resources when viable.
     * </p>
     * 
     * <pre>{@code
     * try (Chunk chunk = new Chunk(heightMap, 0, 0)) {
     *     chunk.upload();
     *     chunk.render();
     * } // dispose() is called automatically after the try-block
     * }</pre>
     */
    public void dispose() {
    }

    /**
     * Implements {@link AutoCloseable} by calling {@link #dispose()}.
     * Call {@link #dispose()} directly in non-try-with-resources context.
     * 
     * @see <a href=
     *      "https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">Oracle
     *      docs</a>
     */
    @Override
    public void close() {
        dispose();
    }
}
