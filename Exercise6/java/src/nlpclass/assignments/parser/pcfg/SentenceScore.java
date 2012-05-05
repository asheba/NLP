package nlpclass.assignments.parser.pcfg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlpclass.assignments.parser.exceptions.IllegalParseException;
import nlpclass.assignments.parser.pcfg.location.BinaryLocation;
import nlpclass.assignments.parser.pcfg.location.Location;
import nlpclass.assignments.parser.pcfg.location.UnaryLocation;
import nlpclass.assignments.parser.utils.Grammar;
import nlpclass.assignments.parser.utils.Lexicon;
import nlpclass.assignments.parser.utils.grammar.BinaryRule;
import nlpclass.assignments.parser.utils.grammar.Rule;
import nlpclass.assignments.parser.utils.grammar.UnaryRule;
import nlpclass.util.Pair;

public class SentenceScore {
	
	/* --- Data Members  --- */
	private int sentenceWordCount; 
	private Map<String, Double>[][] scores;
	private Map<String, Pair<Rule, Location>>[][] backtrace;
	private List<String> sentence;
	private final Lexicon lexicon;
	private final Grammar grammar;
	
	/* --- Constructors --- */
	
	public SentenceScore(Lexicon lexicon, Grammar grammar, int wordCount) {
		this.lexicon = lexicon;
		this.grammar = grammar;
		
		sentenceWordCount = wordCount;
		scores = initScores(wordCount);
		backtrace = initBacktrace(wordCount);
	}
	
	public SentenceScore(Lexicon lexicon, Grammar grammar, List<String> sentence) {
		this(lexicon, grammar, sentence.size());
		this.sentence = sentence;
	}
	
	
	/* --- Getters --- */
	
	public Map<String, Pair<Rule, Location>>[][] getBacktrace() {
		return backtrace;
	}
	
	public int getWordCount() {
		return sentenceWordCount;
	}
	
	/* --- Public Methods --- */

	@Override
	public String toString() {
		return Arrays.deepToString(scores);
	}
	
	public void updateTerminalScores(List<String> sentence) {
		validateCorrectSentence(sentence);
		
		for (int i = 0; i < sentenceWordCount; i++) {
			for (String terminal : lexicon.getAllTags()) {
				scores[i][i+1].put(terminal, lexicon.scoreTagging(sentence.get(i), terminal));
			}
		}
	}

	/**
	 * @param sentence
	 * @return <code>true</code> if something was updated (a score was updated) and <code>false</code> otherwise.
	 */
	public void updateNonTerminalUnaryScores(List<String> sentence) {
		validateCorrectSentence(sentence);

		for (int i = 0; i <= sentenceWordCount; i++) {
			for (int j = 0; j <= sentenceWordCount; j++) {
				updateUnariesForCell(i, j);
			}
		}
	}

	
	public void updateBinaryScores(List<String> sentence) {
		validateCorrectSentence(sentence);

		for (int span = 2; span <= sentenceWordCount; span++) {
			for (int begin = 0; begin <= sentenceWordCount - span; begin++) {
				int end = begin + span;
				
				for (int split = begin+1; split <= end-1; split++) {
					Map<String, Double> leftBranch = scores[begin][split];
					Map<String, Double> rightBranch = scores[split][end];
					Map<String, Double> self = scores[begin][end];
					
					Set<BinaryRule> possibleRules = getLeftBranchRules(leftBranch);
					Set<BinaryRule> rightBranchRules = getRightBranchRules(rightBranch);
					possibleRules.retainAll(rightBranchRules);
					
					for (BinaryRule rule : possibleRules) {
						double prob = leftBranch.get(rule.getLeftChild()) * rightBranch.get(rule.getRightChild()) * rule.getScore();
						if (self.get(rule.getParent()) == null || prob > self.get(rule.getParent())) {
							self.put(rule.getParent(), prob);
							getBacktrace()[begin][end].put(rule.getParent(), 
																				new Pair<Rule, Location>(rule, 
																																 new BinaryLocation(new UnaryLocation(begin, split), 
																																		 								new UnaryLocation(split, end))));
						}
					}
					
					updateUnariesForCell(begin, end);
				}
			}
		}
	}
	

	/* --- Private Initialization --- */
	
	/**
	 * Creates score array.
	 * 
	 * @param wordCount
	 * @return
	 */
	private static Map<String,Double>[][] initScores(int wordCount) {
		@SuppressWarnings("unchecked")
		Map<String, Double>[][] temp = new HashMap[wordCount + 1][wordCount + 1];
		for (int i = 0; i < temp.length; i++) {
			for (int j = 0; j < temp[i].length; j++) {
				temp[i][j] = new HashMap<String, Double>();
			}
		}
		
		return temp;
	}
	
	/**
	 * Creates backtrace array.
	 * 
	 * @param wordCount
	 * @return
	 */
	private static Map<String, Pair<Rule, Location>>[][] initBacktrace(int wordCount) {
		@SuppressWarnings("unchecked")
		Map<String, Pair<Rule, Location>>[][] temp = new HashMap[wordCount + 1][wordCount + 1];
		for (int i = 0; i < temp.length; i++) {
			for (int j = 0; j < temp[i].length; j++) {
				temp[i][j] = new HashMap<String, Pair<Rule, Location>>();
			}
		}
		
		return temp;
	}
	

	/* --- Private Methods --- */
	
	private void validateCorrectSentence(List<String> sentence) {
		if (sentenceWordCount != sentence.size() || (this.sentence != null && !sentence.equals(this.sentence))) {
			throw new IllegalParseException("Initialized score for wrong sentence!");
		}
	}
	
	/**
	 * Retrieves a set of all binary rules with their left branch being one of the scored results kept in the given cell.
	 */
	private Set<BinaryRule> getLeftBranchRules(Map<String, Double> leftBranch) {
		Set<BinaryRule> rules = new HashSet<BinaryRule>();
		for (String term : leftBranch.keySet()) {
			rules.addAll(grammar.getBinaryRulesByLeftChild(term));
		}
		return rules;
	}

	/**
	 * Retrieves a set of all binary rules with their left branch being one of the scored results kept in the given cell.
	 */
	private Set<BinaryRule> getRightBranchRules(Map<String, Double> rightBranch) {
		Set<BinaryRule> rules = new HashSet<BinaryRule>();
		for (String term : rightBranch.keySet()) {
			rules.addAll(grammar.getBinaryRulesByRightChild(term));
		}
		return rules;
	}
	
	private void updateUnariesForCell(int i, int j) {
		Map<String, Double> currentCell = scores[i][j];
		
		boolean added = true;
		while (added) {
			added = false;
			
			Set<String> foundCategories = new HashSet<String>(currentCell.keySet());
			for (String relevantTerminal : foundCategories) {
				for (UnaryRule unaryRule : grammar.getUnaryRulesByChild(relevantTerminal)) {
					
					double score = unaryRule.getScore() * currentCell.get(relevantTerminal);
					if (currentCell.get(unaryRule.getParent()) == null || currentCell.get(unaryRule.getParent()) < score) {
						currentCell.put(unaryRule.getParent(), score);
						getBacktrace()[i][j].put(unaryRule.getParent(), new Pair<Rule, Location>(unaryRule, new UnaryLocation(i, j)));
						added = true;
					}
				}
			}	
		}
	}

	
	
	
}