# supermarket-pathfinding
A* pathfinding on a tile-based map.

My contribution to a 2nd-semester university group homework. My task was to find
the shortest way through a supermarket that passes all articles on our shopping
list.

Pathfinding between two points is done with
[A*](https://en.wikipedia.org/wiki/A*_search_algorithm), then I brute-force
every combination to find the shortest overall path.

Run with `javac Test.java && java Test`.
