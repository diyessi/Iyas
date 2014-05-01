package qa.qcri.qf.tools.questionfocus;

import java.io.File;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.uima.UIMAException;

import qa.qcri.qf.cli.CommandLine;
import qa.qcri.qf.cli.CommandLineParser;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;

public class FocusClassifierRunner {

	private static final String HELP_OPT = "help";
	private static final String LANG_OPT = "lang";
	private static	 final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String TRAINING_QUESTIONS_FOCUS_PATH_OPT = "trainQuestionsFocusPath";
	private static final String TRAINING_CASES_DIR_OPT =  "trainCasesDir";
	private static final String TRAINING_OUTPUT_DIR_OPT = "trainOutputDir";
	
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
		//args = addToArray(0, className, args);

		/*
		args = new ListBuilder<String>(Arrays.asList(args))
				.prepend(className)
				.tolist()
				.toArray(new String[args.length + 1]);
		//new ArrayBuilder<String>(args).prepend(className).build();
		*/
		CommandLineParser parser = new CommandLineParser();
		
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption(HELP_OPT)) {
				parser.printHelpAndExit(className, options);
			}

			/*
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption(HELP_OPT)) {
				new HelpFormatter().printHelp("FocusClassifierRunner", options);
				System.exit(0);
			}

			String argumentsFilePath = getOptionaPathOption(cmd, 
					ARGUMENTS_FILE_OPT, "Please specify a valid arguments file");

			if (argumentsFilePath != null) { 
				String[] newArgs;

				try {
					newArgs = readArgs(argumentsFilePath);
					cmd = new BasicParser().parse(options, newArgs);
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
			 */	
			
			//System.out.println("parsing...");
			
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
			
			Set<String> allowedTags = Focus.getAllowedTagsByLanguage(lang);

			// Process data and generate train samples for the Focus classifier
			new FocusClassifier(analyzer, allowedTags)
				.generateExamples(trainQuestionFocusPath)
				.printStatistics()
				.writeExamplesToDisk(trainOutputFilePath);
		} catch (ParseException e) { 
			System.err.println("Error in parsing the  command line. Use -help for usage.");
			e.printStackTrace();
		}

	}

}
