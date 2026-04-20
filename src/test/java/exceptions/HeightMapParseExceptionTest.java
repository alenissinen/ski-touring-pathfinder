package exceptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@Severity(SeverityLevel.MINOR)
public class HeightMapParseExceptionTest {
    @Test
    void constructor_messageContainsPath() {
        HeightMapParseException exception = new HeightMapParseException("test", "a.asc");
        assertTrue(exception.getMessage().contains("a.asc"));
    }

    @Test
    void constructor_messageContainsErrorMessage() {
        HeightMapParseException exception = new HeightMapParseException("test", "a.asc");
        assertTrue(exception.getMessage().contains("test"));
    }

    @Test
    void constructorIncludingCause_messageContainsPath() {
        HeightMapParseException exception = new HeightMapParseException("test", new RuntimeException("some exception"),
                "a.asc");
        assertTrue(exception.getMessage().contains("a.asc"));
    }

    @Test
    void constructorIncludingCause_messageContainsErrorMessage() {
        HeightMapParseException exception = new HeightMapParseException("test", new RuntimeException("some exception"),
                "a.asc");
        assertTrue(exception.getMessage().contains("test"));
    }

    @Test
    void constructorIncludingCause_messageContainsCause() {
        HeightMapParseException exception = new HeightMapParseException("test", new RuntimeException("some exception"),
                "a.asc");
        assertTrue(exception.getMessage().contains("some exception"));
    }
}
