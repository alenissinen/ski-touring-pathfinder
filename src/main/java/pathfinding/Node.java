package pathfinding;

/**
 * Represents a single node in the A* graph, stores its position, movement costs,
 * and reference to its parent node.
 *
 * <p>Implements {@link Comparable} so that nodes can be ordered by their {@code f} value
 * in a {@link java.util.PriorityQueue}.</p>
 *
 * <p>Cost values:</p>
 * <ul>
 *     <li>{@code g} - actual movement cost from the start node to current node</li>
 *     <li>{@code h} - heuristic estimate of the cost from current node to the goal</li>
 *     <li>{@code f} - total estimated cost: {@code g + h}</li>
 * </ul>
 *
 * @see <a href="https://www.datacamp.com/tutorial/a-star-algorithm">Datacamp A* guide</a>
 */
public class Node implements Comparable<Node> {
    /** Grid X coordinate of this node */
    private final int x;

    /** Grid Z coordinate of this node */
    private final int z;

    /** Slope angle in degrees at current grid position */
    private float slopeAngle;

    /** Actual movement cost from the start node to this node */
    private float g;

    /** Heuristic estimate of the cost from this node to the goal */
    private float h;

    /** Total estimated cost: {@code g + h} */
    private float f;

    /** Parent node in the pathfinding tree */
    private Node parent;

    /**
     * Constructs a new {@code Node} at the given grid position.
     *
     * @param x Grid X position
     * @param z Grid Z position
     */
    public Node(int x, int z) {}

    /**
     * Compares current node to another node by {@code f} cost.
     *
     * @param other The node to be compared
     * @return Negative value if this node has lower cost, positive if higher, zero if equal
     */
    @Override
    public int compareTo(Node other) {}

    /**
     * @return Grid X coordinate
     */
    public int getX() {}

    /**
     * @return Grid Z coordinate
     */
    public int getZ() {}

    /**
     * @return Actual movement cost from start node to current node
     */
    public float getG() {}

    /**
     * @return Heuristic estimate from current node to goal
     */
    public float getH() {}

    /**
     * @return Total estimated cost
     */
    public float getF() {}

    /**
     * @return Slope angle in degrees at current nodes position
     */
    public float getSlopeAngle() {}

    /**
     * @return Parent node, returns {@code null} if current node is start node
     */
    public Node getParent() {}

    /**
     * Sets the movement costs for this node and updates {@link #f} automatically.
     *
     * @param g Actual cost from start
     * @param h Heuristic estimate to goal
     */
    public void setCosts(float g, float h) {}

    /**
     * @param slopeAngle Slope angle in degrees at current nodes position
     */
    public void setSlopeAngle(float slopeAngle) {}

    /**
     * @param parent Parent node in the pathfinding tree
     */
    public void setParent(Node parent) {}
}
