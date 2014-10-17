package qa.qcri.qf.tools.questionfocus;

import java.io.File;
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

public class FocusClassifierTrainCV {
	
	private static final String HELP_OPT = "help";
	private static final String LANG_OPT = "lang";
	private static	 final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String CASES_DIRECTORY_OPT = "casesDir";
	private static final String TRAIN_QUESTIONS_CV_DIRECTORY_OPT = "trainQuestionsCVDirectory";
	private static final String TRAIN_OUTPUT_CV_DIRECTORY_OPT =  "trainOutputCVDirpath";
		
	private static final String QID_QUESTION_SEPARATOR = "\t";
	
	public static void main(String[] args) throws UIMAException {
		Options options = new Options();
		options.addOption(HELP_OPT, false, "Print the help");
		options.addOption(LANG_OPT, true, // handle questions lang
				"The language of the processed questions.");
		options.addOption(ARGUMENTS_FILE_OPT, true,
				"The path of the file containing the command line arguments.");
		options.addOption(CASES_DIRECTORY_OPT, true, 
				"The directory where to store CASes (it enables serialization)");
		options.addOption(TRAIN_QUESTIONS_CV_DIRECTORY_OPT, true, 
				"The directory path of the files containing the data for training k different question-focus classifiers.");
		options.addOption(TRAIN_OUTPUT_CV_DIRECTORY_OPT, true, 
				"The path where the training files will be stored");
		
		CommandLineParser parser = new CommandLineParser();
		
		String className = FocusClassifierTrainCV.class.getSimpleName();
		
		try {
			CommandLine cmd = parser.parse(options, args);
		
			if (cmd.hasOption(HELP_OPT)) {
				parser.printHelpAndExit(className, options);
			}
		
			System.out.println(className);
			
			String lang = cmd.getOptionValue(LANG_OPT, 
					"Please specify the language.");
			
			System.out.println(" -" + LANG_OPT + " " + lang);
			
			//String cmd.get
			String trainQuestionsCVDirpath = cmd.getPathValue(
					TRAIN_QUESTIONS_CV_DIRECTORY_OPT, 
					"Please specify a valid directory path for the question-focus classifiers (CV) train files.");
			
			System.out.println(" -" + TRAIN_QUESTIONS_CV_DIRECTORY_OPT + " " + trainQuestionsCVDirpath);
			
			String casesDirpath = cmd.getPathValue(
					CASES_DIRECTORY_OPT,
					"Please specify a valid directory for the CASes.");
			
			System.out.println(" -" + CASES_DIRECTORY_OPT + " " + casesDirpath);
			
			String trainOutputCVDirpath = cmd.getPathValue(
					TRAIN_OUTPUT_CV_DIRECTORY_OPT, 
					"Please specify a valid output directory for the CV training data.");
			
			System.out.println(" -" + TRAIN_OUTPUT_CV_DIRECTORY_OPT + " " + trainOutputCVDirpath);
			
			
			UIMAPersistence persistence = (casesDirpath == null)
					? new UIMANoPersistence()
					: new UIMAFilePersistence(casesDirpath);
					
			Analyzer analyzer = Commons.instantiateQuestionFocusAnalyzer(lang);
			analyzer.setPersistence(persistence);
					
			Set<String> allowedTags = Focus.allowedTags(lang);
			System.out.println("lang: " + lang + ", allowedTags " + allowedTags);
					
			for (File trainFile : new File(trainQuestionsCVDirpath).listFiles()) {
				String fname = FilenameUtils.getBaseName(trainFile.getName());
				String outputFilepath = FilenameUtils.normalize(trainOutputCVDirpath + "/" + fname + "/svm.train");
				
				System.out.println("train file: " + trainFile.getPath() + ", output file: " + outputFilepath);
				
				Commons.analyzeQuestionsWithId(trainFile.getPath(), analyzer);
				
				FocusClassifierTrain train = new FocusClassifierTrain(analyzer, allowedTags);
				train.generateExamples(trainFile.getPath())
					 .printStatistics()
					 .writeExamplesToDisk(outputFilepath);
			}
			
		} catch (ParseException e) {
			System.err.println(e);
		}
	}

}
