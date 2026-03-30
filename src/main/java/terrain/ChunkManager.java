package terrain;

import rendering.Camera;
import rendering.Renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
 * @implNote LOD (Level of Detail) might be implemented if necessary for
 *           performance.
 */
public class ChunkManager {
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
    }

    /**
     * Disposes all loaded chunks and clears internal state.
     * Must be called on application shutdown to free OpenGL resources!
     */
    public void disposeAll() {
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
