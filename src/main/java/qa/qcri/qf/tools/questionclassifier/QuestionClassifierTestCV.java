package qa.qcri.qf.tools.questionclassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

public class QuestionClassifierTestCV {
	
	public static final String TEST_QUESTIONS_CV_DIRECTORY_OPT = "testQuestionClassifierCVPath";
	
	public static final String TEST_MODELS_CV_DIRECTORY_OPT = "testModelsDirCV";
	
	public static final String TEST_OUTPUT_DIRECTORY_CV_OPT = "testOutputDir";
	
	public static final String CASES_DIRECTORY_OPT = "casesDir";
	
	public static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	
	/** The Language of the processed question */	
	public static final String LANG_OPT = "lang";	

	public static final String HELP_OPT = "help";
	
	//private Logger logger = LoggerFactory.getLogger(QuestionClassifierTestCV.class);
	
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
			
			String testFilesDir = cmd.getPathValue(
					TEST_QUESTIONS_CV_DIRECTORY_OPT, 
					"Please specify a valid directory path for the question-category test data files (CV).");
			
			System.out.println(" -" + TEST_QUESTIONS_CV_DIRECTORY_OPT + " " + testFilesDir);
			
			String casesDirpath = cmd.getPathValue(
					CASES_DIRECTORY_OPT, 
					"Please specify a valid directory for the CASes");
			
			System.out.println(" -" + CASES_DIRECTORY_OPT + " " + casesDirpath);
			
			String testModelsCVDirpath = cmd.getPathValue(
					TEST_MODELS_CV_DIRECTORY_OPT, 
					"Please specify a valid directory containing the question-category classifier models (CV).");
			
			System.out.println(" -" + TEST_MODELS_CV_DIRECTORY_OPT + " " + testModelsCVDirpath);
			
			UIMAPersistence persistence = (casesDirpath == null)
					? new UIMANoPersistence()
					: new UIMAFilePersistence(casesDirpath);
			
			Analyzer analyzer = Commons.instantiateQuestionClassifierAnalyzer(lang);
			analyzer.setPersistence(persistence);
						
			List<QuestionClassifierTest> tests = new ArrayList<>();
			
			for (File testFile : new File(testFilesDir).listFiles()) {
				String testFilename  = FilenameUtils.getBaseName(testFile.getName());
				String modelsDir = FilenameUtils.normalize(testModelsCVDirpath + "/" + testFilename);
				
				System.out.println("testFile: " + testFile.getPath() + ", modelsDir: " + modelsDir);
				QuestionClassifierTest test = new QuestionClassifierTest(analyzer);
				test.generatePredictions(testFile.getPath(), modelsDir);
				tests.add(test);
			}
			
			double totalAccuracy = 0;
			List<Double> accuracies = new ArrayList<>();
			for (int i = 0; i < tests.size(); i++) {
				QuestionClassifierTest test = tests.get(i);
				double accuracy = test.getAccuracy();
				totalAccuracy += accuracy;
				accuracies.add(accuracy);
				
				System.out.printf("(%d-fold) accuracy: %.4f (%d / %d)\n", i, accuracy, test.getCorrectPredictionsNumber(), test.getTotalPredictionsNumber());
			}
			
			System.out.printf("(total)  num-folds: %d, accuracy: %.4f\n", accuracies.size(), (totalAccuracy / accuracies.size()));
			
		} catch (ParseException e) {
			System.err.println(e);
		}
	}
}
