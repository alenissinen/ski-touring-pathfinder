package terrain;

import rendering.Camera;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Chunks are identified by a packed {@code long} key created from 32-bit
 * chunk coordinates. This avoids collisions for negative values.
 * A packed
 * {@code long}
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

    /**
     * Extra chunk rings kept loaded past {@link #renderDistance} so boundary chunks
     * are
     * not unloaded as soon as the camera crosses the edge (reduces visible
     * popping).
     */
    private static final int UNLOAD_EXTRA_CHUNKS = 2;

    /** Reference to the full height map all chunks use for elevation data */
    private final HeightMap heightMap;

    /**
     * Currently loaded chunks, key is a packed long:
     * {@code (((long) chunkX) << 32) | (chunkZ & 0xffffffffL)}
     */
    private final HashMap<Long, Chunk> loadedChunks;

    /**
     * Constructs a new {@code ChunkManager}.
     *
     * @param heightMap      Height map to pass to each {@link Chunk}
     * @param renderDistance Maximum distance in chunks that are rendered
     */
    public ChunkManager(HeightMap heightMap, int renderDistance) {
        this.heightMap = heightMap;
        this.renderDistance = renderDistance;
        this.loadedChunks = new HashMap<Long, Chunk>();

        logger.info("Chunk manager instantiated: renderDistance {}", renderDistance);
    }

    /**
     * @return The height map shared by all chunks.
     */
    public HeightMap getHeightMap() {
        return this.heightMap;
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
        AtomicInteger removedCount = new AtomicInteger(0);

        // Calculate whether previously loaded chunks are in render distance and in
        // front of camera
        this.loadedChunks.entrySet().removeIf(entry -> {
            Chunk chunk = entry.getValue();
            boolean outOfRange = isPastUnloadRadius(chunk.getChunkX(), chunk.getChunkZ(), cameraChunkX,
                    cameraChunkZ);
            boolean outsideData = !this.chunkOverlapsHeightMapData(chunk.getChunkX(), chunk.getChunkZ());

            if (outOfRange || outsideData) {
                chunk.dispose();
                removedCount.incrementAndGet();
                return true;
            }

            return false;
        });

        // Load chunks within render distance
        for (int x = cameraChunkX - this.renderDistance; x <= cameraChunkX + this.renderDistance; x++) {
            for (int z = cameraChunkZ - this.renderDistance; z <= cameraChunkZ + this.renderDistance; z++) {
                if (!this.isInsideLoadRadius(x, z, cameraChunkX, cameraChunkZ))
                    continue;

                if (!this.chunkOverlapsHeightMapData(x, z))
                    continue;

                long packedKey = this.packKey(x, z);

                // Dont add chunk if it already is in the map
                // Uploads chunk data to GPU if it isnt uploaded for some reason
                if (this.loadedChunks.containsKey(packedKey)) {
                    Chunk chunk = this.loadedChunks.get(packedKey);
                    if (!chunk.isUploaded())
                        chunk.upload();

                    continue;
                }

                // Add new chunk to the loaded chunks map and upload to GPU
                Chunk chunk = new Chunk(heightMap, x, z);

                chunk.upload();
                this.loadedChunks.put(packedKey, chunk);
            }
        }
    }

    /**
     * Calculates the squared Euclidean distance between two chunks in a 2D grid.
     *
     * @param chunkX       X-coordinate of the target chunk
     * @param chunkZ       Z-coordinate of the target chunk
     * @param cameraChunkX X-coordinate of the cameras current chunk
     * @param cameraChunkZ Z-coordinate of the cameras current chunk
     * @return The squared distance (dx^2 + dz^2) between the two chunks.
     */
    private static int chunkDistSq(int chunkX, int chunkZ, int cameraChunkX, int cameraChunkZ) {
        int dx = cameraChunkX - chunkX;
        int dz = cameraChunkZ - chunkZ;
        return dx * dx + dz * dz;
    }

    /**
     * Checks if the chunk is within the Euclidean circle, this results in a
     * circular rendering pattern instead of a square one.
     *
     * @param chunkX       X-coordinate of the chunk to check
     * @param chunkZ       Z-coordinate of the chunk to check
     * @param cameraChunkX X-coordinate of the camera's current chunk
     * @param cameraChunkZ Z-coordinate of the camera's current chunk
     * @return {@code true} if the chunk is within or on the boundary of the
     *         render distance, {@code false} otherwise.
     */
    private boolean isInsideLoadRadius(int chunkX, int chunkZ, int cameraChunkX, int cameraChunkZ) {
        int r = this.renderDistance;
        return chunkDistSq(chunkX, chunkZ, cameraChunkX, cameraChunkZ) <= r * r;
    }

    /**
     * Determines whether a chunk is far enough outside the render distance to be
     * safely unloaded.
     * <p>
     * This method uses an expanded radius ({@link #renderDistance} +
     * {@code UNLOAD_EXTRA_CHUNKS}) to prevent flickering on the edge of the
     * rendering distance.
     * </p>
     *
     * @param chunkX       X-coordinate of the chunk to check
     * @param chunkZ       Z-coordinate of the chunk to check
     * @param cameraChunkX X-coordinate of the cameras current chunk
     * @param cameraChunkZ Z-coordinate of the cameras current chunk
     * @return {@code true} if the chunk is outside the unload boundary,
     *         {@code false} otherwise.
     */
    private boolean isPastUnloadRadius(int chunkX, int chunkZ, int cameraChunkX, int cameraChunkZ) {
        int r = this.renderDistance + UNLOAD_EXTRA_CHUNKS;
        return chunkDistSq(chunkX, chunkZ, cameraChunkX, cameraChunkZ) > r * r;
    }

    /**
     * Check whether chunk is still inside the height maps dimensions.
     * 
     * @param chunkX Chunk grid X coordinate
     * @param chunkZ Chunx grid Z coordinate
     * @return {@code true} if chunk is inside height map data, {@code false}
     *         otherwise
     */
    private boolean chunkOverlapsHeightMapData(int chunkX, int chunkZ) {
        int minLx = chunkX * Constants.CHUNK_SIZE;
        int maxLx = minLx + Constants.CHUNK_SIZE;
        int minLz = chunkZ * Constants.CHUNK_SIZE;
        int maxLz = minLz + Constants.CHUNK_SIZE;

        int halfW = this.heightMap.getWidth() / 2;
        int halfH = this.heightMap.getHeight() / 2;
        int minValidX = -halfW;
        int maxValidX = this.heightMap.getWidth() - 1 - halfW;
        int minValidZ = -halfH;
        int maxValidZ = this.heightMap.getHeight() - 1 - halfH;

        boolean xOverlap = maxLx >= minValidX && minLx <= maxValidX;
        boolean zOverlap = maxLz >= minValidZ && minLz <= maxValidZ;
        return xOverlap && zOverlap;
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
     * @return Packed key as
     *         {@code (((long) chunkX) << 32) | (chunkZ & 0xffffffffL)}
     */
    private long packKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
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
