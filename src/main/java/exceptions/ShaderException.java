package exceptions;

/**
 * Thrown when a GLSL shader fails to compile, load or link.
 *
 * <p>
 * At least for now extends {@link RuntimeException} because the application
 * can't render without a working shader. Might change this later e.g. try to
 * recompile
 * the shader etc. if I figure out a way.
 * </p>
 */
public class ShaderException extends RuntimeException {
    /**
     * Constructs a new {@code ShaderException} with a message and the path of the
     * shader.
     * 
     * @param message  Error message
     * @param filePath Path of the shader that caused the error
     */
    public ShaderException(String message, String filePath) {
        super("[file: " + filePath + "]: " + message);
    }

    /**
     * Constructs a new {@code ShaderException} with a message and the path of the
     * shader.
     * 
     * @param message  Error message
     * @param filePath Path of the shader that caused the error
     * @param log      OpenGL compiler or linker error log
     */
    public ShaderException(String message, String filePath, String log) {
        super("[file: " + filePath + "]: " + message + "\n" + log);
    }
}
