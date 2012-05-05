package nlpclass.assignments.parser.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nlpclass.assignments.parser.utils.grammar.BinaryRule;
import nlpclass.assignments.parser.utils.grammar.Rule;
import nlpclass.assignments.parser.utils.grammar.UnaryRule;
import nlpclass.ling.Tree;
import nlpclass.util.CollectionUtils;
import nlpclass.util.Counter;

/**
 * Simple implementation of a PCFG grammar, offering the ability to look up
 * rules by their child symbols. Rule probability estimates are just relative
 * frequency estimates off of training trees.
 */
public class Grammar {

	Map<String, List<BinaryRule>> binaryRulesByLeftChild = new HashMap<String, List<BinaryRule>>();
	Map<String, List<BinaryRule>> binaryRulesByRightChild = new HashMap<String, List<BinaryRule>>();
	Map<String, List<UnaryRule>> unaryRulesByChild = new HashMap<String, List<UnaryRule>>();

	/*
	 * Rules in grammar are indexed by child for easy access when doing bottom up
	 * parsing.
	 */
	public List<BinaryRule> getBinaryRulesByLeftChild(String leftChild) {
		return CollectionUtils.getValueList(binaryRulesByLeftChild, leftChild);
	}

	public List<BinaryRule> getBinaryRulesByRightChild(String rightChild) {
		return CollectionUtils.getValueList(binaryRulesByRightChild, rightChild);
	}

	public List<UnaryRule> getUnaryRulesByChild(String child) {
		return CollectionUtils.getValueList(unaryRulesByChild, child);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		List<String> ruleStrings = new ArrayList<String>();
		for (String leftChild : binaryRulesByLeftChild.keySet()) {
			for (Rule binaryRule : getBinaryRulesByLeftChild(leftChild)) {
				ruleStrings.add(binaryRule.toString());
			}
		}
		for (String child : unaryRulesByChild.keySet()) {
			for (UnaryRule unaryRule : getUnaryRulesByChild(child)) {
				ruleStrings.add(unaryRule.toString());
			}
		}
		for (String ruleString : CollectionUtils.sort(ruleStrings)) {
			sb.append(ruleString);
			sb.append("\n");
		}
		return sb.toString();
	}

	private void addBinary(BinaryRule binaryRule) {
		CollectionUtils.addToValueList(binaryRulesByLeftChild,
				binaryRule.getLeftChild(), binaryRule);
		CollectionUtils.addToValueList(binaryRulesByRightChild,
				binaryRule.getRightChild(), binaryRule);
	}

	private void addUnary(UnaryRule unaryRule) {
		CollectionUtils.addToValueList(unaryRulesByChild, unaryRule.getChild(),
				unaryRule);
	}

	/*
	 * A builds PCFG using the observed counts of binary and unary productions in
	 * the training trees to estimate the probabilities for those rules.
	 */
	public Grammar(List<Tree<String>> trainTrees) {
		Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
		Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
		Counter<String> symbolCounter = new Counter<String>();
		for (Tree<String> trainTree : trainTrees) {
			tallyTree(trainTree, symbolCounter, unaryRuleCounter, binaryRuleCounter);
		}
		for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
			double unaryProbability = unaryRuleCounter.getCount(unaryRule)
					/ symbolCounter.getCount(unaryRule.getParent());
			unaryRule.setScore(unaryProbability);
			addUnary(unaryRule);
		}
		for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
			double binaryProbability = binaryRuleCounter.getCount(binaryRule)
					/ symbolCounter.getCount(binaryRule.getParent());
			binaryRule.setScore(binaryProbability);
			addBinary(binaryRule);
		}
	}

	private void tallyTree(Tree<String> tree, Counter<String> symbolCounter,
			Counter<UnaryRule> unaryRuleCounter, Counter<BinaryRule> binaryRuleCounter) {
		if (tree.isLeaf())
			return;
		if (tree.isPreTerminal())
			return;
		if (tree.getChildren().size() == 1) {
			UnaryRule unaryRule = makeUnaryRule(tree);
			symbolCounter.incrementCount(tree.getLabel(), 1.0);
			unaryRuleCounter.incrementCount(unaryRule, 1.0);
		}
		if (tree.getChildren().size() == 2) {
			BinaryRule binaryRule = makeBinaryRule(tree);
			symbolCounter.incrementCount(tree.getLabel(), 1.0);
			binaryRuleCounter.incrementCount(binaryRule, 1.0);
		}
		if (tree.getChildren().size() < 1 || tree.getChildren().size() > 2) {
			throw new RuntimeException(
					"Attempted to construct a Grammar with an illegal tree: " + tree);
		}
		for (Tree<String> child : tree.getChildren()) {
			tallyTree(child, symbolCounter, unaryRuleCounter, binaryRuleCounter);
		}
	}

	private UnaryRule makeUnaryRule(Tree<String> tree) {
		return new UnaryRule(tree.getLabel(), tree.getChildren().get(0).getLabel());
	}

	private BinaryRule makeBinaryRule(Tree<String> tree) {
		return new BinaryRule(tree.getLabel(),
				tree.getChildren().get(0).getLabel(), tree.getChildren().get(1)
						.getLabel());
	}
}