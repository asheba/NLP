package nlpclass.assignments.parser.utils.grammar;


/** A binary grammar rule with score representing its probability. */
public class BinaryRule extends Rule {

  String leftChild;
  String rightChild;
  public String getLeftChild() {
    return leftChild;
  }

  public String getRightChild() {
    return rightChild;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BinaryRule)) return false;

    final BinaryRule binaryRule = (BinaryRule) o;

    if (leftChild != null ? !leftChild.equals(binaryRule.leftChild) : binaryRule.leftChild != null) 
      return false;
    if (parent != null ? !parent.equals(binaryRule.parent) : binaryRule.parent != null) 
      return false;
    if (rightChild != null ? !rightChild.equals(binaryRule.rightChild) : binaryRule.rightChild != null) 
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (parent != null ? parent.hashCode() : 0);
    result = 29 * result + (leftChild != null ? leftChild.hashCode() : 0);
    result = 29 * result + (rightChild != null ? rightChild.hashCode() : 0);
    return result;
  }

  public String toString() {
    return parent + " -> " + leftChild + " " + rightChild + " %% "+score;
  }

  public BinaryRule(String parent, String leftChild, String rightChild) {
  	super(parent);
    this.leftChild = leftChild;
    this.rightChild = rightChild;
  }
}

