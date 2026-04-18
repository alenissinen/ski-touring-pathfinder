package exceptions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ShaderExceptionTest {

    @Test
    void constructor_messageContainsPath() {
        ShaderException exception = new ShaderException("compile failed", "chunk.frag");
        assertTrue(exception.getMessage().contains("chunk.frag"));
    }

    @Test
    void constructor_messageContainsErrorMessage() {
        ShaderException exception = new ShaderException("compile failed", "chunk.frag");
        assertTrue(exception.getMessage().contains("compile failed"));
    }

    @Test
    void constructorWithLog_messageContainsPath() {
        ShaderException exception = new ShaderException("link failed", "chunk.frag", "ERROR: test");
        assertTrue(exception.getMessage().contains("chunk.frag"));
    }

    @Test
    void constructorWithLog_messageContainsErrorMessage() {
        ShaderException exception = new ShaderException("link failed", "chunk.frag", "ERROR: test");
        assertTrue(exception.getMessage().contains("link failed"));
    }

    @Test
    void constructorWithLog_messageContainsLog() {
        ShaderException exception = new ShaderException("link failed", "chunk.frag", "ERROR: test");
        assertTrue(exception.getMessage().contains("ERROR: test"));
    }
}
