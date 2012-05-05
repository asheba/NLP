package nlpclass.assignments.parser.pcfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import nlpclass.assignments.parser.pcfg.location.BinaryLocation;
import nlpclass.assignments.parser.pcfg.location.Location;
import nlpclass.assignments.parser.pcfg.location.UnaryLocation;
import nlpclass.assignments.parser.utils.grammar.BinaryRule;
import nlpclass.assignments.parser.utils.grammar.Rule;
import nlpclass.assignments.parser.utils.grammar.UnaryRule;
import nlpclass.ling.Tree;
import nlpclass.util.Pair;

public class TreeBuilder {
	
	private Map<String, Pair<Rule, Location>>[][] backtrace;
	private int sentenceLength;

	public TreeBuilder(SentenceScore score) {
		backtrace = score.getBacktrace();
		sentenceLength = score.getWordCount();
	}

	@SuppressWarnings("unchecked")
	public Tree<String> build() {
		return buildTree("ROOT", new UnaryLocation(0, sentenceLength));
	}

	@SuppressWarnings("unchecked")
	private Tree<String> buildTree(String parent, UnaryLocation location) {

		Map<String, Pair<Rule, Location>> cell = backtrace[location.getRow()][location.getCol()];
		Tree<String> tree = new Tree<String>(parent);
		
		Pair<Rule, Location> pair = cell.get(parent);
		
		if (pair != null) {
			Rule rule = pair.getFirst();
			if (rule instanceof UnaryRule) {
				tree.setChildren(Collections.singletonList(buildTree(((UnaryRule) rule).getChild(), (UnaryLocation) pair.getSecond())));
			} else {
				tree.setChildren(Arrays.asList(
						buildTree(((BinaryRule) rule).getLeftChild(), (UnaryLocation) ((BinaryLocation) pair.getSecond()).getLeft()),
						buildTree(((BinaryRule) rule).getRightChild(), (UnaryLocation) ((BinaryLocation) pair.getSecond()).getRight())
				));
			}
		}
		
		return tree;
	}

}
