package terrain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HeightMapTest {
    private HeightMap flat;
    private HeightMap slope;

    @BeforeEach
    void setUp() {
        // Flat 3x3 area
        float[][] flatData = {
                { 15.0f, 15.0f, 15.0f },
                { 15.0f, 15.0f, 15.0f },
                { 15.0f, 15.0f, 15.0f }
        };
        flat = new HeightMap(flatData, 3, 3, 0.0, 0.0, 2.0);

        // Slope where height is different
        float[][] slopeData = {
                { 5.4f, 6.9f, 7.2f },
                { 5.2f, 8.2f, 8.3f },
                { 3.5f, 4.8f, 6.8f }
        };
        slope = new HeightMap(slopeData, 3, 3, 0.0, 0.0, 2.0);
    }

    @Test
    void getElevation_returnsCorrectResult() {
        assertEquals(4.8f, slope.getElevation(1, 2));
    }

    @Test
    void getWidth_returnsCorrectResult() {
        assertEquals(3, flat.getWidth());
    }

    @Test
    void getHeight_returnsCorrectResult() {
        assertEquals(3, flat.getHeight());
    }

    @Test
    void getSlopeAngle_flat_returnsZero() {
        assertEquals(0.0, flat.getSlopeAngle(1, 1), 0.1);
    }

    @Test
    void getSlopeAngle_edgeCell_returnsZero() {
        assertEquals(0.0, flat.getSlopeAngle(0, 0), 0.1);
    }

    @Test
    void getSlopeAngle_slope_returnsCorrectResult() {
        assertEquals(39.2, slope.getSlopeAngle(1, 1), 0.1);
    }
}
