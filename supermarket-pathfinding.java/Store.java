import java.util.*;

/**
 * Represents a store.
 *
 * Instances of this class store the floor layout and the available products
 * of a store.
 */
public class Store {

	/**
	 * A simple coordinate.
	 *
	 * Instances of this class are created on demand for pathfinding - a store
	 * does *not* hold a persistent list of Positions internally, and all
	 * instances are discarded after a path is found.
	 */
	public static class Position {
		/**
		 * X/Y coordinates. These are `short` so they can be concatenated by
		 * `hashCode()` to produce values unique to a certain position.
		 */
		public short x;
		public short y;

		/**
		 * Stores the distance to the start position during A* pathfinding.
		 * This value is called `g` in the generic algorithm.
		 */
		public int distanceToStart;

		/**
		 * Distance to start plus shortest path to end.
		 * This value is used to prioritze tiles that are more likely to be on
		 * the shortest path, which is indicated by a low value.
		 */
		public int priority;

		/**
		 * The Position that precedes this one on the path. Used during A*
		 * pathfinding.
		 */
		public Position parent;

		/**
		 * Default constructor.
		 */
		public Position(short x, short y) {
			this.x = x;
			this.y = y;
			this.distanceToStart = 0;
			this.priority = 0;
		}

		/**
		 * Overloaded constructor because Java recognizes all integer literals
		 * as `int`, independent of their size, and can't convert to short on
		 * the fly.
		 */
		public Position(int x, int y) {
			this((short) x, (short) y);
		}

		@Override
		public int hashCode() {
			// Concatenate x and y.
			return (x << 16) | y;
		}

