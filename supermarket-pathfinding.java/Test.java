import java.util.*;

class Test {

	/**
	 * Create a store and print it. The output should look like this:
	 *
	 *  ^█░··P░░
	 *  ·█░·█·░░
	 *  C█░·█·S·
	 *  ··B·█░░$
	 *
	 *
	 * Then change the floor layout and print it again to look like this:
	 *
	 *  ^█░··P░░
	 *  ····█·░░
	 *  C████·S·
	 *  ··B░█░░$
	 */
	public static void main(String[] args) throws Exception {
		Map<Store.Position, String> articles = new HashMap<>();
		articles.put(new Store.Position(0, 2), "Cheese");
		articles.put(new Store.Position(2, 3), "Butter");
		articles.put(new Store.Position(5, 0), "Ponies");
		articles.put(new Store.Position(6, 2), "Salad");

		Store s = new Store(new byte[] {
				(byte) 64,
				(byte) 72,
				(byte) 72,
				(byte) 8
			},
			1,
			new Store.Position(0, 0),
			new Store.Position(7, 3),
			articles
		);

		System.out.print(s.debugPrint());
		System.out.println("Length of path: " + s.getOptimalPath().size());

		System.out.println();
		s.block(new Store.Position(2, 2));
		s.block(new Store.Position(3, 2));
		s.free(new Store.Position(1, 1));
		System.out.print(s.debugPrint());
		System.out.println("Length of path: " + s.getOptimalPath().size());
	}
}
