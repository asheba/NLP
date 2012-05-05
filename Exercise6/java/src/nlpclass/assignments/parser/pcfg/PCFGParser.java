package nlpclass.assignments.parser.pcfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nlpclass.assignments.parser.Parser;
import nlpclass.assignments.parser.utils.Grammar;
import nlpclass.assignments.parser.utils.Lexicon;
import nlpclass.assignments.parser.utils.TreeAnnotations;
import nlpclass.ling.Tree;

/**
 * The PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
	
	/* --- Data Members --- */
	
  private Grammar grammar;
  private Lexicon lexicon;
  
  public void train(List<Tree<String>> trainTrees) {
    List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
  	
  	for (Tree<String> tree : trainTrees) {
  		Tree<String> binarizedTree = TreeAnnotations.annotateTree(tree);
//  		Tree<String> binarizedTree = TreeAnnotations.binarizeTree(tree);
  		binarizedTrees.add(binarizedTree);
   	}
    
    lexicon = new Lexicon(binarizedTrees);
    grammar = new Grammar(binarizedTrees);

  }

  
  public Tree<String> getBestParse(List<String> sentence) {
  	SentenceScore score = new SentenceScore(lexicon, grammar, sentence.size());
  	
  	score.updateTerminalScores(sentence);
  	score.updateNonTerminalUnaryScores(sentence);

		score.updateBinaryScores(sentence);
		score.updateNonTerminalUnaryScores(sentence);
  	
		TreeBuilder treeBuilder = new TreeBuilder(score);
		Tree<String> tree = TreeAnnotations.unAnnotateTree(treeBuilder.build());
		appendDummyNodesToLeaves(tree);
		tree.setWords(sentence);
		return tree;
  }


	private void appendDummyNodesToLeaves(Tree<String> tree) {
		if (tree.isLeaf()) {
			if (tree.getLabel() == "DUMMY") {
				return;
			} else {
				tree.setChildren(Collections.singletonList(new Tree<String>("DUMMY")));
			}
		} else {
			for (Tree<String> child : tree.getChildren()) {
				appendDummyNodesToLeaves(child);
			}
		}
	}

}