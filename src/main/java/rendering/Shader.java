package rendering;

import exceptions.ShaderException;

import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL20C.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for loading, compiling, and managing GLSL shader programs.
 *
 * <p>
 * Loads vertex and fragment shaders from {@code resources/shaders/}.
 * {@link #bind()} must be called before using draw calls or setting uniforms!
 * </p>
 *
 * <p>
 * Must be cleaned up with {@link #dispose()} to free GPU resources!
 * Implements {@link AutoCloseable} to support try-with-resources.
 * </p>
 */
public class Shader implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Shader.class);

    /** OpenGL handle for the compiled and linked shader program */
    private int programId;

    /** OpenGL handle for the vertex shader */
    private int vertexShaderId;

    /** OpenGL handle for the fragment shader */
    private int fragmentShaderId;

    // 16-bit float buffer to use for uploading uniform values to gpu
    private final FloatBuffer mat4Buffer = BufferUtils.createFloatBuffer(16);

    /**
     * Loads, compiles and links a shader program from vertex and fragment shader
     * files.
     * Shaders must be located in {@code resources/shaders/}!
     *
     * @param vertexShader   Name of the vertex shader
     * @param fragmentShader Name of the fragment shader
     * @throws ShaderException If one of the shaders fails to compile or the
     *                         combined
     *                         program fails to link
     */
    public Shader(String vertexShader, String fragmentShader) throws ShaderException {
        // Create shader program and shaders
        this.programId = glCreateProgram();
        this.vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
        this.fragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER);

        // Read shader files and compile them
        try {
            String vertex = this.loadSource(vertexShader);
            this.compileShader(vertex, GL_VERTEX_SHADER, vertexShader);

            logger.info("Vertex shader ({}) compiled", vertexShader);
        } catch (IOException e) {
            logger.error("Failed to compile vertex shader");
            throw new ShaderException("Failed to read vertex shader", vertexShader);
        }

        try {
            String fragment = this.loadSource(fragmentShader);
            this.compileShader(fragment, GL_FRAGMENT_SHADER, fragmentShader);

            logger.info("Fragment shader ({}) compiled", fragmentShader);
        } catch (IOException e) {
            logger.error("Failed to compile fragment shader");
            throw new ShaderException("Failed to read fragment shader", fragmentShader);
        }

        // Link the program
        this.linkProgram();
        logger.info("Shader program ({}) linked", this.programId);
    }

    /** Binds the shader program as the active OpenGL shader */
    public void bind() {
        glUseProgram(this.programId);
    }

    /** Unbinds the shader program */
    public void unbind() {
        glUseProgram(0);
    }

    /**
     * Sets a {@code mat4} uniform value in the shader.
     *
     * @param name  Name of the uniform value in the shader
     * @param value 4x4 matrix ({@link Matrix4f})
     */
    public void setMat4(String name, Matrix4f value) {
        this.mat4Buffer.clear();
        value.get(this.mat4Buffer);
        this.mat4Buffer.rewind();

        int uniformLocation = glGetUniformLocation(this.programId, name);
        glUniformMatrix4fv(uniformLocation, false, this.mat4Buffer);
    }

    /**
     * Sets a {@code float} uniform.
     * 
     * @param name  Name of the uniform value in the shader
     * @param value float value to set
     */
    public void setFloat(String name, float value) {
        int location = glGetUniformLocation(this.programId, name);
        glUniform1f(location, value);
    }

    /**
     * Sets a {@code int} uniform.
     * 
     * @param name  Name of the uniform value in the shader
     * @param value integer value to set
     */
    public void setInt(String name, int value) {
        int location = glGetUniformLocation(this.programId, name);
        glUniform1i(location, value);
    }

    /**
     * Sets a {@code vec2} uniform.
     * 
     * @param name  Name of the uniform value in the shader
     * @param value 2 component vector ({@link Vector2f})
     */
    public void setVec2(String name, Vector2f value) {
        int location = glGetUniformLocation(this.programId, name);
        glUniform2f(location, value.x, value.y);
    }

    /**
     * Sets a {@code vec3} uniform.
     * 
     * @param name  Name of the uniform value in the shader
     * @param value 3 component vector ({@link Vector3f})
     */
    public void setVec3(String name, Vector3f value) {
        int location = glGetUniformLocation(this.programId, name);
        glUniform3f(location, value.x, value.y, value.z);
    }

    /**
     * Sets a {@code vec4} uniform value in the shader.
     *
     * @param name  Name of the uniform value in the shader
     * @param value 4 component vector ({@link Vector4f})
     */
    public void setVec4(String name, Vector4f value) {
        int location = glGetUniformLocation(this.programId, name);
        glUniform4f(location, value.x, value.y, value.z, value.w);
    }

    /**
     * Reads a GLSL shader file from {@code resources/shaders/}.
     *
     * @param shaderName Shader filename
     * @return Shader source code as a {@code String}
     * @throws IOException If the file cannot be read
     */
    private String loadSource(String shaderName) throws IOException {
        InputStream shaderStream = Shader.class.getResourceAsStream(shaderName);

        if (shaderStream == null) {
            logger.error("Shader {} not found", shaderName);
            throw new IOException("[shader: " + shaderName + "] not found");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(shaderStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            logger.info("Shader {} loaded from source", shaderName);
            return sb.toString();
        }
    }

    /**
     * Links current shader program. Should be called after compiling shaders.
     */
    private void linkProgram() throws ShaderException {
        glLinkProgram(this.programId);
        if (glGetProgrami(this.programId, GL_LINK_STATUS) == GL_FALSE)
            throw new ShaderException("Failed to link shader program", null);
    }

    /**
     * Compiles a single GLSL shader of the given type and attach it to shader
     * program.
     *
     * @param source     GLSL shader source code
     * @param shaderType OpenGL shader type (e.g. {@code GL_VERTEX_SHADER})
     * @param shaderName GLSL shader file name
     * @throws ShaderException If compilation fails
     */
    private void compileShader(String source, int shaderType, String shaderName) throws ShaderException {
        switch (shaderType) {
            // Source the shader, compile it and attach to shader program
            case GL_VERTEX_SHADER:
                glShaderSource(this.vertexShaderId, source);
                glCompileShader(this.vertexShaderId);

                if (glGetShaderi(this.vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE)
                    throw new ShaderException("Failed to compile vertex shader", shaderName,
                            glGetShaderInfoLog(this.vertexShaderId));
                glAttachShader(this.programId, this.vertexShaderId);
                break;
            case GL_FRAGMENT_SHADER:
                glShaderSource(this.fragmentShaderId, source);
                glCompileShader(this.fragmentShaderId);

                if (glGetShaderi(this.fragmentShaderId, GL_COMPILE_STATUS) == GL_FALSE)
                    throw new ShaderException("Failed to compile fragment shader", shaderName,
                            glGetShaderInfoLog(this.fragmentShaderId));
                glAttachShader(this.programId, this.fragmentShaderId);
            default:
                break;
        }
    }

    /**
     * Frees the OpenGL shaders and program. Must be called when the shader is no
     * longer needed!
     */
    public void dispose() {
        glDeleteProgram(this.programId);
        glDeleteShader(this.vertexShaderId);
        glDeleteShader(this.fragmentShaderId);

        logger.info("Shaders ({}, {}) and program ({}) disposed", this.vertexShaderId, this.fragmentShaderId,
                this.programId);
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