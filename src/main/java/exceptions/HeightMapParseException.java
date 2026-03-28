package exceptions;

import java.io.IOException;

public class HeightMapParseException extends IOException {
    /**
     * Constructs a new {@code HeightMapParseException} with a message and the
     * path of the file that failed to parse.
     *
     * @param message Error message
     * @param filePath Path of the height map file that caused the error
     */
    public HeightMapParseException(String message, String filePath) {
        super("[file: " + filePath + "]: " + message);
    }

    /**
     * Constructs a new {@code HeightMapParseException} with a message, the
     * path of the file that failed to parse and the underlying cause.
     *
     * @param message Error message
     * @param cause The underlying exception
     * @param filePath Path of the height map file that caused the error
     */
    public HeightMapParseException(String message, Throwable cause, String filePath) {
        super("[file: " + filePath + "]: " + message + "\n" + cause);
    }
}
