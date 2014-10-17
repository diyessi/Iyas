package qa.qcri.qf.tools.questionfocus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;

import qa.qcri.qf.cli.CommandLine;
import qa.qcri.qf.cli.CommandLineParser;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;

public class FocusClassifierTestCV {
	
	private static final String HELP_OPT = "help";
	private static final String LANG_OPT = "lang";
	private static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String CASES_DIRECTORY_OPT = "casesDir";
	private static final String TEST_QUESTIONS_CV_DIRECTORY_OPT = "testQuestionsCVDirectory";
	private static final String TEST_MODELS_CV_DIRECTORY_OPT = "testModelsCVDirectory";
	// private static final String TEST_OUTPUT_CV_DIRECTORY_OPT = "testOutputCVDirpath";
	
	//private static Logger logger = LoggerFactory.getLogger(FocusClassifierTestCV.class);
	
	public static void main(String[] args) throws UIMAException {
		Options options = new Options();
		options.addOption(HELP_OPT, false, "Print the help");
		options.addOption(LANG_OPT, true, // handle questions lang
				"The language of the processed questions.");
		options.addOption(ARGUMENTS_FILE_OPT, true, 
				"The path of the file containing the command line arguments.");
		options.addOption(CASES_DIRECTORY_OPT, true, 
				"The directory where to store CASes (it enables serialization)");
		options.addOption(TEST_QUESTIONS_CV_DIRECTORY_OPT, true,
				"The directory path of the files containig the questions for testing the k different question-focus classifiers.");
		options.addOption(TEST_MODELS_CV_DIRECTORY_OPT, true, 
				"The path where the quesion-focus classifier models are stored");
		
		CommandLineParser parser = new CommandLineParser();
		
		String className = FocusClassifierTestCV.class.getSimpleName();
		
		try {
			CommandLine cmd = parser.parse(options, args);
		
			if (cmd.hasOption(HELP_OPT)) { 
				parser.printHelpAndExit(className, options);
			}
			
			System.out.println(className);
			
			String lang = cmd.getOptionValue(LANG_OPT, 
					"Please specify the language.");
			
			System.out.println(" -" + LANG_OPT + " " + lang);
			
			String testQuestionsCVDirpath = cmd.getPathValue(
					TEST_QUESTIONS_CV_DIRECTORY_OPT,
					"Please specify a valid directory path for the question-focus classifiers (CV) test files.");
			
			System.out.println(" -" + TEST_QUESTIONS_CV_DIRECTORY_OPT + " " + testQuestionsCVDirpath);
			
			String testModelsCVDirpath = cmd.getPathValue(
					TEST_MODELS_CV_DIRECTORY_OPT, 
					"Please specify a valid directory containing for the question-focus classifier models (CV).");
			
			System.out.println(" -" + TEST_MODELS_CV_DIRECTORY_OPT + " " + testModelsCVDirpath);
			
			String casesDirpath = cmd.getPathValue(
					CASES_DIRECTORY_OPT, 
					"Please specify a valid directory for the CASes.");
			
			System.out.println(" -" + CASES_DIRECTORY_OPT + " " + casesDirpath);
			
			UIMAPersistence  persistence = (casesDirpath == null)
					? new UIMANoPersistence()
					: new UIMAFilePersistence(casesDirpath);
					
			Analyzer analyzer = Commons.instantiateQuestionFocusAnalyzer(lang);
			analyzer.setPersistence(persistence);
			
			Set<String> allowedTags = Focus.allowedTags(lang);
			List<String> foldNames = new ArrayList<>();
			List<FocusClassifierTest> tests = new ArrayList<>();
			for (File testFile : new File(testQuestionsCVDirpath).listFiles()) { 
				String fname =  FilenameUtils.getBaseName(testFile.getName());
				
				foldNames.add(fname);
				String modelFilepath = FilenameUtils.normalize(testModelsCVDirpath + "/" + fname + "/svm.model");
				
				System.out.println("test file: " + testFile.getPath() + ", model file:" + modelFilepath);

				//QuestionWithIdReader questionsWithAnnotatedFocus = new QuestionWithIdReader(testFile.getPath(), "\t");
				Commons.analyzeQuestionsWithId(testFile.getPath(), analyzer);
				
				FocusClassifierTest test = new FocusClassifierTest(allowedTags, analyzer);
				test.generatePredictions(testFile.getPath(), modelFilepath);
				tests.add(test);
			}
			
			double totalAccuracy = 0;
			List<Double> accuracies = new ArrayList<>();
			for (int i = 0; i < tests.size(); i++) {
				FocusClassifierTest test = tests.get(i);
				double accuracy = test.getAccuracy();
				totalAccuracy += accuracy;
				accuracies.add(accuracy);
				
				System.out.printf("(%s-fold) accuracy: %.4f (%d / %d - %d ?)\n", foldNames.get(i), accuracy, test.getCorrectPredictionsNumber(), test.getTotalPredictionsNumber(), test.getNullPredictionsNumber());
			}
			
			System.out.printf("(total)  num-folds: %d, accuracy: %4f\n", accuracies.size(), (totalAccuracy / accuracies.size()));
				
		} catch (ParseException e) { 
			System.err.println(e);
		}
	}
		
}
