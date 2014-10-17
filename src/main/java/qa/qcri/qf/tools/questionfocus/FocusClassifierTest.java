package qa.qcri.qf.tools.questionfocus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qa.qcri.qf.classifiers.Classifier;
import qa.qcri.qf.classifiers.SVMLightTKClassifierFactory;
import qa.qcri.qf.cli.CommandLine;
import qa.qcri.qf.cli.CommandLineParser;
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



public class FocusClassifierTest {

	private static final String HELP_OPT = "help";
	private static final String LANG_OPT = "lang";
	private static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String CASES_DIRECTORY_OPT = "casesDir";
	private static final String TEST_QUESTIONS_FILEPATH_OPT = "testQuestionsFilepath";
	private static final String TEST_MODEL_FILEPATH_OPT = "testModelFilepath";

	private static final String QID_QUESTION_SEPARATOR = "\t"; // line separator: <qid, text>  

	private static TreeSerializer treeSerializer = 
			new TreeSerializer().enableAdditionalLabels();

	private final Analyzer analyzer;
	private final Set<String> allowedTags;

	private int nullPredictionsNumber = 0; // predictions with no examples generated.
	private int totalPredictionsNumber = 0;
	private int correctPredictionsNumber = 0;
	
	//private static Logger logger = LoggerFactory.getLogger(FocusClassifierTest.class);
	private static Logger logger = LoggerFactory.getLogger(FocusClassifierTest.class);


	public FocusClassifierTest(Set<String> allowedTags, Analyzer analyzer) {
		if (allowedTags == null)
			throw new NullPointerException("allowedTags is null");
		if (analyzer == null)
			throw new NullPointerException("analyzer is null");

		this.analyzer = analyzer;
		this.allowedTags = allowedTags;
	}

	public FocusClassifierTest generatePredictions(String testFilepath, String modelFilepath) 
			throws UIMAException {
		if (testFilepath == null)
			throw new NullPointerException("testFilepath is null");
		if (modelFilepath == null)
			throw new NullPointerException("modelFilepath is null");

		Classifier classifier = 
				new SVMLightTKClassifierFactory()
		.createClassifier(modelFilepath);

		nullPredictionsNumber = 0;
		totalPredictionsNumber = 0;
		correctPredictionsNumber = 0;

		//QuestionWithIdReader questionsReader = new QuestionWithIdReader(testFilepath, QID_QUESTION_SEPARATOR);

		Iterator<QuestionWithFocus> questionsWithFocus = 
				new QuestionWithIdReader(testFilepath, QID_QUESTION_SEPARATOR)
					.iterator();

		JCas cas = JCasFactory.createJCas();

		while (questionsWithFocus.hasNext()) { 
			//QuestionWithFocus questionWithAnnotatedFocus = questionsWithAnnotatedFocus.next();
			QuestionWithFocus questionWithFocus = questionsWithFocus.next();
			
			assert questionWithFocus.hasFocus();
			
			if (questionWithFocus.getFocusNumber() > 1) continue;
			
			Focus expectFocus = questionWithFocus.getFoci().get(0);
			
			analyzer.analyze(cas, questionWithFocus);

			TokenTree tree = RichTree.getConstituencyTree(cas);

			List<Pair<String, RichTokenNode>> examples = generateExamples(tree);
			
			//logger.debug("examples: " + examples.size());

			RichTokenNode focusNode = predictFocusNode(classifier, examples);
			
			if (focusNode == null) { 
				nullPredictionsNumber++;
				continue;
			}
			
			String focustxt = focusNode.getValue();
			
			int begin = focusNode.getToken().getBegin();
			Focus actualFocus = new Focus(focustxt, begin);
			
			logger.debug("actual focus:   " + actualFocus);
			logger.debug("expected focus: " + expectFocus);
			
			if (actualFocus.equals(expectFocus)) { 
				correctPredictionsNumber++;
			} else {
				logger.warn(questionWithFocus + " Predicted " + actualFocus.getWord() 
							+ ". Was " + expectFocus.getWord());
			}

			totalPredictionsNumber++;	
		}
		

		return this;
	}

	public RichTokenNode predictFocusNode(Classifier classifier, List<Pair<String, RichTokenNode>> examples) {
		if (classifier == null)
			throw new NullPointerException("classifier is null");
		if (examples == null)
			throw new NullPointerException("examples is null");

		Double maxPrediction = Double.NEGATIVE_INFINITY;
		RichTokenNode focusNode = null;
		for (Pair<String, RichTokenNode> example : examples) {
			Double prediction = classifier.classify(example.getA());

			if (prediction > maxPrediction) {
				maxPrediction = prediction;
				focusNode = example.getB();
			}
		}

		return focusNode;
	}

