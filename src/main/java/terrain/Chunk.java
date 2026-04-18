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

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * Represents a {@code CHUNK_SIZE}×{@code CHUNK_SIZE} cell region of the active
 * {@link HeightMap}, built from {@code (CHUNK_SIZE+1)²} vertices so seams match
 * neighbors.
 * 
 * <p>
 * Manages its own OpenGL resources (VBO, VAO, EBO) and handles data sharing
 * to and from the GPU.
 * </p>
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
    // private static final Logger logger = LoggerFactory.getLogger(Chunk.class);

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
        int dim = Constants.CHUNK_VERTEX_DIM;
        float[] vertexData = new float[dim * dim * 3];

        for (int row = 0; row < dim; row++) {
            for (int col = 0; col < dim; col++) {
                // Convert grid coordinates to world coordinates
                float x = (offsetX + col) * Constants.WORLD_SCALE;
                float z = (offsetZ + row) * Constants.WORLD_SCALE;
                float y = this.heightMap.getElevation(offsetX + col, offsetZ + row);

                // Store coordinates in the array
                int index = (row * dim + col) * 3;
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
     * @return Int array of index data, size {@code CHUNK_SIZE² * 2 * 3}
     */
    private int[] createIndexData() {
        int dim = Constants.CHUNK_VERTEX_DIM;
        int quads = Constants.CHUNK_SIZE;
        int index = 0;

        // CHUNK_SIZE^2 squares (shared seam: dim = CHUNK_SIZE + 1 vertices per axis)
        int[] indexData = new int[quads * quads * 2 * 3];

        for (int row = 0; row < quads; row++) {
            for (int col = 0; col < quads; col++) {
                int a = row * dim + col;
                int b = row * dim + col + 1;
                int c = (row + 1) * dim + col;
                int d = (row + 1) * dim + col + 1;

                // First triangle
                indexData[index++] = a;
                indexData[index++] = b;
                indexData[index++] = c;

                // Second triangle
                indexData[index++] = b;
                indexData[index++] = d;
                indexData[index++] = c;
            }
        }

        return indexData;
    }

    /**
     * Calculates normal for each vertex for lightning calculation
     * 
     * @param vertices Vertex data
     * @param indices  Index data
     * @return Calculated normals
     */
    private float[] calculateNormals(float[] vertices, int[] indices) {
        float[] normals = new float[vertices.length];

        for (int i = 0; i < indices.length; i += 3) {
            int index1 = indices[i];
            int index2 = indices[i + 1];
            int index3 = indices[i + 2];

            // Extract position vectors
            float v0x = vertices[index1 * 3];
            float v0y = vertices[index1 * 3 + 1];
            float v0z = vertices[index1 * 3 + 2];

            float v1x = vertices[index2 * 3];
            float v1y = vertices[index2 * 3 + 1];
            float v1z = vertices[index2 * 3 + 2];

            float v2x = vertices[index3 * 3];
            float v2y = vertices[index3 * 3 + 1];
            float v2z = vertices[index3 * 3 + 2];

            // Create normal vector
            Vector3f e1 = new Vector3f(v1x - v0x, v1y - v0y, v1z - v0z);
            Vector3f e2 = new Vector3f(v2x - v0x, v2y - v0y, v2z - v0z);
            Vector3f normal = e1.cross(e2);
            normal.normalize();

            // Store normal vector data
            normals[index1 * 3] += normal.x;
            normals[index1 * 3 + 1] += normal.y;
            normals[index1 * 3 + 2] += normal.z;

            normals[index2 * 3] += normal.x;
            normals[index2 * 3 + 1] += normal.y;
            normals[index2 * 3 + 2] += normal.z;

            normals[index3 * 3] += normal.x;
            normals[index3 * 3 + 1] += normal.y;
            normals[index3 * 3 + 2] += normal.z;
        }

        // Normalize all the vertex normals
        for (int i = 0; i < normals.length; i += 3) {
            Vector3f vN = new Vector3f(normals[i], normals[i + 1], normals[i + 2]);
            vN.normalize();

            normals[i] = vN.x;
            normals[i + 1] = vN.y;
            normals[i + 2] = vN.z;
        }

        return normals;
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
        float[] normalData = this.calculateNormals(vertexData, indexData);

        this.vao = glGenVertexArrays();
        glBindVertexArray(this.vao);

        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
        glBufferData(GL_ARRAY_BUFFER,
                (FloatBuffer) BufferUtils.createFloatBuffer(vertexData.length).put(vertexData).flip(), GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        int normalVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, normalVbo);
        glBufferData(GL_ARRAY_BUFFER,
                (FloatBuffer) BufferUtils.createFloatBuffer(normalData.length).put(normalData).flip(), GL_STATIC_DRAW);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);

        this.ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,
                (IntBuffer) BufferUtils.createIntBuffer(indexData.length).put(indexData).flip(), GL_STATIC_DRAW);

        this.indexCount = indexData.length;
        glBindVertexArray(0);
        this.uploaded = true;
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
        this.uploaded = false;
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
