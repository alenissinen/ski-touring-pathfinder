package pathfinding;

import terrain.HeightMap;
import application.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the optimal skinning route between two points on the terrain using
 * A* pathfinding algorithm. Stores the resulting path after calculation.
 *
 * <p>
 * Movement is allowed in 8 directions. The {@code g} cost of each step is
 * calculated by distance to neighbor (2m for cardinal, 2m*sqrt(2)=~2.83m for
 * diagonal)
 * and slope angle.
 * </p>
 *
 * <p>
 * The heuristic value {@code h} is the Euclidean distance to the goal, which
 * should work while supporting diagonal movement. It also shouldn't
 * overestimate
 * the true cost.
 * </p>
 * 
 * <p>
 * The AStar implementation is iterative. Call {@link #init} to create a new
 * iterator and
 * {@link #step} once per frame to advance the search without blocking the
 * main thread.
 * </p>
 */
public class AStar {
    private static final Logger logger = LoggerFactory.getLogger(AStar.class);

    /** Reference to the height map used for elevation and slope angle */
    private final HeightMap heightMap;

    /**
     * Open set includes discovered but not yet evaluated nodes, ordered by
     * {@code f} cost
     */
    private final PriorityQueue<Node> openSet;

    /** HashSet for O(1) lookup whether openSet contains node */
    private final HashSet<Long> openSetContains;

    /** Closed set includes keys of nodes already evaluated */
    private final HashSet<Long> closedSet;

    /** Optimal path after search completes */
    private List<Node> path;

    /** Goal node */
    private Node goalNode;

    /** Current node for iterator */
    private Node currentNode;

    /** Is pathfinding active */
    private boolean running;

    /** All possible offsets for neighbours */
    private static final int[][] POSSIBLE_MOVES = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
            { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 }
    };

    /**
     * Constructs a new {@code AStar} instance for the given height map.
     *
     * @param heightMap Current height map in use
     */
    public AStar(HeightMap heightMap) {
        this.heightMap = heightMap;
        this.openSet = new PriorityQueue<Node>();
        this.closedSet = new HashSet<Long>();
        this.openSetContains = new HashSet<Long>();
        this.running = false;
    }

    /**
     * Initializes new A* iterator. Call {@link #step} each frame!
     *
     * @param startX Grid X coordinate of the start point
     * @param startZ Grid Z coordinate of the start point
     * @param goalX  Grid X coordinate of the goal point
     * @param goalZ  Grid Z coordinate of the goal point
     */
    public void init(float startX, float startZ, float goalX, float goalZ) {
        // Reset variables
        this.openSet.clear();
        this.closedSet.clear();
        this.openSetContains.clear();
        this.path = null;
        this.running = true;

        Node start = new Node(startX, startZ);
        this.goalNode = new Node(goalX, goalZ);

        start.setCosts(0f, calculateH(start, goalNode));
        openSet.add(start);
        openSetContains.add(packKey(startX, startZ));
    }

    /**
     * Advances the pathfinding search by one iteration.
     *
     * @return {@code true} if active, {@code false} if finished
     */
    public boolean step() {
        // Check if iterator is active or if there is something to iterate over
        if (!this.running || this.openSet.isEmpty()) {
            this.running = false;
            return false;
        }

        // Poll node with lowest f value and set its elevation value
        this.currentNode = openSet.poll();
        this.currentNode
                .setElevation(
                        this.heightMap.getElevation((int) this.currentNode.getX(), (int) this.currentNode.getZ()));

        // Check if current node is the goal
        if (this.currentNode.getX() == this.goalNode.getX() && this.currentNode.getZ() == this.goalNode.getZ()) {
            this.path = this.reconstructPath(this.currentNode);
            logger.debug("Path found: nodes = {}, openSet.size() = {}, closedSet.size() = {}",
                    path.size(), openSet.size(), closedSet.size());
            this.running = false;
            return false;
        }

        // Move current node to closed set
        this.closedSet.add(packKey(this.currentNode.getX(), this.currentNode.getZ()));

        // Evaluate each neighbour
        for (int[] move : POSSIBLE_MOVES) {
            float x = this.currentNode.getX() + move[0];
            float z = this.currentNode.getZ() + move[1];

            // Boundary check
            int boundaryWidth = heightMap.getWidth() / 2;
            int boundaryHeight = heightMap.getHeight() / 2;

            if ((int) x < -boundaryWidth || (int) x >= boundaryWidth || (int) z < -boundaryHeight
                    || (int) z >= boundaryHeight)
                continue;

            long key = this.packKey(x, z);

            // Skip already evaluated nodes
            if (this.closedSet.contains(key))
                continue;

            Node neighbour = new Node(x, z);
            boolean isDiagonal = move[0] != 0 && move[1] != 0;
            float tentativeG = this.currentNode.getG() + calculateG(this.currentNode, neighbour, isDiagonal);
            float h = this.calculateH(neighbour, this.goalNode);

            neighbour.setParent(this.currentNode);
            neighbour.setCosts(tentativeG, h);

            // TODO: fix this mess
            if (!this.openSetContains.contains(key)) {
                this.openSet.add(neighbour);
                this.openSetContains.add(key);
            } else {
                // Find and remove the existing node with worse g cost
                Node existing = this.openSet.stream()
                        .filter(n -> n.getX() == x && n.getZ() == z)
                        .findFirst()
                        .orElse(null);

                if (existing != null && tentativeG < existing.getG()) {
                    this.openSet.remove(existing);
                    this.openSet.add(neighbour);
                }
            }
        }

        return true;
    }

    /**
     * Calculates {@code g} cost of moving from a node to its neighbor.
     * Slopes exceeding {@link Constants#SLOPE_THRESHOLD} receive an additional
     * multiplier
     * {@link Constants#SLOPE_PENALTY}.
     *
     * @param current    The node being evaluated
     * @param neighbour  The neighbor node being evaluated
     * @param isDiagonal Is the move diagonal
     * @return Movement cost from {@code current} to {@code neighbor}
     */
    private float calculateG(Node current, Node neighbour, boolean isDiagonal) {
        // Diagonal moves are longer
        float distance = isDiagonal ? Constants.WORLD_SCALE * 1.41421356f : Constants.WORLD_SCALE;

        // Height difference between current and neighbor node
        float heightDiff = Math.abs(
                heightMap.getElevation((int) neighbour.getX(), (int) neighbour.getZ()) -
                        heightMap.getElevation((int) current.getX(), (int) current.getZ()));

        // Apply slope penalty if slope exceeds threshold
        if (neighbour.getSlopeAngle() == -1.0f)
            neighbour.setSlopeAngle((float) heightMap.getSlopeAngle((int) neighbour.getX(),
                    (int) neighbour.getZ()));

        float slope = neighbour.getSlopeAngle();
        float slopeMultiplier = slope > Constants.SLOPE_THRESHOLD ? Constants.SLOPE_PENALTY : 1.0f;

        return (distance + heightDiff) * slopeMultiplier;
    }

    /**
     * Calculates {@code h} (heuristic) cost from a node to the goal.
     * Uses Euclidean distance.
     *
     * @param node  The node to calculate distance from
     * @param other The node to calculate distance to
     * @return Euclidean distance from {@code node} to the goal
     */
    private float calculateH(Node node, Node other) {
        float dx = node.getX() - other.getX();
        float dz = node.getZ() - other.getZ();

        return (float) Math.sqrt(dx * dx + dz * dz) * Constants.WORLD_SCALE;
    }

    /**
     * Recontsructs the path by following parent references from the goal node
     * back to the start node. The resulting list is ordered from start to goal.
     * 
     * @param goal The goal node from found path
     * @return Ordered list of nodes from start to goal
     */
    private List<Node> reconstructPath(Node goal) {
        List<Node> path = new ArrayList<Node>();
        Node current = goal;
        path.add(goal);

        while ((current = current.getParent()) != null)
            path.addFirst(current);

        return path;
    }

    /** Calculates a packed key for node to avoid string allocation */
    private long packKey(float x, float z) {
        return ((long) (int) x << 32) | ((int) z & 0xFFFFFFFFL);
    }

    /** @return Currently evaluated node, {@code null} if not running */
    public Node getCurrentNode() {
        return this.currentNode;
    }

    /** @return Is the A* iterator active */
    public boolean isRunning() {
        return this.running;
    }

    /** @return Current open set as unmodifiable */
    public Collection<Node> getUnModifiableOpenSet() {
        return Collections.unmodifiableCollection(this.openSet);
    }

    /**
     * Returns the current path.
     * 
     * @return Ordered list of {@link Node} objects, or {@code null} if no path
     *         exists
     */
    public List<Node> getPath() {
        return this.path;
    }
}
