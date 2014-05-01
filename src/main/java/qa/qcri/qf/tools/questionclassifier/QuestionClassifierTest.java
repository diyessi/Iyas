package qa.qcri.qf.tools.questionclassifier;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.classifiers.OneVsAllClassifier;
import qa.qcri.qf.classifiers.SVMLightTKClassifierFactory;
import qa.qcri.qf.cli.CommandLine;
import qa.qcri.qf.cli.CommandLineParser;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.providers.ConstituencyTreeProvider;
import qa.qcri.qf.trees.providers.TokenTreeProvider;
import edu.berkeley.nlp.util.Logger;

public class QuestionClassifierTest {
	
	public static final String TEST_QUESTION_CLASSIFIER_PATH_OPT = "testQuestionClassifierPath";
	
	public static final String TEST_CASES_DIRECTORY_OPT = "testCasesDir";
	
	public static final String TEST_MODELS_DIRECTORY_OPT = "testModelsDir";
	
	//public static final String TEST_OUTPUT_DIRECTORY_OPT = "testOutputDir";
	
	public static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	
	public static final String LANG_OPT = "lang";	

	public static final String HELP_OPT = "help";
	
	private final Analyzer analyzer;
	
	public QuestionClassifierTest(Analyzer analyzer) { 
		if (analyzer == null)
			throw new NullPointerException("analyzer is null");
		
		this.analyzer = analyzer;
	}
	
	public QuestionClassifierTest generatePredictions(String testFilepath, String modelsDirpath) 
			throws UIMAException { 
		if (testFilepath == null)
			throw new NullPointerException("testFilepath is null");
		if (modelsDirpath == null)
			throw new NullPointerException("modelsDirpath is null");
		/*
		if (outputDirpath == null)
			throw new NullPointerException("outputDirpath is null");
		*/
					
		Set<String> categories = Commons.analyzeAndCollectCategories(
				testFilepath, analyzer);
		
		String parameterList = Commons.getParameterList();
		
		FileManager fm = new FileManager();
		
		TokenTreeProvider treeProvider = new ConstituencyTreeProvider();
		
		TreeSerializer ts = new TreeSerializer();
		
		JCas cas = JCasFactory.createJCas();
		
		int totalPredictionsNumber = 0;
		int correctPredictionsNumber = 0;
		
		OneVsAllClassifier ovaClassifier = 
				new OneVsAllClassifier(
						new SVMLightTKClassifierFactory());
		
		for (String category : categories) {
			ovaClassifier.addModel(category, new File(modelsDirpath, category + ".model").toString());
		}
		
		Iterator<CategoryContent> questions = new QuestionReader(
				testFilepath).iterator();
		while (questions.hasNext()) {
			CategoryContent question = questions.next();
			analyzer.analyze(cas, question);
			
			String tree = ts.serializeTree(treeProvider.getTree(cas), parameterList);
			
			String example = "|BT| " + tree + " |ET|";
			
			String predictedCategory = ovaClassifier
					.getMostConfidentModel(example);
					
			if (predictedCategory.equals(question.getCategory())) {
				correctPredictionsNumber++;
			} else  {
				String message = question.getContent() + " Predicted " + predictedCategory
						+ ". Was " + question.getCategory();
				fm.writeLn("data/question-classifier/errors.txt", message);
				Logger.warn(message);
			}
			
			totalPredictionsNumber++;					
		}
		
		fm.closeFiles();
		
		System.out.println("Correct prediction: " + correctPredictionsNumber
				+ " out of " + totalPredictionsNumber);
		return this;			
	}
	
	public static void main(String[] args) throws UIMAException { 
		Options options = new Options();
		options.addOption(HELP_OPT, false, "Prin the help");
		options.addOption(LANG_OPT, true, // handle the question lang
				"The language of the processed questions.");
		options.addOption(ARGUMENTS_FILE_OPT, true, 
				"The path of the file containing the command line arguments");
		options.addOption(TEST_QUESTION_CLASSIFIER_PATH_OPT, true, 
				"The path of the file containing the data for testing the question classifier");
		options.addOption(TEST_MODELS_DIRECTORY_OPT, true,
				"The path where the models are stored");
		options.addOption(TEST_CASES_DIRECTORY_OPT, true,
				"The path where the test CASes are stored (this enables file persistence)");
		/*
		options.addOption(TEST_OUTPUT_DIRECTORY_OPT, true, 
				"The path where the test files will be stored");
		*/
		String className = QuestionClassifierTest.class.getSimpleName();
		
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
			
			String testQuestionClassifierPath = cmd.getFileValue(
					TEST_QUESTION_CLASSIFIER_PATH_OPT,
					"Please specify the path of the question classifier data file.");
			
			System.out.println(" -" + TEST_QUESTION_CLASSIFIER_PATH_OPT + " " + testQuestionClassifierPath);
			
			String testModelsDir = cmd.getPathValue(
					TEST_MODELS_DIRECTORY_OPT,
					"Please specify a valid directory for the models.");
			
			System.out.println(" -" + TEST_MODELS_DIRECTORY_OPT + " " + testModelsDir);
			
			String testCasesDir = cmd.getPathValue(
					TEST_CASES_DIRECTORY_OPT,
					"Please specify a valid directory for test CASes.");
			
			System.out.println(" -" + TEST_CASES_DIRECTORY_OPT + " " + testCasesDir);
			
			/*
			String testOutputDir = cmd.getPathValue(
					TEST_OUTPUT_DIRECTORY_OPT,
					"Please specify a valid output directory for training data.");
			
			System.out.println(" -" + TEST_OUTPUT_DIRECTORY_OPT + " " + testOutputDir);
			*/
			
			UIMAPersistence persistence = 
					(testCasesDir == null)
					? new UIMANoPersistence()
					: new UIMAFilePersistence(testCasesDir);
			
			Analyzer analyzer = Commons.instantiateQuestionClassifierAnalyzer(lang);
			analyzer.setPersistence(persistence);
			
			QuestionClassifierTest questionClassifierTest = new QuestionClassifierTest(analyzer);
			questionClassifierTest.generatePredictions(testQuestionClassifierPath, testModelsDir);
		} catch (ParseException e) { 
			System.err.println(e.getMessage());
		}
	}
	

}
