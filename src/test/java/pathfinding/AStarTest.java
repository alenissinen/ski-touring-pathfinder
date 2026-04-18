package pathfinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import terrain.HeightMap;

public class AStarTest {
    private HeightMap flat;
    private AStar flatAStar;

    @BeforeEach
    void setUp() {
        // 7x7 flat map (100m elevation)
        float[][] flatData = new float[7][7];
        for (int r = 0; r < 7; r++)
            for (int c = 0; c < 7; c++)
                flatData[r][c] = 100.0f;

        this.flat = new HeightMap(flatData, flatData[0].length, flatData.length, 0.0, 0.0, 2.0);
        this.flatAStar = new AStar(this.flat);
    }

    @Test
    void init_setsRunningToTrue() {
        this.flatAStar.init(0, 0, 3, 3);
        assertTrue(this.flatAStar.isRunning());
    }

    @Test
    void init_resetsState() {
        this.flatAStar.init(0, 0, 2, 2);
        // Run the old iterator to completion and init new one
        this.runIterator(this.flatAStar, 1000);

        // Initialize new path and check that state is fully reseted
        this.flatAStar.init(0, 0, 3, 3);
        assertTrue(this.flatAStar.isRunning());
        assertNull(this.flatAStar.getPath());
        assertFalse(this.flatAStar.getUnModifiableOpenSet().isEmpty());
    }

    @Test
    void init_pathIsNullBeforeFirstStep() {
        this.flatAStar.init(0, 0, 3, 3);
        assertNull(this.flatAStar.getPath());
    }

    @Test
    void init_openSetContainsStartNode() {
        this.flatAStar.init(0, 0, 2, 2);
        assertEquals(1, this.flatAStar.getUnModifiableOpenSet().size());
    }

    @Test
    void step_returnsTrue_whileSearching() {
        this.flatAStar.init(0, 0, 2, 2);
        assertTrue(this.flatAStar.step());
    }

    @Test
    void step_returnsFalse_whenNotInitialized() {
        assertFalse(this.flatAStar.step());
    }

    @Test
    void step_correctStateAfterGoalReached() {
        // Set adjacent goal node and run the iterator
        this.flatAStar.init(0, 0, 1, 0);
        this.runIterator(this.flatAStar, 1000);

        assertFalse(this.flatAStar.isRunning());
        assertNotNull(this.flatAStar.getPath());
    }

    @Test
    void step_setsCurrentNode() {
        this.flatAStar.init(0, 0, 2, 2);
        this.flatAStar.step();

        assertNotNull(this.flatAStar.getCurrentNode());
    }

    @Test
    void getPath_returnsNull_beforeInitialized() {
        assertNull(this.flatAStar.getPath());
    }

    @Test
    void getPath_firstNodeIsStartNode() {
        this.flatAStar.init(0, 0, 2, 2);
        this.runIterator(this.flatAStar, 1000);
        List<Node> path = this.flatAStar.getPath();

        assertEquals(0f, path.getFirst().getX());
        assertEquals(0f, path.getFirst().getZ());
    }

    @Test
    void getPath_lastNodeIsGoalNode() {
        this.flatAStar.init(0, 0, 2, 2);
        this.runIterator(this.flatAStar, 1000);
        List<Node> path = this.flatAStar.getPath();

        assertEquals(2f, path.getLast().getX());
        assertEquals(2f, path.getLast().getZ());
    }

    @Test
    void getPath_returnsSingleNodePath() {
        this.flatAStar.init(0, 0, 0, 0);
        this.runIterator(this.flatAStar, 1000);

        assertNotNull(this.flatAStar.getPath());
        assertEquals(1, this.flatAStar.getPath().size());
    }

    @Test
    void getPath_flatTerrain_findsDiagonalPath() {
        this.flatAStar.init(-3, -3, 2, 2);
        this.runIterator(this.flatAStar, 10000);

        // Optimal path should contain 6 nodes
        assertTrue(this.flatAStar.getPath().size() == 6);
    }

    @Test
    void getUnModifiableOpenSet_isUnmodifiable() {
        this.flatAStar.init(0, 0, 2, 2);
        assertThrows(UnsupportedOperationException.class,
                () -> this.flatAStar.getUnModifiableOpenSet().add(new Node(0, 0)));
    }

    @Test
    void isRunning_returnsFalse_afterPathFound() {
        this.flatAStar.init(-3, -3, 2, 2);
        this.runIterator(this.flatAStar, 10000);
        assertFalse(this.flatAStar.isRunning());
    }

    // Run the iterator for n iterations
    void runIterator(AStar aStar, int iterations) {
        int i = 0;
        while (aStar.step() && i++ < iterations) {
        }
    }
}
