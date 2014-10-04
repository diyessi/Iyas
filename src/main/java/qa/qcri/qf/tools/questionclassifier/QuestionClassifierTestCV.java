package qa.qcri.qf.tools.questionclassifier;

import java.io.File;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qa.qcri.qf.fileutil.FileManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

import qa.qcri.qf.classifiers.OneVsAllClassifier;
import qa.qcri.qf.classifiers.SVMLightTKClassifierFactory;
import qa.qcri.qf.cli.CommandLine;
import qa.qcri.qf.cli.CommandLineParser;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.providers.ConstituencyTreeProvider;
import qa.qcri.qf.trees.providers.TokenTreeProvider;

public class QuestionClassifierTestCV {
	
	public static final String TEST_QUESTIONS_CV_DIRECTORY_OPT = "testQuestionClassifierCVPath";
	
	public static final String TEST_MODELS_CV_DIRECTORY_OPT = "testModelsDirCV";
	
	public static final String TEST_OUTPUT_DIRECTORY_CV_OPT = "testOutputDir";
	
	public static final String CASES_DIRECTORY_OPT = "casesDir";
	
	public static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	
	public static final String LANG_OPT = "lang";	

	public static final String HELP_OPT = "help";
	
	private static final Logger logger = LoggerFactory.getLogger(QuestionClassifierTestCV.class);
	
	public static void main(String[] args) throws UIMAException { 
		Options options = new Options();
		options.addOption(HELP_OPT, false, "Print the help");
		options.addOption(LANG_OPT, true, 
				"The language of the processed questions"); // handle  the questions language
		options.addOption(ARGUMENTS_FILE_OPT, true,
				"The path of the file containing the command line arguments");
		options.addOption(TEST_QUESTIONS_CV_DIRECTORY_OPT, true,
				"The path of directory containing folds for the test questions");
		options.addOption(TEST_MODELS_CV_DIRECTORY_OPT, true, 
				"The path of the directory containing models for the train folds.");
		/*
		options.addOption(TEST_OUTPUT_DIRECTORY_CV_OPT, true,
				"The path of the directory containing predictions for test folds.");
		*/
		options.addOption(CASES_DIRECTORY_OPT, true, 
				"The path where test CASes are stored (this enables file persistence)");
		
		//TODO to complete
		CommandLineParser parser = new CommandLineParser();
		
		String className = QuestionClassifierTestCV.class.getSimpleName();
		
		try {
			CommandLine cmd = parser.parse(options, args);
			
			if (cmd.hasOption(HELP_OPT)) {
				parser.printHelpAndExit(className, options);
			}
			
			System.out.println(className);
			
			String lang = cmd.getOptionValue(LANG_OPT, 
					"Please specify the language.");
			
			System.out.println(" -" + LANG_OPT + " " + lang);
			
			String testQuestionsCVDirectoryPath = cmd.getPathValue(
					TEST_QUESTIONS_CV_DIRECTORY_OPT, 
					"Please specify a valid directory path for the question-category test data files (CV).");
			
			System.out.println(" -" + TEST_QUESTIONS_CV_DIRECTORY_OPT + " " + testQuestionsCVDirectoryPath);
			
			String casesDirpath = cmd.getPathValue(
					CASES_DIRECTORY_OPT, 
					"Please specify a valid directory for the CASes");
			
			System.out.println(" -" + CASES_DIRECTORY_OPT + " " + casesDirpath);
			
			String testModelsCVDirpath = cmd.getPathValue(
					TEST_MODELS_CV_DIRECTORY_OPT, 
					"Please specify a valid directory containing for the question-category classifier models (CV).");
			
			System.out.println(" -" + TEST_MODELS_CV_DIRECTORY_OPT + " " + testModelsCVDirpath);
			
			UIMAPersistence persistence = (casesDirpath == null)
					? new UIMANoPersistence()
					: new UIMAFilePersistence(casesDirpath);
			
			Analyzer analyzer = Commons.instantiateQuestionClassifierAnalyzer(lang);
			analyzer.setPersistence(persistence);
			
			
			List<QuestionClassifierTestFold> questionClassifierTestCVFolds = new ArrayList<>();
			
			for (File testFile : new File(testQuestionsCVDirectoryPath).listFiles()) {
				String testFilename  = FilenameUtils.getBaseName(testFile.getName());
				String modelsDirname = FilenameUtils.normalize(testModelsCVDirpath + "/" + testFilename);
				
				System.out.println("testFilename: " + testFile.getPath() + ", modelsDirname: " + modelsDirname);
				QuestionClassifierTestFold qctf = new QuestionClassifierTestFold(analyzer);
				qctf.generatePredictions(testFile.getPath(), modelsDirname);
				questionClassifierTestCVFolds.add(qctf);
			}
			
			double totalAccuracy = 0;
			List<Double> accuracies = new ArrayList<>();
			for (int i = 0; i < questionClassifierTestCVFolds.size(); i++) {
				QuestionClassifierTestFold qctf = questionClassifierTestCVFolds.get(i);
				double accuracy = qctf.getAccuracy();
				totalAccuracy += accuracy;
				accuracies.add(accuracy);
				
				System.out.printf("(%d-fold) accuracy: %.4f (%d / %d)\n", i, accuracy, qctf.getCorrectPredictionsNumber(), qctf.getTotalPredictionsNumber());
			}
			
			System.out.printf("(total)  num-folds: %d, accuracy: %.4f\n", accuracies.size(), (totalAccuracy / accuracies.size()));
			
		} catch (ParseException e) {
			System.err.println(e);
		}
	}
	
