package rendering;

import exceptions.ShaderException;

import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL20C.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20C.glAttachShader;
import static org.lwjgl.opengl.GL20C.glCompileShader;
import static org.lwjgl.opengl.GL20C.glCreateProgram;
import static org.lwjgl.opengl.GL20C.glCreateShader;
import static org.lwjgl.opengl.GL20C.glDeleteProgram;
import static org.lwjgl.opengl.GL20C.glDeleteShader;
import static org.lwjgl.opengl.GL20C.glGetProgrami;
import static org.lwjgl.opengl.GL20C.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20C.glShaderSource;
import static org.lwjgl.opengl.GL20C.glUniform4f;
import static org.lwjgl.opengl.GL20C.glGetShaderi;
import static org.lwjgl.opengl.GL20C.glGetUniformLocation;
import static org.lwjgl.opengl.GL20C.glLinkProgram;
import static org.lwjgl.opengl.GL20C.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20C.glUseProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

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
    /** OpenGL handle for the compiled and linked shader program */
    private int programId;

    /** OpenGL handle for the vertex shader */
    private int vertexShaderId;

    /** OpenGL handle for the fragment shader */
    private int fragmentShaderId;

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
        } catch (IOException e) {
            throw new ShaderException("Failed to read vertex shader", vertexShader);
        }

        try {
            String fragment = this.loadSource(fragmentShader);
            this.compileShader(fragment, GL_VERTEX_SHADER, fragmentShader);
        } catch (IOException e) {
            throw new ShaderException("Failed to read fragment shader", fragmentShader);
        }

        // Link the program
        this.linkProgram();
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
        // Create new float buffer
        FloatBuffer floatBuf = BufferUtils.createFloatBuffer(16);
        value.get(floatBuf);
        floatBuf.flip();

        int uniformLocation = glGetUniformLocation(this.programId, name);
        glUniformMatrix4fv(uniformLocation, false, floatBuf);
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
        try (
                InputStreamReader reader = new InputStreamReader(shaderStream);
                BufferedReader buffered = new BufferedReader(reader);) {
            return buffered.toString();
        } catch (IOException e) {
            throw new IOException("[shader: " + shaderName + "] failed to read", e);
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

                if (glGetShaderi(this.vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE)
                    throw new ShaderException("Failed to compile fragment shader", shaderName,
                            glGetShaderInfoLog(this.vertexShaderId));
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