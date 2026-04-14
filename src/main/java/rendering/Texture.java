package rendering;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL30C.*;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for OpenGL textures.
 * Implements {@link AutoCloseable} to support try-with-resources.
 */
public class Texture implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Texture.class);

    /** OpenGL texture handle */
    private final int textureId;

    /** Texture width */
    private final int width;

    /** Texture height */
    private final int height;

    /** Buffer for texture data */
    private final ByteBuffer buffer;

    /**
     * Creates a new texture with the given dimensions and data.
     *
     * @param width  Texture width in pixels
     * @param height Texture height in pixels
     */
    public Texture(int width, int height) {
        this.width = width;
        this.height = height;

        // Allocate buffer
        this.buffer = BufferUtils.createByteBuffer(width * height);

        // Genrate texture and bind it
        this.textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.textureId);

        // Allocate empty texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, this.buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        logger.info("New texture created: buffer size = {}", width * height);
    }

    /**
     * Clears the buffer and sets each texel as unvisited. Call before updating
     * individual texels each frame.
     */
    public void clear() {
        buffer.clear();

        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }

        buffer.flip();
    }

    /**
     * Sets a single texel as visited/unvisited.
     *
     * @param x     Texel X coordinate
     * @param y     Texel Y coordinate
     * @param value Texel value (0 = unvisited, 255 = visited)
     */
    public void setTexel(int x, int y, byte value) {
        buffer.put(y * width + x, value);
    }

    /**
     * Binds this texture to the given texture unit.
     *
     * @param unit Texture unit index
     */
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Uploads the current buffer to the GPU texture.
     * Must be called after updating texels to make changes visible.
     */
    public void upload() {
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RED, GL_UNSIGNED_BYTE, buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** Unbinds bound texture */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** @return OpenGL texture handle */
    public int getTextureId() {
        return textureId;
    }

    /** @return Texture width in pixels */
    public int getWidth() {
        return width;
    }

    /** @return Texture height in pixels */
    public int getHeight() {
        return height;
    }

    /**
     * Frees the OpenGL texture resource.
     * Must be called when the texture is no longer needed.
     */
    public void dispose() {
        glDeleteTextures(this.textureId);
    }

    @Override
    public void close() {
        dispose();
    }
}