	private static class QuestionClassifierTestFold {
		
		private final Analyzer analyzer;
				
		private int totalPredictionsNumber = 0;
		
		private int correctPredictionsNumber = 0;
		
		private QuestionClassifierTestFold(Analyzer analyzer) {
			if (analyzer == null)
				throw new NullPointerException("analyzer is null");
			
			this.analyzer = analyzer;
		}
		
		private QuestionClassifierTestFold generatePredictions(String testFilepath, String modelsDirpath)  
			throws UIMAException {
			if (testFilepath == null)
				throw new NullPointerException("testFilepath is null");
			if (modelsDirpath == null)
				throw new NullPointerException("modelsDirpath is null");
			
			totalPredictionsNumber = 0;
			correctPredictionsNumber = 0;
			
			QuestionWithIdReader questionReader = new QuestionWithIdReader(testFilepath, "\t");
			Set<String> categories = Commons.analyzeQuestionsWithIdAndCollectCategories(questionReader, analyzer);
			
			String parametersList = Commons.getParameterList();
			
			FileManager fm = new FileManager();
			
			TokenTreeProvider treeProvider = new ConstituencyTreeProvider();
			
			TreeSerializer ts = new TreeSerializer();
			
			JCas cas = JCasFactory.createJCas();
			
			//int totalPredictionsNumber = 0;
			//int correctPredictionsNumber = 0;
			
			OneVsAllClassifier ovaClassifier = 
					new OneVsAllClassifier(
							new SVMLightTKClassifierFactory());
			
			for (String category : categories) { 
				String modelFilepath = FilenameUtils.normalize(modelsDirpath + "/" + category + ".model");
				ovaClassifier.addModel(category, modelFilepath);
			}
			
			Iterator<CategoryContent> questions = new QuestionWithIdReader(
					testFilepath, "\t").iterator();
			while (questions.hasNext()) {
				CategoryContent question = questions.next();
				analyzer.analyze(cas, question);
				
				String tree = ts.serializeTree(treeProvider.getTree(cas), parametersList);
				
				String example = "|BT| " + tree + " |ET|";
				
				String predictedCategory = ovaClassifier
						.getMostConfidentModel(example);
				
				if (predictedCategory.equals(question.getCategory())) {
					correctPredictionsNumber++;
				} else {
					logger.warn(question.getContent() + " Predicted " + predictedCategory
							+ ". Was " + question.getCategory());
				}
				totalPredictionsNumber++;				
			}
			fm.closeFiles();
			
			System.out.println("Correct prediction: " + correctPredictionsNumber
					+ " out of " + totalPredictionsNumber);
			return this;
		}
		
		private int getTotalPredictionsNumber() {
			return totalPredictionsNumber;
		}
		
		private int getCorrectPredictionsNumber() { 
			return correctPredictionsNumber;
		}
		
		private double getAccuracy() { 
			return ((double) correctPredictionsNumber) / totalPredictionsNumber;
		}
		
	}
}
