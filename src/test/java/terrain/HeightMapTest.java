package terrain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HeightMapTest {
    private HeightMap flat;
    private HeightMap slope;
    private HeightMap large;

    @BeforeEach
    void setUp() {
        // Flat 3x3 area
        float[][] flatData = {
                { 15.0f, 15.0f, 15.0f },
                { 15.0f, 15.0f, 15.0f },
                { 15.0f, 15.0f, 15.0f }
        };
        this.flat = new HeightMap(flatData, 3, 3, 0.0, 0.0, 2.0);

        // Slope where height is different
        float[][] slopeData = {
                { 5.4f, 6.9f, 7.2f },
                { 5.2f, 8.2f, 8.3f },
                { 3.5f, 4.8f, 6.8f }
        };
        this.slope = new HeightMap(slopeData, 3, 3, 0.0, 0.0, 2.0);

        // Larger map
        float[][] largeData = {
                { 100, 102, 104, 102, 100 },
                { 102, 106, 108, 106, 102 },
                { 104, 108, 110, 108, 104 },
                { 102, 106, 108, 106, 102 },
                { 100, 102, 104, 102, 100 },
        };
        this.large = new HeightMap(largeData, 5, 5, 0.0, 0.0, 2.0);
    }

    @Test
    void getElevation_returnsCorrectResult() {
        assertEquals(4.8f, this.slope.getElevation(0, 1));
    }

    @Test
    void getElevation_centerCell_returnsCorrectValue() {
        assertEquals(8.2f, this.slope.getElevation(0, 0));
    }

    @Test
    void getElevation_oobPositive_clampsToBottomRight() {
        assertEquals(6.8f, this.slope.getElevation(111, 111));
    }

    @Test
    void getElevation_oobNegative_clampsToTopLeft() {
        assertEquals(5.4f, this.slope.getElevation(-222, -111));
    }

    @Test
    void getWidth_returnsCorrectResult() {
        assertEquals(3, this.flat.getWidth());
    }

    @Test
    void getHeight_returnsCorrectResult() {
        assertEquals(3, this.flat.getHeight());
    }

    @Test
    void getSlopeAngle_flat_returnsZero() {
        assertEquals(0.0f, this.flat.getSlopeAngle(0, 0), 0.1);
    }

    @Test
    void getSlopeAngle_edgeCell_returnsZero() {
        assertEquals(0.0f, this.flat.getSlopeAngle(1, 0), 0.1);
    }

    @Test
    void getSlopeAngle_slope_returnsCorrectResult() {
        assertEquals(39.2f, this.slope.getSlopeAngle(0, 0), 0.1);
    }

    @Test
    void isLogicalOnGrid_centerCell_returnsTrue() {
        assertTrue(this.flat.isLogicalOnGrid(0, 0));
    }

    @Test
    void isLogicalOnGrid_edgeCell_returnsTrue() {
        assertTrue(this.flat.isLogicalOnGrid(1, 1));
    }

    @Test
    void isLogicalOnGrid_outsideCell_returnsFalse() {
        assertFalse(this.flat.isLogicalOnGrid(2, 2));
    }

    @Test
    void interpolateElevation_flat_returnsExactElevation() {
        assertEquals(15.0f, this.flat.interpolateElevation(0f, 0f));
    }

    @Test
    void interpolateElevation_betweenCells_returnsAverage() {
        assertEquals(109.0f, this.large.interpolateElevation(1, 0));
    }

    @Test
    void getDataMinElevation_returnsLowest() {
        assertEquals(3.5f, this.slope.getDataMinElevation());
    }

    @Test
    void getDataMaxElevation_returnsHighest() {
        assertEquals(8.3f, this.slope.getDataMaxElevation());
    }

    @Test
    void merge_null_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> HeightMap.merge(null));
    }

    @Test
    void merge_empty_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> HeightMap.merge(List.of()));
    }

    @Test
    void merge_oneMap_returnsSameInstance() {
        assertSame(this.flat, HeightMap.merge(List.of(this.flat)));
    }

    @Test
    void merge_twoMapsHorizontally_correctDimensions() {
        float[][] data = {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 8, 9 }
        };

        HeightMap a = new HeightMap(data, 3, 3, 0.0, 0.0, 2.0);
        HeightMap b = new HeightMap(data, 3, 3, 6.0, 0.0, 2.0);

        HeightMap merged = HeightMap.merge(List.of(a, b));
        assertEquals(6, merged.getWidth());
        assertEquals(3, merged.getHeight());
    }

    @Test
    void merge_differentCellSize_throwsIllegalArgument() {
        float[][] data = {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 8, 9 }
        };

        HeightMap a = new HeightMap(data, 3, 3, 0.0, 0.0, 2.0);
        HeightMap b = new HeightMap(data, 3, 3, 6.0, 0.0, 4.0);

        assertThrows(IllegalArgumentException.class, () -> HeightMap.merge(List.of(a, b)));
    }

    @Test
    void merge_gap_throwsIllegalArgument() {
        float[][] data = {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 8, 9 }
        };

        HeightMap a = new HeightMap(data, 3, 3, 0.0, 0.0, 2.0);
        HeightMap b = new HeightMap(data, 3, 3, 10.0, 0.0, 2.0);

        assertThrows(IllegalArgumentException.class, () -> HeightMap.merge(List.of(a, b)));
    }

    @Test
    void getCellSize_returnsCorrect() {
        assertEquals(2.0, this.flat.getCellSize());
    }
}
