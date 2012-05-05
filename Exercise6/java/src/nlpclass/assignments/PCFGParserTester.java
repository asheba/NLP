package nlpclass.assignments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlpclass.assignments.parser.Parser;
import nlpclass.assignments.parser.exceptions.IllegalParseException;
import nlpclass.assignments.parser.utils.Grammar;
import nlpclass.assignments.parser.utils.Lexicon;
import nlpclass.assignments.parser.utils.TreeAnnotations;
import nlpclass.assignments.parser.utils.grammar.UnaryRule;
import nlpclass.io.MASCTreebankReader;
import nlpclass.io.PennTreebankReader;
import nlpclass.ling.Tree;
import nlpclass.ling.Trees;
import nlpclass.parser.EnglishPennTreebankParseEvaluator;
import nlpclass.util.CommandLineUtils;

/**
 * Harness for PCFG Parser project.
 *
 * @author Dan Klein
 */
public class PCFGParserTester {

  // Longest sentence length that will be tested on.
  private static int MAX_LENGTH = 20;
 
  private static double testParser(Parser parser, List<Tree<String>> testTrees) {
    EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String> eval = 
      new EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String>
      (Collections.singleton("ROOT"), 
       new HashSet<String>(Arrays.asList(new String[] {"''", "``", ".", ":", ","})));
    for (Tree<String> testTree : testTrees) {
      List<String> testSentence = testTree.getYield();

      if (testSentence.size() > MAX_LENGTH)
        continue;
      Tree<String> guessedTree = parser.getBestParse(testSentence);
      System.out.println("Guess:\n"+Trees.PennTreeRenderer.render(guessedTree));
      System.out.println("Gold:\n"+Trees.PennTreeRenderer.render(testTree));
      eval.evaluate(guessedTree, testTree);
    }
    System.out.println();
    return eval.display(true);
  }
  
  private static List<Tree<String>> readTrees(String basePath, int low,
			int high) {
		Collection<Tree<String>> trees = PennTreebankReader.readTrees(basePath,
				low, high);
		// normalize trees
		Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
		List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			Tree<String> normalizedTree = treeTransformer.transformTree(tree);
			// System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
			normalizedTreeList.add(normalizedTree);
		}
		return normalizedTreeList;
	}

	private static List<Tree<String>> readTrees(String basePath) {
		Collection<Tree<String>> trees = PennTreebankReader.readTrees(basePath);
		// normalize trees
		Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
	  List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
    for (Tree<String> tree : trees) {
      //      System.err.println(tree);
      Tree<String> normalizedTree = treeTransformer.transformTree(tree);
      // System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
      normalizedTreeList.add(normalizedTree);
    }
    return normalizedTreeList;
  }


  private static List<Tree<String>> readMASCTrees(String basePath, int low, int high) {
    System.out.println("MASC basepath: " + basePath);
    Collection<Tree<String>> trees = MASCTreebankReader.readTrees(basePath, low, high);
    // normalize trees
    Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
    List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
    for (Tree<String> tree : trees) {
      Tree<String> normalizedTree = treeTransformer.transformTree(tree);
      // System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
      normalizedTreeList.add(normalizedTree);
    }
    return normalizedTreeList;
  }


  private static List<Tree<String>> readMASCTrees(String basePath) {
    System.out.println("MASC basepath: " + basePath);
    Collection<Tree<String>> trees = MASCTreebankReader.readTrees(basePath);
    // normalize trees
    Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
    List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
    for (Tree<String> tree : trees) {
      Tree<String> normalizedTree = treeTransformer.transformTree(tree);
      // System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
      normalizedTreeList.add(normalizedTree);
    }
    return normalizedTreeList;
  }
  
  
  public static void main(String[] args) {

    // set up default options ..............................................
    Map<String, String> options = new HashMap<String, String>();
    options.put("--path",      "/home/sheba/workspace/NLP/Exercise6/data/parser/");
    options.put("--data",      "masc");
//    options.put("--parser",    "nlpclass.assignments.parser.baseline.BaselineParser");
    options.put("--parser",    "nlpclass.assignments.parser.pcfg.PCFGParser");

    options.put("--maxLength", "20");

    // let command-line options supersede defaults .........................
    options.putAll(CommandLineUtils.simpleCommandLineParser(args));
    System.out.println("PCFGParserTester options:");
    for (Map.Entry<String, String> entry: options.entrySet()) {
      System.out.printf("  %-12s: %s%n", entry.getKey(), entry.getValue());
    }
    System.out.println();

    MAX_LENGTH = Integer.parseInt(options.get("--maxLength"));

    Parser parser;
    try {
      Class parserClass = Class.forName(options.get("--parser"));
      parser = (Parser) parserClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("Using parser: " + parser);

    String basePath = options.get("--path");
    String preBasePath = basePath;
    String dataSet = options.get("--data");
    if (!basePath.endsWith("/"))
      basePath += "/";
    //basePath += dataSet;
    System.out.println("Data will be loaded from: " + basePath + "\n");

    List<Tree<String>> trainTrees = new ArrayList<Tree<String>>(),
    				   validationTrees = new ArrayList<Tree<String>>(),
    				   testTrees = new ArrayList<Tree<String>>();

    if (dataSet.equals("miniTest")) {
      // training data: first 3 of 4 datums
      basePath += dataSet;
      System.out.println("Loading training trees...");
      trainTrees = readTrees(basePath, 1, 3);
      System.out.println("done.");

      // test data: last of 4 datums
      System.out.println("Loading test trees...");
      testTrees = readTrees(basePath, 4, 4);
      System.out.println("done.");

    }
    if (dataSet.equals("masc")) {
    	basePath += "parser/";
      // training data: MASC train
      System.out.println("Loading MASC training trees... from: "+basePath+"masc/train");
      trainTrees.addAll(readMASCTrees(basePath+"masc/train", 0, 38));
      System.out.println("done.");
      System.out.println("Train trees size: "+trainTrees.size());

      System.out.println("First train tree: "+Trees.PennTreeRenderer.render(trainTrees.get(0)));
      System.out.println("Last train tree: "+Trees.PennTreeRenderer.render(trainTrees.get(trainTrees.size()-1)));
      
      // test data: MASC devtest
      System.out.println("Loading MASC test trees...");
      testTrees.addAll(readMASCTrees(basePath+"masc/devtest", 0, 11));
      //testTrees.addAll(readMASCTrees(basePath+"masc/blindtest", 0, 8));
      System.out.println("Test trees size: "+testTrees.size());
      System.out.println("done.");
      
      System.out.println("First test tree: "+Trees.PennTreeRenderer.render(testTrees.get(0)));
      System.out.println("Last test tree: "+Trees.PennTreeRenderer.render(testTrees.get(testTrees.size()-1)));
    }
    if (!dataSet.equals("miniTest") && !dataSet.equals("masc")){
      throw new RuntimeException("Bad data set: " + dataSet + ": use miniTest or masc."); 
    }

    System.out.println("\nTraining parser...");
    parser.train(trainTrees);

    System.out.println("\nTesting parser...");
    testParser(parser, testTrees);
  }
}
