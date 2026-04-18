package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import exceptions.HeightMapParseException;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@Severity(SeverityLevel.BLOCKER)
public class HeightMapParserTest {
    @TempDir
    Path tempDir;

    private Path writeAsciiMapFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);

        return file;
    }

    @Test
    void parse_validFile_correctHeaderValues() throws IOException {
        Path file = this.writeAsciiMapFile("tempValid.asc", """
                ncols           3
                nrows           3
                xllcorner       122.000000
                yllcorner       165.000000
                cellsize        2.00000000
                NODATA_value    -9999.0000
                10.0 20.0 15.0
                15.0 10.0 20.0
                20.0 15.0 10.0
                """);

        HeightMapParser parser = new HeightMapParser(file.toString());
        HeightMap result = parser.parse();

        assertEquals(3, result.getWidth());
        assertEquals(3, result.getHeight());
        assertEquals(2.0, result.getCellSize());
    }

    @Test
    void parse_validFile_correctElevation() throws IOException {
        Path file = this.writeAsciiMapFile("tempValid.asc", """
                ncols           3
                nrows           3
                xllcorner       122.000000
                yllcorner       165.000000
                cellsize        2.00000000
                NODATA_value    -9999.0000
                10.0 20.0 15.0
                15.0 10.0 20.0
                20.0 15.0 10.0
                """);

        HeightMapParser parser = new HeightMapParser(file.toString());
        HeightMap result = parser.parse();

        assertEquals(10.0f, result.getElevation(0, 0));
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    void parse_nonExistentFile_throwsFileNotFound() {
        HeightMapParser parser = new HeightMapParser("tempInvalid.asc");
        assertThrows(FileNotFoundException.class, () -> parser.parse());
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    void parse_badHeader_throwsHeightMapParse() throws IOException {
        Path file = this.writeAsciiMapFile("tempValid.asc", """
                BADHEADER
                ncols           3
                nrows           3
                xllcorner       122.000000
                yllcorner       165.000000
                cellsize        2.00000000
                NODATA_value    -9999.0000
                10.0 20.0 15.0
                15.0 10.0 20.0
                20.0 15.0 10.0
                """);

        HeightMapParser parser = new HeightMapParser(file.toString());
        assertThrows(HeightMapParseException.class, () -> parser.parse());
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    void parse_lessRowsThanHeader_throwsHeightMapParse() throws IOException {
        Path file = this.writeAsciiMapFile("tempValid.asc", """
                ncols           3
                nrows           3
                xllcorner       122.000000
                yllcorner       165.000000
                cellsize        2.00000000
                NODATA_value    -9999.0000
                10.0 20.0 15.0
                """);

        HeightMapParser parser = new HeightMapParser(file.toString());
        assertThrows(HeightMapParseException.class, () -> parser.parse());
    }
}
