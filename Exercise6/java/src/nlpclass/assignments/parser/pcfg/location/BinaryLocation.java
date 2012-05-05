package nlpclass.assignments.parser.pcfg.location;

public class BinaryLocation implements Location {

	private Location right;
	private Location left;

	public BinaryLocation(Location left, Location right) {
		this.left = left;
		this.right = right;
	}

	public Location getLeft() {
		return left;
	}

	public Location getRight() {
		return right;
	}
}
