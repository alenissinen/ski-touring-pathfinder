package terrain;

import rendering.Camera;
import rendering.Renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.Constants;

/**
 * Manages terrain chunks by tracking their state based on camera position.
 * Acts as a bridge between {@link Chunk} and {@link Renderer}
 *
 * <p>
 * Chunks are identified by a packed {@code int} key created by its
 * grid coordinates: {@code (chunkX << 8) | chunkZ}. The key can be unpacked:
 * {@code chunkX = key >> 8} and {@code chunkZ = key & 0xFF}. A packed
 * {@code int}
 * is preferred over a {@code String} key to avoid object allocation and hashing
 * on
 * every lookup. This is done pre-emptively to support a lot larger maps.
 * </p>
 *
 * <p>
 * Chunks are loaded dynamically as the camera moves, prioritized by distance.
 * Chunks that fall outside the rendering distance are removed from the memory.
 * </p>
 * 
 * <p>
 * Implemantion note: LOD (Level of Detail) might be implemented if
 * necessary for performance.
 * </p>
 */
public class ChunkManager {
    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);

    /** Maximum distance in chunks that are rendered */
    private final int renderDistance;

    /** Reference to the full height map all chunks use for elevation data */
    private final HeightMap heightMap;

    /**
     * Currently loaded chunks, key is a packed int: {@code (chunkX << 8) | chunkZ}
     */
    private final HashMap<Integer, Chunk> loadedChunks;

    /**
     * Constructs a new {@code ChunkManager}.
     *
     * @param heightMap      Height map to pass to each {@link Chunk}
     * @param renderDistance Maximum distance in chunks that are rendered
     */
    public ChunkManager(HeightMap heightMap, int renderDistance) {
        this.heightMap = heightMap;
        this.renderDistance = renderDistance;
        this.loadedChunks = new HashMap<Integer, Chunk>();

        logger.info("Chunk manager instantiated: renderDistance {}", renderDistance);
    }

    /**
     * @return All currently loaded chunks ({@link Chunk}) that are ready to be
     *         rendered
     */
    public List<Chunk> getLoadedChunks() {
        return loadedChunks.values()
                .stream()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Updates the set of loaded chunks based on camera position.
     * Loads chunks within {@link #renderDistance} and removes chunks that have
     * moved out of range.
     *
     * <p>
     * Should be called once per frame!
     * </p>
     *
     * @param camera The active camera in use
     */
    public void update(Camera camera) {
        // Camera position and chunk in which it currently is
        Vector3f cameraPos = camera.getPosition();
        int cameraChunkX = this.toChunkX(cameraPos.x);
        int cameraChunkZ = this.toChunkZ(cameraPos.z);

        // Calculate whether previously loaded chunks are in render distance and in
        // front of camera
        for (Map.Entry<Integer, Chunk> element : this.loadedChunks.entrySet()) {
            Chunk chunk = element.getValue();

            // Remove chunk if it is behind camera
            if (this.isChunkBehindCamera(chunk.getChunkX(), chunk.getChunkZ(), cameraChunkX, cameraChunkZ,
                    camera.getFront())) {
                // Free chunk gpu resources and remove from loaded list
                chunk.dispose();
                this.loadedChunks.remove(element.getKey());

                continue;
            }

            if (!this.isChunkInRenderDistance(chunk.getChunkX(), chunk.getChunkZ(), cameraChunkX, cameraChunkZ)) {
                // Free chunk gpu resources and remove from loaded list
                chunk.dispose();
                this.loadedChunks.remove(element.getKey());
            }
        }

        // Load new chunks within render distance
        for (int x = cameraChunkX - this.renderDistance; x <= cameraChunkX + this.renderDistance; x++) {
            for (int z = cameraChunkZ - this.renderDistance; z <= cameraChunkZ + this.renderDistance; z++) {
                // Dont add chunk if it is behind camera
                if (this.isChunkBehindCamera(x, z, cameraChunkX, cameraChunkZ, cameraPos))
                    continue;

                int packedKey = this.packKey(x, z);

                // Dont add chunk if it already is in the map
                if (this.loadedChunks.containsKey(packedKey))
                    continue;

                // Add new chunk to the loaded chunks map
                this.loadedChunks.put(packedKey, new Chunk(heightMap, x, z));
            }
        }
    }

    /**
     * Checks if a chunk is inside the render distance from the camera.
     * 
     * @param chunkX       Target chunk X coordinate
     * @param chunkZ       Target chunk Z coordinate
     * @param cameraChunkX Camera chunk X coordinate
     * @param cameraChunkZ Camera chunk Z coordinate
     * @return True if the chunk is inside the render distance, false if outside
     */
    private boolean isChunkInRenderDistance(int chunkX, int chunkZ, int cameraChunkX, int cameraChunkZ) {
        // Calculate whether distance between chunks is greater than render distance
        int chunkdx = cameraChunkX - chunkX;
        int chunkdz = cameraChunkZ - chunkZ;

        return Math.sqrt(chunkdx * chunkdx + chunkdz * chunkdz) <= this.renderDistance;
    }

    /**
     * Checks if a chunk is behind the camera.
     * 
     * @param chunkX       Target chunk X coordinate
     * @param chunkZ       Target chunk Z coordinate
     * @param cameraChunkX Camera chunk X coordinate
     * @param cameraChunkZ Camera chunk Z coordinate
     * @param cameraFront  Camera front vector
     * @return True if the chunk is behind the camera, false if not
     */
    private boolean isChunkBehindCamera(int chunkX, int chunkZ, int cameraChunkX, int cameraChunkZ,
            Vector3f cameraFront) {
        // Calculate dot product between camera front vector and
        // current chunk - camera chunk vector
        float dx = chunkX - cameraChunkX;
        float dz = chunkZ - cameraChunkZ;

        float dot = dx * cameraFront.x + dz * cameraFront.z;

        return dot < 0;
    }

    /**
     * Disposes all loaded chunks and clears internal state.
     * Must be called on application shutdown to free OpenGL resources!
     */
    public void disposeAll() {
        for (Chunk chunk : loadedChunks.values()) {
            chunk.dispose();
        }

        logger.info("All loaded chunks disposed");
    }

    /**
     * Packs chunk grid coords into a single {@code int} key.
     *
     * @param chunkX Grid X coordinate of the chunk
     * @param chunkZ Grid Z coordinate of the chunk
     * @return Packed key as {@code (chunkX << 8) | chunkZ}
     */
    private int packKey(int chunkX, int chunkZ) {
        return (chunkX << 8) | chunkZ;
    }

    /**
     * Converts a world position X coordinate to a chunk grid X coordinate
     * 
     * @param worldX World position X in meters
     * @return Chunk grid X coordinate
     */
    private int toChunkX(float worldX) {
        return (int) (worldX / (Constants.CHUNK_SIZE * Constants.WORLD_SCALE));
    }

    /**
     * Converts a world position Z coordinate to a chunk grid Z coordinate
     * 
     * @param worldZ World position Z in meters
     * @return Chunk grid Z coordinate
     */
    private int toChunkZ(float worldZ) {
        return (int) (worldZ / (Constants.CHUNK_SIZE * Constants.WORLD_SCALE));
    }
}
