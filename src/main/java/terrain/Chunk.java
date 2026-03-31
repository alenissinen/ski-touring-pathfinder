package terrain;

import application.Constants;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.opengl.GL15C.glDeleteBuffers;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

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
     * Generates vertex data for this chunk as a flat array.
     * Each vertex is made from (x, y, z) coordinates where y is value from
     * {@link HeightMap}.
     * 
     * @return Float array of vertex data
     */
    private float[] createVertexData() {
        float[] vertexData = new float[Constants.CHUNK_SIZE * Constants.CHUNK_SIZE * 3];

        for (int row = 0; row < Constants.CHUNK_SIZE; row++) {
            for (int col = 0; col < Constants.CHUNK_SIZE; col++) {
                // Convert grid coordinates to world coordinates
                float x = (offsetX + col) * Constants.WORLD_SCALE;
                float z = (offsetZ + row) * Constants.WORLD_SCALE;
                float y = this.heightMap.getElevation(offsetX + col, offsetZ + row);

                // Store coordinates in the array
                int index = (row * Constants.CHUNK_SIZE + col) * 3;
                vertexData[index] = x;
                vertexData[index + 1] = y;
                vertexData[index + 2] = z;
            }
        }

        return vertexData;
    }

    /**
     * Generates index data for this chunk as a flat int array.
     * Each grid square is divided into two triangles (A,C,B) and (B,C,D).
     *
     * @return Int array of index data, size {@code (CHUNK_SIZE-1)² * 2 * 3}
     */
    private int[] createIndexData() {
        int indexAmount = Constants.CHUNK_SIZE - 1;
        int index = 0;

        // (CHUNK_SIZE-1)^2 squares, 2 triangles per square, 3 indices per triangle
        int[] indexData = new int[indexAmount * indexAmount * 2 * 3];

        for (int row = 0; row < indexAmount; row++) {
            for (int col = 0; col < indexAmount; col++) {
                int a = row * Constants.CHUNK_SIZE + col;
                int b = row * Constants.CHUNK_SIZE + col + 1;
                int c = (row + 1) * Constants.CHUNK_SIZE + col;
                int d = (row + 1) * Constants.CHUNK_SIZE + col + 1;

                // First triangle
                indexData[index++] = a;
                indexData[index++] = c;
                indexData[index++] = b;

                // Second triangle
                indexData[index++] = b;
                indexData[index++] = c;
                indexData[index++] = d;
            }
        }

        return indexData;
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
        float[] vertexData = this.createVertexData();
        int[] indexData = this.createIndexData();

        this.vao = glGenVertexArrays();
        glBindVertexArray(this.vao);

        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
        glBufferData(GL_ARRAY_BUFFER,
                (FloatBuffer) BufferUtils.createFloatBuffer(vertexData.length).put(vertexData).flip(), GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        this.ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,
                (IntBuffer) BufferUtils.createIntBuffer(indexData.length).put(indexData).flip(), GL_STATIC_DRAW);

        this.indexCount = indexData.length;
        glBindVertexArray(0);
    }

    /**
     * Calls OpenGL draw functions for this chunk.
     * {@link #upload()} must be called before rendering!
     */
    public void render() {
        glBindVertexArray(this.vao);
        glDrawElements(GL_TRIANGLES, this.indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
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
        glDeleteVertexArrays(this.vao);
        glDeleteBuffers(new int[] { this.vbo, this.ebo });
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
