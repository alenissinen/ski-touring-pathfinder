package rendering;

import exceptions.ShaderException;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Utility class for loading, compiling, and managing GLSL shader programs.
 *
 * <p>Loads vertex and fragment shaders from {@code resources/shaders/}.
 * {@link #bind()} must be called before using draw calls or setting uniforms!</p>
 *
 * <p>Must be cleaned up with {@link #dispose()} to free GPU resources!
 * Implements {@link AutoCloseable} to support try-with-resources.</p>
 */
public class Shader implements AutoCloseable {
    /** OpenGL handle for the compiled and linked shader program */
    private int programId;

    /** OpenGL handle for the vertex shader */
    private int vertexShaderId;

    /** OpenGL handle for the fragment shader */
    private int fragmentShaderId;

    /**
     * Loads, compiles and links a shader program from vertex and fragment shader files.
     * Shaders must be located in {@code resources/shaders/}!
     *
     * @param vertexShader Name of the vertex shader
     * @param fragmentShader Name of the fragment shader
     * @throws ShaderException If one of the shaders fails to compile or the combined
     *         program fails to link
     */
    public Shader(String vertexShader, String fragmentShader) throws ShaderException {}

    /** Binds the shader program as the active OpenGL shader */
    public void bind() {}

    /** Unbinds the shader program */
    public void unbind() {}

    /**
     * Sets a {@code mat4} uniform value in the shader.
     *
     * @param name Name of the uniform value in the shader
     * @param value 4x4 matrix ({@link Matrix4f})
     */
    public void setMat4(String name, Matrix4f value) {}

    /**
     * Sets a {@code vec4} uniform value in the shader.
     *
     * @param name Name of the uniform value in the shader
     * @param value 4 component vector ({@link Vector4f})
     */
    public void setVec4(String name, Vector4f value) {}

    /**
     * Reads a GLSL shader file from {@code resources/shaders/}.
     *
     * @param shaderName Shader filename
     * @return Shader source code as a {@code String}
     * @throws ShaderException If the file cannot be read
     */
    private String loadSource(String shaderName) throws ShaderException {}

    /**
     * Compiles a single GLSL shader of the given type.
     *
     * @param source GLSL shader source code
     * @param shaderType OpenGL shader type (e.g. {@code GL_VERTEX_SHADER})
     * @return OpenGL handle of the compiled shader
     * @throws ShaderException If compilation fails
     */
    private int compileShader(String source, int shaderType)  throws ShaderException {}

    /**
     * Frees the OpenGL shader program. Must be called when the shader is no longer
     * needed!
     */
    public void dispose() {}

    /**
     * Implements {@link AutoCloseable} by calling {@link #dispose()}.
     * Call {@link #dispose()} directly in non-try-with-resources context.
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">Oracle docs</a>
     */
    @Override
    public void close() {
        dispose();
    }
}