		/**
		 * Compare coordinates with `other`.
		 */
		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof Position))
				return false;
			Position p = (Position) other;
			return x == p.x && y == p.y;
		}

		@Override
		public String toString() {
			return String.format("Position(%d/%d)", x, y);
		}

		/**
		 * Append all `parent`s into a list object.
		 * Used during A* pathfinding.
		 */
		public List<Position> toList() {
			int capacity = 0;
			Position current = this;
			while(current != null) {
				capacity++;
				current = current.parent;
			}
			current = this;

			List<Position> l = new ArrayList<>(capacity);
			while (current != null) {
				l.add(current);
				current = current.parent;
			}
			Collections.reverse(l);
			return l;
		}

		/**
		 * Generate a hash value of the assigned positions by concatenating
		 * their hashes. Put the first value in the high bytes and the second
		 * value in the low bytes.
		 */
		public static Long hashPair(Position lhs, Position rhs) {
			int a = lhs.hashCode();
			int b = rhs.hashCode();
			return new Long( ((long) a << 32) | ((long) b & 0xFFFFFFFFL));
		}

		/**
		 * Return the minimum number of steps required to go from `lhs` to `rhs`
		 * without taking obstacles into account.
		 */
		public static int airlineDistanceBetween(Position lhs, Position rhs) {
			return Math.abs(lhs.x - rhs.x) + Math.abs(lhs.y - rhs.y);
		}
	}

	/**
	 * The floor layout. Each bit represents one position and stores the
	 * information whether it is blocked (1) or free (0).
	 * Use `Store::isBlocked()` to conveniently query the status of individual
	 * positions.
	 */
	private byte[] floor;

	/**
	 * Holds information how wide the floor is in bytes - that is, after how
	 * many bytes to apply a linebreak. The floor width is always a multiple of
	 * 8, so a break can only happen between bytes.
	 * For example, if `floor` stores 4 bytes and `floorWidth` is 2, the logical
	 * size of the floor is 16x2.
	 */
	private int floorWidth;

	/**
	 * The starting point for pathfinding.
	 */
	private Position entrance;

	/**
	 * The endpoint for pathfinding.
	 */
	private Position counter;

	/**
	 * List of articles available in this store.
	 * @todo Use actual `Article` objects once the class is available.
	 */
	private Map<Position, String> articles;

	/**
	 * Initialize the store and check assigned arguments for integrity.
	 *
	 * @throws IllegalArgumentException if `floor`, `entrance` or `counter` are
	 *         null.
	 * @throws IntegrityException if `floor` can't be divided by `floorWidth` or
	 *         an assigned `articles` position is outside of the floor area.
	 */
	public Store(byte[] floor,
				int floorWidth,
				Position entrance,
				Position counter,
				Map<Position, String> articles)
	throws IllegalArgumentException, IntegrityException
	{
		if (floor == null)
			throw new IllegalArgumentException("Argument `floor` missing.");
		if (entrance == null)
			throw new IllegalArgumentException("Argument `entrance` missing.");
		if (counter == null)
			throw new IllegalArgumentException("Argument `counter` missing.");
		if (floor.length % floorWidth != 0)
			throw new IntegrityException("Can't divide floor by floorWidth.");

		this.floor = floor;
		this.floorWidth = floorWidth;

		if (isBlocked(entrance))
			throw new IntegrityException("Position `entrance` is blocked."
				+ " (marked as blocked on the floor or outside of floor area)");
		if (isBlocked(entrance))
			throw new IntegrityException("Position `counter` is blocked."
				+ " (marked as blocked on the floor or outside of floor area)");
		for (Map.Entry<Position, String> article : articles.entrySet()) {
			if (isBlocked(article.getKey()))
				throw new IntegrityException(String.format("`%s` for article %s"
						+ " is blocked. (marked as blocked on the floor"
						+ " or outside of floor area)",
					article.getKey().toString(),
					article.getValue()));
		}

		this.entrance = entrance;
		this.counter = counter;
		this.articles = articles != null ? articles : new HashMap<>();
	}

	/**
	 * @return The logical width of the store.
	 */
	public int getWidth() {
		return floorWidth * 8;
	}

	/**
	 * @return The logical height of the store.
	 */
	public int getHeight() {
		return floor.length / floorWidth;
	}

	public Position getEntrance() {
		return entrance;
	}

	public Position getCounter() {
		return counter;
	}

	public Map<Position, String> getArticles() {
		return articles;
	}

	/**
	 * Check whether the assigned position is out of bounds or marked as blocked
	 * in `floor`.
	 *
	 * @param p The position that is to be checked.
	 * @return TRUE if the position is blocked, otherwise FALSE.
	 */
	public boolean isBlocked(Position p) {
		// Return true if the `p` is out of bounds.
		if (p.x < 0
			|| p.x >= floorWidth * 8
			|| p.y < 0
			|| p.y >= floor.length / floorWidth)
			return true;
		// From a logical perspective, `floor` is a 2-dimensional array of
	 	// booleans. However, it is implemented as a 1-dimensional byte array
	 	// to save memory. This means you have to perform two steps to find the
	 	// desired information.
	 	// In the first step you resolve the byte that stores the bit with:
	 	//   p.y * floorWidth + p.x / 8
	 	// because `p.y * floorWidth` steps as many bytes forward as there are
	 	// in a row for each step in y direction, and p.x steps an additional
	 	// 1/8th step forward because each byte (or index in `floor`) holds
	 	// eight values.
	 	// In the second step you retrieve the right bit from the indexed byte
	 	// with:
	 	//   128 >> p.x % 8
	 	// The result of `p.x % 8` gives you the remaining offset inside of the
	 	// selected byte, and the bitmask `0b10000000` (decimal 128) gets
	 	// shifted that many positions to the right.
		return (floor[p.y * floorWidth + p.x / 8] & (128 >> p.x % 8)) != 0;
	}

	/**
	 * Mark `p` as blocked on the map.
	 */
	public void block(Position p) {
		floor[p.y * floorWidth + p.x / 8] |= (128 >> p.x % 8);
	}

	/**
	 * Mark `p` as free on the map.
	 */
	public void free(Position p) {
		floor[p.y * floorWidth + p.x / 8] &= ~(128 >> p.x % 8);
	}

	/**
	 * Find the path with the shortest length that starts at `entrance`, passes
	 * all `articles`, and ends at `counter`.
	 *
	 * The current implementation uses a brute force approach which is only
	 * suitable for small numbers of articles.
	 * @todo Find an algorithm that doesn't need years to complete on a real
	 *       life shopping lists (> 30 items).
	 * @see http://stackoverflow.com/questions/222413/find-the-shortest-path-in
	 *      -a-graph-which-visits-certain-nodes#answer-228248
	 * @return The complete list of steps you have to go to walk (one of) the
	 *         shortest routes through the market that passes all articles.
	 *         Each step is represented as a `Position` object.
	 * @throws IntegrityException if not all positions can be reached.
	 */
	public List<Position> getOptimalPath() throws IntegrityException {
		Position[] mustPass = articles.keySet().toArray(new Position[0]);
		Map<Long, List<Position>> distances = new HashMap<>();

		// Calculate paths between every pair of positions. Reverse every path
		// to cover both directions.
		for (int i = 0; i < mustPass.length; i++) {
			Position a = mustPass[i];

			distances.put(Position.hashPair(entrance, a),
				findShortestPath(entrance, a));

			distances.put(Position.hashPair(a, counter),
				findShortestPath(a, counter));

			for (int j = i + 1; j < mustPass.length; j++) {
				Position b = mustPass[j];

				List<Position> l = findShortestPath(a, b);
				distances.put(Position.hashPair(a, b), l);
				l = new ArrayList<>(l);
				Collections.reverse(l);
				distances.put(Position.hashPair(b, a), l);
			}
		}

		// Try all combinations; Generate a list of all possible combinations
		// with a `Permutator`.
		Permutator<Position> perm = new Permutator<>(mustPass, Position.class);
		Position[] shortestPath = null;
		int shortestDistance = Integer.MAX_VALUE;

		combinations: while (perm.hasNext()) {
			Position[] currentPath = perm.next();
			int currentDistance = 0;

			Position last = entrance;
			int permutationIndex = 0;
			for (Position current : currentPath) {
				List<Position> tmp = distances.get(
					Position.hashPair(last, current));
				currentDistance += tmp.size();
				last = tmp.get(tmp.size() - 1);

				// Skip whole blocks of combinations if they can't be shorter
				// than the shortest known path.
				// Note that this doesn't help us in the worst case scenario: If
				// the combinations are ordered in ascending path length, the
				// following condition will never be true.
				if (shortestDistance < currentDistance) {
					perm.skipRemainingAt(permutationIndex);
					continue combinations;
				}
				permutationIndex++;
			}

			if (currentDistance < shortestDistance) {
				shortestPath = currentPath;
				shortestDistance = currentDistance;
			}
		}

		// Build the path from the chosen permutation
		LinkedList<Position> result = new LinkedList<>();
		Position last = entrance;
		for (Position p : shortestPath) {
			result.addAll(distances.get(Position.hashPair(last, p)));
			last = result.removeLast();
		}
		result.addAll(distances.get(Position.hashPair(last, counter)));

		return result;
	}

	/**
	 * Generate a string representation of the floor.
	 */
	public String debugPrint() {
		final char DEBUG_PRINT_ACCESSIBLE = '\u2591';
		final char DEBUG_PRINT_BLOCKED = '\u2588';
		final char DEBUG_PRINT_PATH_SEGMENT = '\u00B7';
		final char DEBUG_PRINT_ENTRANCE = '^';
		final char DEBUG_PRINT_COUNTER = '$';

		StringBuilder builder = new StringBuilder(
			floor.length * 8			// one char for each position (aka bit)
			+ floor.length / floorWidth	// plus one char for each linebreak
		);

		List<Position> path;
		try {
			path = getOptimalPath();
		} catch(IntegrityException e) {
			path = new ArrayList<>(0);
		}

		for (short y = 0; y < floor.length / floorWidth; y++) {
			for (short x = 0; x < floorWidth * 8; x++) {
				Position p = new Position(x, y);
				if (p.equals(entrance)) {
					builder.append(DEBUG_PRINT_ENTRANCE);
					continue;
				}
				if (p.equals(counter)) {
					builder.append(DEBUG_PRINT_COUNTER);
					continue;
				}
				String article = articles.get(p);
				if (article != null)
					builder.append(article.charAt(0));
				else if (path.contains(p))
					builder.append(DEBUG_PRINT_PATH_SEGMENT);
				else if (isBlocked(p))
					builder.append(DEBUG_PRINT_BLOCKED);
				else
					builder.append(DEBUG_PRINT_ACCESSIBLE);
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * Find the shortest path from `start` to `end`, using A* to avoid
	 * unpassable `Position`s.
	 * @return The list of steps you have to go to walk from start to end.
	 * @throws IntegrityException if no path between start and end exists.
	 */
	private List<Position> findShortestPath(Position start, Position end)
			throws IntegrityException
		{
		PriorityQueue<Position> open = new PriorityQueue<>(
			new Comparator<Position>() {
				public int compare(Position o1, Position o2) {
					return o1.priority - o2.priority;
				}
			}
		);
		Set<Position> closed = new HashSet<>();

		start.distanceToStart = 0;
		start.priority = 0;
		open.add(start);

		while(!open.isEmpty()) {
			Position current = open.poll();
			closed.add(current);

			if (current.equals(end)) {
				return current.toList();
			}

			for (Position neighbour : getAdjacent(current)) {
				if (isBlocked(neighbour) || closed.contains(neighbour)) {
					continue;
				}
				neighbour.distanceToStart = (current.distanceToStart + 1);
				neighbour.priority = (neighbour.distanceToStart + Position.airlineDistanceBetween(neighbour, end));
				neighbour.parent = current;
				open.add(neighbour);
			}
		}
		throw new IntegrityException(
			start.toString() + " unreachable from " + end.toString());
	}

	/**
	 * Generate an array with 4 positions adjacent to the assigned one.
	 */
	private Position[] getAdjacent(Position p) {
		return new Position[] {
			new Position((short) (p.x - 1), p.y),
			new Position((short) (p.x + 1), p.y),
			new Position(p.x, (short) (p.y - 1)),
			new Position(p.x, (short) (p.y + 1))
		};
	}
}
