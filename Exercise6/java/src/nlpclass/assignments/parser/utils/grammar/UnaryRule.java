package nlpclass.assignments.parser.utils.grammar;


/** A unary grammar rule with score representing its probability. */
public class UnaryRule extends Rule {

  String child;
  double score;

  public String getChild() {
    return child;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UnaryRule)) return false;

    final UnaryRule unaryRule = (UnaryRule) o;

    if (child != null ? !child.equals(unaryRule.child) : unaryRule.child != null) return false;
    if (parent != null ? !parent.equals(unaryRule.parent) : unaryRule.parent != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (parent != null ? parent.hashCode() : 0);
    result = 29 * result + (child != null ? child.hashCode() : 0);
    return result;
  }

  public String toString() {
    return parent + " -> " + child + " %% "+score;
  }

  public UnaryRule(String parent, String child) {
  	super(parent);
    this.child = child;
  }
}
