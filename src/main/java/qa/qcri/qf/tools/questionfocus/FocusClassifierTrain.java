package qa.qcri.qf.tools.questionfocus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.cli.CommandLine;
import qa.qcri.qf.cli.CommandLineParser;
import qa.qcri.qf.fileutil.WriteFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import util.Pair;

import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class FocusClassifierTrain {
	
	private static final String HELP_OPT = "help";
	private static final String LANG_OPT = "lang";
	private static	 final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String TRAINING_QUESTIONS_FOCUS_PATH_OPT = "trainQuestionsFocusPath";
	private static final String TRAINING_CASES_DIR_OPT =  "trainCasesDir";
	private static final String TRAINING_OUTPUT_DIR_OPT = "trainOutputDir";
	
	private static final String QID_QUESTION_SEPARATOR = "\t";
		
	public static final String DATA = Commons.QUESTION_FOCUS_DATA + "qf-it_train10.txt";
	
	public static final String CASES_DIRECTORY = "CASes/question-focus/train.it";
	
	private static Logger logger = Logger.getLogger(FocusClassifierTrain.class);
		
	private final Set<String> allowedTags;
	
	private static String[] emptyTagset = { };
	
	private static String[] englishTagset = { 
		//"NNPS", "VBN", "JJ", "JJS", // These postags are seen just once as focus
		"NNS", "NNP",  "NN" // These are high frequency postags
	};
	
	private static String[] italianTagset = { 
		"SN", "SP", "SPN", "SS" 
	};
	
	private Analyzer analyzer;
	
	private TreeSerializer treeSerializer =
			new TreeSerializer().enableAdditionalLabels();
	
	private Map<Integer, List<String>> examples;
	
	private Map<String, Integer> postoFreq = new HashMap<>();
	
	public FocusClassifierTrain(Analyzer analyzer, Set<String> allowedTags) throws UIMAException {
		if (analyzer == null)
			throw new NullPointerException("analyzer is null");
		if (allowedTags == null)
			throw new NullPointerException("allowedTags is null");
		 
		this.analyzer = analyzer;
		this.allowedTags = allowedTags;
		this.examples = new HashMap<>();		
		
	}
	
	public FocusClassifierTrain generateExamples(String trainFile) throws UIMAException {
		
		Iterator<QuestionWithFocus> questions =
				new QuestionWithIdReader(trainFile, QID_QUESTION_SEPARATOR)
					.iterator();
		
		JCas cas = JCasFactory.createJCas();
		
		while (questions.hasNext()) {
			QuestionWithFocus questionWithFocus = questions.next();
			
			String qid = questionWithFocus.getId();
			List<Focus> foci = questionWithFocus.getFoci();
			if (questionWithFocus.getFoci().size() > 1) { 
				logger.debug("question(" + qid + ") contains " + foci.size() + " foci");
				continue;
			}
			assert questionWithFocus.hasFocus();
						
			Focus focus = foci.get(0);			
					
			this.analyzer.analyze(cas, questionWithFocus);
			
			TokenTree tree = RichTree.getConstituencyTree(cas);
			
			logger.debug("tree: " + treeSerializer.serializeTree(tree, Commons.getParameterList()));
			
			//System.out.println("focus: " + questionWithAnnotatedFocus.getFocus());
			
			addFocusMetadata(tree, focus.getBegin(), focus.getEnd());
			//this.addFocusMetadata(tree, focusSpan.ge, focusSpan.getB());
			
			this.examples.put(Integer.parseInt(questionWithFocus.getId()), produceExamples(Integer.parseInt(questionWithFocus.getId()), tree));
		}
		return this;
	}
	
	/**
	 * Generates a list of examples from a tree with tagged focus.
	 * @param tree Tree tree from which examples are generated
	 * @param ts The serializer used to output the trees
	 * @return A list of pair of examples, and the rich token tagged in that example
	 */
	public List<Pair<String, RichTokenNode>> generateExamples(TokenTree tree) {
		List<Pair<String, RichTokenNode>> examples = new ArrayList<>();
		
		for (RichTokenNode node : tree.getTokens()) {
			
			RichNode posTag = node.getParent();
			
			if (!allowedTags.contains(posTag.getValue())) continue;
			
			posTag.addAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
			
			boolean isFocus = node.getMetadata().containsKey(Commons.QUESTION_FOCUS_KEY);
			
			String label = isFocus ? "+1" : "-1";
			
			String taggedTree = treeSerializer.serializeTree(tree, Commons.getParameterList());
			
			String example = "";
			example += label;
			example += "|BT| ";
			example += taggedTree;
			example += " |ET|";		
			
			System.out.println("adding example: " + example.toString());
			examples.add(new Pair<>(example, node));
			
			posTag.removeAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
		}
		
		return examples;
				
	}
	
	/**
	 * Generates examples for a given question
	 * @param qid the id of the question
	 * @param tree the tree representation of the question
	 * @return a list of examples
	 */
	private List<String> produceExamples(int qid, TokenTree tree) {
		List<String> examples = new ArrayList<>();
		
		for (RichTokenNode node : tree.getTokens())  {
			RichNode posTag = node.getParent();
			
			if (!allowedTags.contains(posTag.getValue())) continue;
			
			posTag.addAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
			
			boolean isFocus = node.getMetadata().containsKey(Commons.QUESTION_FOCUS_KEY);
			
			String label = isFocus ? "+1" : "-1";
			
			String taggedTree = treeSerializer.serializeTree(tree, Commons.getParameterList());
			
			String example = "";
			example += label;
			example += "|BT| ";
			example += taggedTree;
			example += " |ET|";
			
			examples.add(example);
			
			posTag.removeAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
			
			if (isFocus) { 
				System.out.println(taggedTree);
				String pos = node.getParent().getValue();
				
				Integer freq = this.postoFreq.get(pos);
				
				if (freq == null) { 
					this.postoFreq.put(pos, 1);
				} else {
					this.postoFreq.put(pos, freq + 1);
				}						
			}
		}
		
		return examples;
	}

	/**
	 * Add a FOCUS metadata to the leaf node of the tree delimited by the given indexes
	 * @param tree The tree to augment with metadata
	 * @param beginPos The starting index of the token
	 * @param endPos The ending index of the token
	 */
	private void addFocusMetadata(TokenTree tree, int beginPos, int endPos) {
		///System.out.printf("focus.beginPos: %d, focus.endPos: %d\n", beginPos, endPos);
		for (RichTokenNode node : tree.getTokens()) { 
			Token token = node.getToken();
			//System.out.printf("token.beginPos: %d, token.endPos: %d\n", node.getToken().getBegin(), node.getToken().getEnd());
			if (token.getBegin() == beginPos && token.getEnd() == endPos) {
				node.getMetadata().put(Commons.QUESTION_FOCUS_KEY, Commons.QUESTION_FOCUS_KEY);
			}				
		}
	}
	
	/**
	 * Print useful statistics on the generated data
	 * @return this class instance for chaining
	 */
	public FocusClassifierTrain printStatistics() { 
		System.out.println("F-POS\tFREQUENCY");
		for (String pos : this.postoFreq.keySet()) { 
			System.out.println(pos + "\t" + this.postoFreq.get(pos));
		}
		
		int totalNumberOfExamples = 0;
		for (List<String> exampleList : this.examples.values()) {
			totalNumberOfExamples += exampleList.size();
		}
		
		System.out.println("\nTotal number of examples: " + totalNumberOfExamples);
		
		return this;
	}
	
	/**
	 * Writes the examples on disk
	 * @param outputPath A string holding the output file
	 * @param examples The data structure containing the examples mapped to questions
	 * @return This class instance for chaining
	 */
	public FocusClassifierTrain writeExamplesToDisk(String outputPath, 
			Map<Integer, List<String>> examples) {
		
		WriteFile out = new WriteFile(outputPath);
		
		for (Integer qid : examples.keySet()) { 
			List<String> examplesList = examples.get(qid);
			
			for (String example : examplesList) {
				out.writeLn(example);
			}
		}
		
		out.close();
		
		return this;
	}
	
	public FocusClassifierTrain writeExamplesToDisk(String outputPath) {
		return this.writeExamplesToDisk(outputPath, this.examples);
	}
	
	/**
	 * Writes all the examples on disk, but producing several folds
	 * @param outputDir The directory which will contain the output files
	 * @param folds The number of folds to create
	 * @return This class instance for chaining
	 */
	public FocusClassifierTrain writeExamplesInFolds(String outputDir, int foldsNum) { 
		List<Integer> questionIds = Lists.newArrayList(this.examples.keySet());
		List<List<Integer>> foldsOfIds = Lists.partition(questionIds, questionIds.size() / foldsNum);
		
		System.out.println("Total number of questions: " + questionIds.size());
		System.out.println("Number of folds: " + foldsOfIds.size());
		for (List<Integer> foldOfIds : foldsOfIds) { 
			System.out.println("Total questions in single fold: " + foldOfIds.size());
		}
		
		for (int i = 0; i < foldsNum; i++) { 
			List<Integer> trainIds = new ArrayList<>();
			List<Integer> testIds = foldsOfIds.get(i);
			
			for (int j = 0; j < foldsNum; i++) {
				if (i != j) {
					trainIds.addAll(foldsOfIds.get(j));
				}
			}
			
			this.writeExamples(outputDir + "/" + "fold-" + i + "/svm.train", trainIds);
			this.writeExamples(outputDir + "/" + "fold-" + i + "/svm.test", testIds);
		}
		
		return this;
	}

	/**
	 * Writes the examples related to the questions with the specified
	 *  question id
	 * @param outputFile The output file which will contain the examples
	 * @param qids The ids of questions to consider
	 */
	private void writeExamples(String outputFile, List<Integer> qids) {
		WriteFile out = new WriteFile(outputFile);
		for (Integer qid : qids) { 
			for (String example : this.examples.get(qid)) { 
				out.writeLn(example);
			}
		}
		out.close();
	}
	
	public static Set<String> getTagsetByLang(String lang) { 
		assert lang != null;
		
		String[] tagset = null;
		if (lang.equals("en")) { 
			tagset = englishTagset;
		} else if (lang.equals("it")) {
			tagset = italianTagset;		
		} else {
			tagset = emptyTagset;
		}
		
		return new TreeSet<>(Arrays.asList(tagset));
	}
	
	public static void main(String[] args) throws UIMAException {
		Options options = new Options();
		options.addOption(HELP_OPT, false, "Print the help");
		options.addOption(LANG_OPT, true, // handle the question focus lang
				"The lang of the processing data");
		options.addOption(ARGUMENTS_FILE_OPT, true,
				"The path of the file containing the command line arguments");
		options.addOption(TRAINING_QUESTIONS_FOCUS_PATH_OPT, true,
				"The path of the file containing the data for training question focus");
		options.addOption(TRAINING_CASES_DIR_OPT, true,
				"The path where training CASes are stored (this enables file persistence)");
		options.addOption(TRAINING_OUTPUT_DIR_OPT, true,
				"The path where the training files will be stord.");

		// Add the className to the args
		String className = FocusClassifier.class.getSimpleName();
	
		CommandLineParser parser = new CommandLineParser();
		
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption(HELP_OPT)) {
				parser.printHelpAndExit(className, options);
			}
			
			System.out.println(className);
			
			String lang = cmd.getOptionValue(LANG_OPT, 
					"Please specify the language.");
			
			System.out.println(" -" + LANG_OPT + " " + lang);
			
			//String trainQuestionFocusPath = getFileOption(cmd,
			String trainQuestionFocusPath =  cmd.getFileValue(
					TRAINING_QUESTIONS_FOCUS_PATH_OPT,
					"Please specify the path of the question focus data file.");
			
			System.out.println(" -" + TRAINING_QUESTIONS_FOCUS_PATH_OPT + " " + trainQuestionFocusPath);
			
			String trainCasesDir = cmd.getPathValue(
					TRAINING_CASES_DIR_OPT,
					"Plase specify a valid directory for the traning CASes.");
			
			System.out.println(" -" + TRAINING_CASES_DIR_OPT + " " + trainCasesDir);

			String trainOutputDir = cmd.getPathValue(
					TRAINING_OUTPUT_DIR_OPT,
					"Please specify a valid output directory for training data");
			
			System.out.println(" -" + TRAINING_OUTPUT_DIR_OPT + " " + trainOutputDir);

			Analyzer analyzer = Commons.instantiateQuestionFocusAnalyzer(lang);
			
			// Set persistence
			UIMAPersistence persistence = trainCasesDir == null 
				? new UIMANoPersistence()
				: new UIMAFilePersistence(trainCasesDir);
			analyzer.setPersistence(persistence);

			// Check that the train question focus file does exist
			if (!new File(trainQuestionFocusPath).isFile()) {
				System.err.println("File '" + trainQuestionFocusPath + "' does not exist. Please check the path.");
				System.exit(1);
			}
			
			String trainOutputFilePath = new File(trainOutputDir, "svm.train").toString();
			
			Set<String> allowedTags = Focus.allowedTags(lang);
			
			//QuestionWithIdReader reader = new QuestionWithIdReader(trainQuestionFocusPath, QID_QUESTION_SEPARATOR);
			
			new FocusClassifierTrain(analyzer, allowedTags) 
				.generateExamples(trainQuestionFocusPath)
				.printStatistics()
				.writeExamplesToDisk(trainOutputFilePath);

			// Process data and generate train samples for the Focus classifier
			//new FocusClassifier(analyzer, allowedTags)
			//	.generateExamples(trainQuestionFocusPath)
			//	.printStatistics()
			//	.writeExamplesToDisk(trainOutputFilePath);
		} catch (ParseException e) { 
			System.err.println("Error in parsing the  command line. Use -help for usage.");
			e.printStackTrace();
		}
	}
}

