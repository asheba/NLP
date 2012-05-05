package nlpclass.assignments.parser.utils.grammar;

public abstract class Rule {

	protected String parent;
	protected double score;

	public Rule(String parent) {
		this.parent = parent;
	}

	public String getParent() {
	  return parent;
	}

	public double getScore() {
	  return score;
	}

	public void setScore(double score) {
	  this.score = score;
	}

}