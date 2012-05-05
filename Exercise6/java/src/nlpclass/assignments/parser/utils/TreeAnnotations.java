package nlpclass.assignments.parser.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nlpclass.ling.Tree;
import nlpclass.ling.Trees;
import nlpclass.util.Filter;

/**
 * Class which contains code for annotating and binarizing trees for
 * the parser's use, and debinarizing and unannotating them for
 * scoring.
 */
public class TreeAnnotations {

  private static final String PARENT_DELIMITER = "^";
	private static final Set<String> SUBORDINATING_CONJUNCTIONS = new HashSet<String>(Arrays.asList(
			"after","although","as","as if","as long as","as much as","as soon as","as though","because","before","even",
			"even if","even though","if","if only","if when","if then","in as much","in order that","just as","lest","now",
			"now since","now that","now when","once","provided","provided that","rather than","since","so that","supposing",
			"than","that","though","til","unless","until","when","whenever","where","whereas","where if","wherever","whether",
			"which","while","who","whoever","why"
			));
	private static final Set<String> PREPOSITIONS = new HashSet<String>(Arrays.asList(
			"aboard","about","above","across","after","against","along","amid","among","anti","around","as","at","before",
			"behind","below","beneath","beside","besides","between","beyond","but","by","concerning","considering","despite",
			"down","during","except","excepting","excluding","following","for","from","in","inside","into","like","minus",
			"near","of","off","on","onto","opposite","outside","over","past","per","plus","regarding","round","save","since",
			"than","through","to","toward","towards","under","underneath","unlike","until","up","upon","versus","via","with",
			"within","without"
			));

	public static Tree<String> annotateTree(Tree<String> unAnnotatedTree) {

    // Currently, the only annotation done is a lossless binarization

    // TODO: change the annotation from a lossless binarization to a
    // finite-order markov process (try at least 1st and 2nd order)
    // mark nodes with the label of their parent nodes, giving a second
    // order vertical markov process

    Tree<String> binarizedTree = binarizeTree(unAnnotatedTree);
    categorizeInTree(binarizedTree);
    Tree<String> binarizedAndAnnotatedTree = annotateParents(binarizedTree, 2);
    categorizeVerbPhrases(binarizedAndAnnotatedTree);
//    categorizeNounPhrases(binarizedAndAnnotatedTree);
    
    return binarizedAndAnnotatedTree;
  }

  private static void categorizeVerbPhrases(Tree<String> binarizedTree) {
  	for (Tree<String> tree : binarizedTree.getPreOrderTraversal()) {
  		if (tree.getLabel().startsWith("VP")) {
  			if (tree.getChildren().size() > 1 && "TO".equals(tree.getChildren().get(0).getLabel())) {
  				tree.setLabel(tree.getLabel() + "-VBI");
  			} else {
  				tree.setLabel(tree.getLabel() + "-VBF");
  			}
  		}
  	}
	}

  private static void categorizeNounPhrases(Tree<String> binarizedTree) {
  	for (Tree<String> tree : binarizedTree.getPreOrderTraversal()) {
  		if (tree.getLabel().startsWith("NP")) {
  			if (tree.getPreTerminalYield().contains("PRP$")) {
  				tree.setLabel(tree.getLabel() + "-NP$");
  			} else {
  				tree.setLabel(tree.getLabel() + "-NPN$");
  			}
  		}
  	}
  }

	private static void categorizeInTree(Tree<String> binarizedTree) {
  	for (Tree<String> tree : binarizedTree.getPreOrderTraversal()) {
  		if ("IN".equals(tree.getLabel()) && tree.getChildren().size() == 1 && tree.getChildren().get(0).isLeaf()) {
  			tree.setLabel("IN^" + getInLabelForWord(tree));
  		}
  	}
	}

	private static String getInLabelForWord(Tree<String> tree) {
		String word = tree.getChildren().get(0).getLabel().toLowerCase();
		if (SUBORDINATING_CONJUNCTIONS.contains(word)) {
			return "SBN";
		} else if (PREPOSITIONS.contains(word)) {
			return "PRP";
		} else {
			return "SNT";
		}
	}

	private static Tree<String> annotateParents(Tree<String> binarizedTree, int order) {

		String parentLabel = binarizedTree.getLabel();
		String parentTag = getTagOfOrder(parentLabel, order);
		
		for (Tree<String> tree : binarizedTree.getChildren()) {
			if (tree.isPhrasal()) {
				annotateParents(tree, order);
				tree.setLabel(tree.getLabel() + PARENT_DELIMITER + parentTag);
			}
		}
  	
		return binarizedTree;
	}

	private static String getTagOfOrder(String parentLabel, int order) {
		String[] split = parentLabel.split(PARENT_DELIMITER);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < split.length && i < order; i++) {
			sb.append(split[i]);
		}
		
		return sb.toString();
	}

	public static Tree<String> binarizeTree(Tree<String> tree) {
    String label = tree.getLabel();
    if (tree.isLeaf())
      return new Tree<String>(label);
    if (tree.getChildren().size() == 1) {
      return new Tree<String>
        (label, 
         Collections.singletonList(binarizeTree(tree.getChildren().get(0))));
    }
    // otherwise, it's a binary-or-more local tree, 
    // so decompose it into a sequence of binary and unary trees.
    String intermediateLabel = "@"+label+"->";
    Tree<String> intermediateTree =
      binarizeTreeHelper(tree, 0, intermediateLabel);
    return new Tree<String>(label, intermediateTree.getChildren());
  }

  private static Tree<String> binarizeTreeHelper(Tree<String> tree,
                                                 int numChildrenGenerated, 
                                                 String intermediateLabel) {
    Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
    List<Tree<String>> children = new ArrayList<Tree<String>>();
    children.add(binarizeTree(leftTree));
    if (numChildrenGenerated < tree.getChildren().size() - 1) {
      Tree<String> rightTree = 
        binarizeTreeHelper(tree, numChildrenGenerated + 1, 
                           intermediateLabel + "_" + leftTree.getLabel());
      children.add(rightTree);
    }
    return new Tree<String>(intermediateLabel, children);
  } 

  public static Tree<String> unAnnotateTree(Tree<String> annotatedTree) {

    // Remove intermediate nodes (labels beginning with "@"
    // Remove all material on node labels which follow their base symbol 
    // (cuts at the leftmost -, ^, or : character)
    // Examples: a node with label @NP->DT_JJ will be spliced out, 
   // and a node with label NP^S will be reduced to NP

    Tree<String> debinarizedTree =
      Trees.spliceNodes(annotatedTree, new Filter<String>() {
        public boolean accept(String s) {
          return s.startsWith("@");
        }
      });
    Tree<String> unAnnotatedTree = 
      (new Trees.FunctionNodeStripper()).transformTree(debinarizedTree);
    return unAnnotatedTree;
  }
}