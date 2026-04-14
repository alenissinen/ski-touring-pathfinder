package pathfinding;

/**
 * Represents a single node in the A* graph, stores its position, movement
 * costs,
 * and reference to its parent node.
 *
 * <p>
 * Implements {@link Comparable} so that nodes can be ordered by their {@code f}
 * value
 * in a {@link java.util.PriorityQueue}.
 * </p>
 *
 * <p>
 * Cost values:
 * </p>
 * <ul>
 * <li>{@code g} - actual movement cost from the start node to current node</li>
 * <li>{@code h} - heuristic estimate of the cost from current node to the
 * goal</li>
 * <li>{@code f} - total estimated cost: {@code g + h}</li>
 * </ul>
 *
 * @see <a href="https://www.datacamp.com/tutorial/a-star-algorithm">Datacamp A*
 *      guide</a>
 */
public class Node implements Comparable<Node> {
    /** Grid X coordinate of this node */
    private final float x;

    /** Grid Z coordinate of this node */
    private final float z;

    /** Slope angle in degrees at current grid position */
    private float slopeAngle = -1.0f;

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
    public Node(float x, float z) {
        this.x = x;
        this.z = z;
    }

    /**
     * @return Grid X coordinate
     */
    public float getX() {
        return this.x;
    }

    /**
     * @return Grid Z coordinate
     */
    public float getZ() {
        return this.z;
    }

    /**
     * @return Actual movement cost from start node to current node
     */
    public float getG() {
        return this.g;
    }

    /**
     * @return Heuristic estimate from current node to goal
     */
    public float getH() {
        return this.h;
    }

    /**
     * @return Total estimated cost
     */
    public float getF() {
        return this.f;
    }

    /**
     * @return Slope angle in degrees at current nodes position
     */
    public float getSlopeAngle() {
        return this.slopeAngle;
    }

    /**
     * @return Parent node, returns {@code null} if current node is start node
     */
    public Node getParent() {
        return this.parent;
    }

    /**
     * Sets the movement costs for this node and updates {@link #f} automatically.
     *
     * @param g Actual cost from start
     * @param h Heuristic estimate to goal
     */
    public void setCosts(float g, float h) {
        this.g = g;
        this.h = h;
        this.f = g + h;
    }

    /**
     * @param slopeAngle Slope angle in degrees at current nodes position
     */
    public void setSlopeAngle(float slopeAngle) {
        this.slopeAngle = slopeAngle;
    }

    /**
     * @param parent Parent node in the pathfinding tree
     */
    public void setParent(Node parent) {
        this.parent = parent;
    }

    /**
     * Compares current node to another node by {@code f} cost.
     *
     * @param other The node to be compared
     * @return Negative value if this node has lower cost, positive if higher, zero
     *         if equal
     */
    @Override
    public int compareTo(Node other) {
        return Float.compare(this.f, other.f);
    }

    /** Describes how {@code Node} is printed to STDOUT */
    @Override
    public String toString() {
        return String.format("(%s, %s)", this.x, this.z);
    }
}
