package nlpclass.assignments.parser;

import java.util.List;

import nlpclass.ling.Tree;

/**
 * Parsers are required to map sentences to trees.  How a parser is
 * constructed and trained is not specified.
 */
public interface Parser {
  public void train(List<Tree<String>> trainTrees);
  public Tree<String> getBestParse(List<String> sentence);
}
