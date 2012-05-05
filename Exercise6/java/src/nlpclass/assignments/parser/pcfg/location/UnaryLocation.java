package nlpclass.assignments.parser.pcfg.location;

public class UnaryLocation implements Location {

	private int col;
	private int row;

	public UnaryLocation(int row, int col) {
		this.row = row;
		this.col = col;
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}
}
