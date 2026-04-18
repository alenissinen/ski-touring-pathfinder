package pathfinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NodeTest {
    @Test
    void constructor_setsCoordinates() {
        Node node = new Node(1f, 1f);
        assertEquals(1f, node.getX());
        assertEquals(1f, node.getZ());
    }

    @Test
    void constructor_negativeCoordinates() {
        Node node = new Node(-1f, -1f);
        assertEquals(-1f, node.getX());
        assertEquals(-1f, node.getZ());
    }

    @Test
    void constructor_defaultSlopeAngle_isNegativeOne() {
        Node node = new Node(1f, 1f);
        assertEquals(-1f, node.getSlopeAngle());
    }

    @Test
    void constructor_defaultEleveation_isZero() {
        Node node = new Node(1f, 1f);
        assertEquals(0f, node.getY());
    }

    @Test
    void constructor_defaultParent_isNull() {
        Node node = new Node(1f, 1f);
        assertNull(node.getParent());
    }

    @Test
    void setCosts_valuesAreCorrect() {
        Node node = new Node(1f, 1f);
        node.setCosts(3f, 6f);

        assertEquals(3f, node.getG());
        assertEquals(6f, node.getH());
        assertEquals(9f, node.getF());
    }

    @Test
    void setCosts_zeroCost() {
        Node node = new Node(1f, 1f);
        node.setCosts(0, 0);
        assertEquals(0f, node.getF());
    }

    @Test
    void setCosts_overwritesPreviousCost() {
        Node node = new Node(1f, 1f);
        node.setCosts(6f, 10f);
        node.setCosts(3f, 6f);

        assertEquals(3f, node.getG());
        assertEquals(6f, node.getH());
        assertEquals(9f, node.getF());
    }

    @Test
    void setElevation_setsY() {
        Node node = new Node(1f, 1f);
        node.setElevation(50f);
        assertEquals(50f, node.getY());
    }

    @Test
    void setSlopeAngle_setCorrectly() {
        Node node = new Node(1f, 1f);
        node.setSlopeAngle(12f);
        assertEquals(12f, node.getSlopeAngle());
    }

    @Test
    void setParent_setCorrectly() {
        Node node = new Node(1f, 1f);
        Node parent = new Node(1f, 2f);
        node.setParent(parent);

        assertSame(parent, node.getParent());
    }

    @Test
    void compareTo_lowerF_returnsNegative() {
        Node node = new Node(1f, 1f);
        Node other = new Node(2f, 2f);
        node.setCosts(1f, 1f);
        other.setCosts(2f, 2f);

        assertTrue(node.compareTo(other) < 0);
    }

    @Test
    void compareTo_higherF_returnsPositive() {
        Node node = new Node(1f, 1f);
        Node other = new Node(2f, 2f);
        node.setCosts(1f, 1f);
        other.setCosts(2f, 2f);

        assertTrue(other.compareTo(node) > 0);
    }

    @Test
    void compareTo_equalF_returnsZero() {
        Node node = new Node(1f, 1f);
        Node other = new Node(2f, 2f);
        node.setCosts(1f, 1f);
        other.setCosts(1f, 1f);

        assertTrue(node.compareTo(other) == 0);
    }

    @Test
    void toString_correctFormat() {
        Node node = new Node(1f, 1f);
        assertEquals("(1.0, 1.0)", node.toString());
    }
}