	public List<Pair<String, RichTokenNode>> generateExamples(TokenTree tree) {
		if (tree == null)
			throw new NullPointerException("tree is null");

		List<Pair<String, RichTokenNode>> examples = new ArrayList<>();

		for (RichTokenNode node : tree.getTokens()) {
			RichNode posTag = node.getParent();

			if (!allowedTags.contains(posTag.getValue())) continue;

			posTag.addAdditionalLabel(Commons.QUESTION_FOCUS_KEY);

			boolean isFocus = node.getMetadata().containsKey(Commons.QUESTION_FOCUS_KEY);

			String label = isFocus ? "+1" : "-1";

			String taggedTree = treeSerializer.serializeTree(tree, Commons.getParameterList());

			StringBuilder example = new StringBuilder();
			example.append(label);
			example.append("|BT| ");
			example.append(taggedTree);
			example.append(" |ET|");	

			examples.add(new Pair<>(example.toString(), node));

			posTag.removeAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
		}

		return examples;
	}		

	public int getNullPredictionsNumber() {
		return nullPredictionsNumber;
	}		

	public int getTotalPredictionsNumber() {
		return totalPredictionsNumber;
	}

	public int getCorrectPredictionsNumber() {
		return correctPredictionsNumber;
	}

	public double getAccuracy() { 
		return (double) (correctPredictionsNumber) / totalPredictionsNumber;
	}

	public static void main(String[] args) throws UIMAException { 
		Options options = new Options();
		options.addOption(HELP_OPT, false, "Print the help");
		options.addOption(LANG_OPT, true, // handle questions lang)
				"The language of the processed questions.");
		options.addOption(ARGUMENTS_FILE_OPT, true, 
				"The path of the file containing the command line arguments.");
		options.addOption(CASES_DIRECTORY_OPT, true,
				"The directory where to store CASes (it enables serialization)");
		options.addOption(TEST_QUESTIONS_FILEPATH_OPT, true, 
				"The filepath of the files containing the test questions for Focus Classifier.");
		options.addOption(TEST_MODEL_FILEPATH_OPT, true,
				"The filepath of the Focus Classifier model.");

		CommandLineParser parser = new CommandLineParser();

		String className = FocusClassifierTest.class.getSimpleName();

		try { 

			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption(HELP_OPT)) { 
				parser.printHelpAndExit(className, options);
			}

			System.out.println(className);

			String lang = cmd.getOptionValue(LANG_OPT, 
					"Please specify the language.");

			System.out.println(" -" + LANG_OPT + " " + lang);

			String testQuestionsFilepath = cmd.getFileValue(
					TEST_QUESTIONS_FILEPATH_OPT, 
					"Please specify a valid file containing the Question-Focus test questions.");

			System.out.println(" -" + TEST_QUESTIONS_FILEPATH_OPT + " " + testQuestionsFilepath);

			String testModelFilepath = cmd.getFileValue(
					TEST_MODEL_FILEPATH_OPT,
					"Please specify a valid file containing the Question-Focus classifier model.");

			System.out.println(" -" + TEST_MODEL_FILEPATH_OPT + " " + testModelFilepath);

			String casesDirpath = cmd.getPathValue(
					CASES_DIRECTORY_OPT,
					"Please specify a valid directory for the CASes.");

			System.out.println(" -" + CASES_DIRECTORY_OPT + " " + casesDirpath);

			UIMAPersistence persistence = (casesDirpath == null)
					? new UIMANoPersistence()
					: new UIMAFilePersistence(casesDirpath);

			Analyzer analyzer = Commons.instantiateQuestionFocusAnalyzer(lang);
			analyzer.setPersistence(persistence);

			Set<String> allowedTags = //Focus.getAllowedTagsByLanguage(lang); 
					Focus.allowedTags(lang);
			
			Commons.analyzeQuestionsWithId(testQuestionsFilepath, analyzer);

			FocusClassifierTest test = new FocusClassifierTest(allowedTags, analyzer);
			test.generatePredictions(testQuestionsFilepath, testModelFilepath);

			System.out.printf("accuracy: %.4f, ", test.getAccuracy());
			System.out.printf("correct: %d, ",  test.getCorrectPredictionsNumber());
			System.out.printf("total: %d, ", test.getTotalPredictionsNumber());
			System.out.printf("null: %d\n", test.getNullPredictionsNumber());

			//System.out.printf("( - ) accuracy: %4.f (%d / %d - %d ?)\n", accuracy, correctPredictionsNumber, totalPredictionsNumber, nullPredictionsNumber);
		} catch (ParseException e) { 
			System.err.println(e);
		}
	}
}

