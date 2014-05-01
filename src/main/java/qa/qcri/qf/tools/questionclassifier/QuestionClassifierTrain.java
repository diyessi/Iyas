package qa.qcri.qf.tools.questionclassifier;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

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

public class QuestionClassifierTrain {
	
	private static final String HELP_OPT = "help";
	private static final String LANG_OPT = "lang";
	private static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String TRAINING_QUESTION_CLASSIFIER_OPT = "trainQuestionClassifierPath";
	private static final String TRAINING_CASES_DIR_OPT = "trainCasesDir";
	private static final String TRAINING_OUTPUT_DIR_OPT = "trainOutputDir";
	
	private final Analyzer analyzer;
	
	public QuestionClassifierTrain(Analyzer analyzer){
		if (analyzer == null)
			throw new NullPointerException("analyzer is null");
		
		this.analyzer = analyzer;
	}
	
	
	public QuestionClassifierTrain generateExamples(String dataFilepath, String outputDirpath) throws UIMAException { 
		if (dataFilepath == null)
			throw new NullPointerException("dataFilepath is null");
		if (outputDirpath == null)
			throw new NullPointerException("outputDirpath is null");
		
		Set<String> categories = Commons.analyzeAndCollectCategories(dataFilepath, analyzer);
		
		System.out.printf("Found (%d) categories: %s", categories.size(), categories);
		
		//String parametersList = Commons.getParameterList();
		
		FileManager fm = new FileManager();
		
		TokenTreeProvider treeProvider = new ConstituencyTreeProvider();
		
		TreeSerializer ts = new TreeSerializer();
		
		JCas cas = JCasFactory.createJCas();
		
		Iterator<CategoryContent> questions = new QuestionReader(dataFilepath).iterator();
		
		String parametersList = Commons.getParameterList();
		
		//Map<String, List<String>> category2examples = new HashMap<>();
		
		while (questions.hasNext()) { 
			CategoryContent question = questions.next();
			System.out.println("doctxt: " + question.getContent());
			analyzer.analyze(cas, question);
			
			String tree = ts.serializeTree(treeProvider.getTree(cas), parametersList);
			System.out.println("tree: " + tree);
			
			for (String category : categories) { 
				String label = category.equals(question.getCategory()) ? "+1" : "-1";
				String outputFile = new File(outputDirpath, category + ".train").toString();
				String example = label + " |BT| " + tree + " |ET|";
				
				fm.writeLn(outputFile, example);
			}
		}
		
		fm.closeFiles();		
		return this;
		//return category2examples;		
	}	
	
	
	public static void main(String[] args) throws UIMAException {
		Options options = new Options();
		options.addOption(HELP_OPT, false, "Print the help");
		options.addOption(LANG_OPT, true, // handle the questions lang
				"The language of the processed questions.");
		options.addOption(ARGUMENTS_FILE_OPT, true, 
				"The path of the file containing the command line arguments");
		options.addOption(TRAINING_QUESTION_CLASSIFIER_OPT, true,
				"The path of the file containing the data for training the question classifier");
		options.addOption(TRAINING_CASES_DIR_OPT, true,
				"The path where training CASes are stored (this enables file persistence)");
		options.addOption(TRAINING_OUTPUT_DIR_OPT, true,
				"The path where the training files will be stored");
		
		CommandLineParser parser = new CommandLineParser();
		
		String className = QuestionClassifierTrain.class.getSimpleName();
		
		try {
			CommandLine cmd = parser.parse(options, args);
					
			if (cmd.hasOption(HELP_OPT)) {
				parser.printHelpAndExit(className, options);
			}
			
			System.out.println(className);
			
			String lang = cmd.getOptionValue(LANG_OPT, 
					"Please specify the language.");
			
			System.out.println(" -"  + LANG_OPT + " " + lang);
			
			String trainQuestionClassifierPath = cmd.getFileValue(
					TRAINING_QUESTION_CLASSIFIER_OPT,
					"Please specify the path of the question classifier data fle.");
			
			System.out.println(" -" + TRAINING_QUESTION_CLASSIFIER_OPT + " " + trainQuestionClassifierPath);
			
			String trainCasesDir = cmd.getPathValue(
					TRAINING_CASES_DIR_OPT,
					"Please specify a valid directory for the training CASes.");
			System.out.println(" -" + TRAINING_CASES_DIR_OPT + " " + trainCasesDir);
			
			String trainOutputDir = cmd.getPathValue(
					TRAINING_OUTPUT_DIR_OPT,
					"Please specify a valid output directory for training data.");
			
			System.out.println(" -" + TRAINING_OUTPUT_DIR_OPT + " " + trainOutputDir);
								
			// Set persistence
			UIMAPersistence persistence = 
					(trainCasesDir == null)
					? new UIMANoPersistence()
					: new UIMAFilePersistence(trainCasesDir);
					
			Analyzer analyzer = Commons.instantiateQuestionClassifierAnalyzer(lang);
			analyzer.setPersistence(persistence);
				
			QuestionClassifierTrain questionClassifierTrain = new QuestionClassifierTrain(analyzer);
			questionClassifierTrain.generateExamples(trainQuestionClassifierPath, trainOutputDir);
		} catch (ParseException e) { 
			System.err.println(e.getMessage());
		}		
	}

}
