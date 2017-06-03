import java.util.*;
import java.lang.reflect.*;

/**
 * Iterator that yields permutations of the assigned array, including the
 * unmodified group itself.
 *
 * The class stores the current progress in the integer array `iteration` and
 * uses these values as indices into `group`.
 * `iteration` is initialized to values in increasing order, starting from 0,
 * thus representing the unmodified group. Each call to `next` will then find
 * the rightmost value that can be increased (because it has a higher value on
 * its right) and increase it to the next-higher value on its right. All other
 * values on the right are ordered in ascending order.
 *
 * -- Example --
 *   0 1 2 3
 *   0 1 3 2
 *   0 2 1 3
 *   0 2 3 1
 *   0 3 1 2
 *   0 3 2 1
 *   1 0 2 3
 *    ...
 */
public class Permutator<E> implements Iterator<E[]> {

	/**
	 * The array of which permutations are created.
	 */
	private E[] group;

	/**
	 * Class reflection of E.
	 * @see http://stackoverflow.com/questions/529085/how-to-create-a-generic
	 *      -array-in-java
	 */
	private Class<E> cls;

	/**
	 * Stores the last applied permutation between calls to `next`.
	 */
	private int[] iteration;

	/**
	 * @param group The array of which permutations are created
	 * @param cls The runtime class of `group`, because this information gets
	 *        lost due to type erasure.
	 */
	public Permutator(E[] group, Class<E> cls) {
		this.group = group;
		this.cls = cls;
	}

	/**
	 * Check whether `iteration` reached the final ordering.
	 */
	public boolean hasNext() {
		if (iteration == null)
			return true;
		for (int i = 0; i < group.length; i++) {
			if (iteration[i] < group.length - 1 - i)
				return true;
		}
		return false;
	}

	/**
	 * @return The next permutation of `group`.
	 */
	public E[] next() {
		if (iteration == null) {
			// The first call to `next()` returns the unmodified group. During
			// that, the iteration gets initialized for consecutive calls.
			iteration = new int[group.length];
			for (int i = 0; i < group.length; i++) {
				iteration[i] = i;
			}
		}
		else {
			nextInternal();
		}

		@SuppressWarnings("unchecked")
		E[] result = (E[]) Array.newInstance(cls, group.length);
		for (int i = 0; i < group.length; i++) {
			result[i] = group[iteration[i]];
		}
		return result;
	}

	/**
	 * Order all elements behind `index` in descending order.
	 *
	 * This way we skip all remaining combinations that have the assigned index
	 * at the current value, because the next time we call `next()`, `index`
	 * will be increased.
	 *
	 */
	public void skipRemainingAt(int index) {
		if (index > group.length - 3)
			// `group.length - 3` because
			//   index >= group.length: out of bounds
			//   index == group.length - 1: reorder everything behind the last
			//                              element
			//   index == group.length - 2: reorder the last 1 element
			return;

		Arrays.sort(iteration, index + 1, group.length);
		// `Arrays.sort()` orders in ascending order, so we have to reverse the
		// ordered part.
		int interchangeOffset = (group.length - index - 1) / 2;
		for (int i = 0; i < interchangeOffset; i++) {
			iteration[index + 1 + i] ^= iteration[group.length - 1 - i];
			iteration[group.length - 1 - i] ^= iteration[index + 1 + i];
			iteration[index + 1 + i] ^= iteration[group.length - 1 - i];
		}
	}

	/**
	 * Perform one iteration, as described in the class doc.
	 */
	private void nextInternal() {
		int leftmost = group.length - 1;
		while (iteration[leftmost] < iteration[--leftmost]);

		int indexOfNewValueForLeftmost = leftmost + 1;
		for (int i = leftmost + 1; i < group.length; i++) {
			if (iteration[leftmost] < iteration[i])
				indexOfNewValueForLeftmost = i;
		}

		// Swap iteration[leftmost] and iteration[indexOfNewValueForLeftmost]
		iteration[leftmost] ^= iteration[indexOfNewValueForLeftmost];
		iteration[indexOfNewValueForLeftmost] ^= iteration[leftmost];
		iteration[leftmost] ^= iteration[indexOfNewValueForLeftmost];

		Arrays.sort(iteration, leftmost + 1, group.length);
	}
}
