package qa.qcri.qf.tools.questionclassifier;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.cli.CommandLine;
import qa.qcri.qf.cli.CommandLineParser;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.providers.ConstituencyTreeProvider;
import qa.qcri.qf.trees.providers.TokenTreeProvider;

public class QuestionClassifierTrainCV {
	
	private static final String HELP_OPT = "help";
	private static final String LANG_OPT = "lang";
	private static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String TRAINING_QUESTION_CLASSIFIER_CV_OPT = "trainQuestionClassifierCVPath";
	private static final String CASES_DIR_OPT = "CasesDir";
	private static final String TRAINING_OUTPUT_DIR_CV_OPT = "trainOutputDirCV";

	public static void main(String[] args) throws UIMAException { 
		Options options = new Options();
		options.addOption(HELP_OPT, false, "Print the help");
		options.addOption(LANG_OPT, true, // handle the questions lang
				"The language of the processed questions.");
		options.addOption(ARGUMENTS_FILE_OPT, true, 
				"The path of the file containing the command line arguments");
		options.addOption(TRAINING_QUESTION_CLASSIFIER_CV_OPT, true,
				"The directory path of the files containing the data for training k different question-categories classifiers");
		options.addOption(CASES_DIR_OPT, true,
				"The directory where to store CASes (it enables serialization)");
		options.addOption(TRAINING_OUTPUT_DIR_CV_OPT, true,
				"The path where the training files will be stored");
		
		CommandLineParser parser = new CommandLineParser();
		
		String className = QuestionClassifierTrainCV.class.getSimpleName();
		
		try {
			CommandLine cmd = parser.parse(options, args);
			
			if (cmd.hasOption(HELP_OPT)) { 
				parser.printHelpAndExit(className, options);
			}
			
			System.out.println(className);
			
			String lang = cmd.getOptionValue(LANG_OPT, 
					"Please specify the language.");
			
			System.out.println(" -" + LANG_OPT + " " + lang);
			
			String trainQuestionClassifierCVPath = cmd.getPathValue(
					TRAINING_QUESTION_CLASSIFIER_CV_OPT,
					"Please specify a valid directory path for the question-category classifiers (CV) data files.");
					
			System.out.println(" -" + TRAINING_QUESTION_CLASSIFIER_CV_OPT + " " + trainQuestionClassifierCVPath);
			
			String casesDirpath = cmd.getPathValue(
					CASES_DIR_OPT,
					"Please specify a valid directory for the CASes.");
			
			System.out.println(" -" + CASES_DIR_OPT + " " + casesDirpath);
			
			String trainOutputDirCV = cmd.getPathValue(
					TRAINING_OUTPUT_DIR_CV_OPT,
					"Please specify a valid output dir for the CV training data.");
			
			System.out.println(" -" + TRAINING_OUTPUT_DIR_CV_OPT + " " + trainOutputDirCV);
			
			UIMAPersistence persistence = (casesDirpath == null)
					? new UIMANoPersistence()
					: new UIMAFilePersistence(casesDirpath);
			
			Analyzer analyzer = Commons.instantiateQuestionClassifierAnalyzer(lang);
			analyzer.setPersistence(persistence);
			
			for (File fold : new File(trainQuestionClassifierCVPath).listFiles()) {
				String foldFilename = FilenameUtils.getBaseName(fold.getName());
				String outputFoldDirpath = FilenameUtils.normalize(trainOutputDirCV + "/" + foldFilename);
				System.out.println("foldFilename: " + fold.getPath() + ", outputFoldDirpath: " + outputFoldDirpath);
				QuestionClassifierTrainFold qftf = new QuestionClassifierTrainFold(analyzer);
				qftf.generateExamples(fold.getPath(), outputFoldDirpath);
			}
			
		} catch (ParseException e) {
			System.err.println(e.getMessage());
		}				
	}
	
	private static class QuestionClassifierTrainFold {
		
		private final Analyzer analyzer;
		
		private QuestionClassifierTrainFold(Analyzer analyzer) {
			assert analyzer != null;
			
			this.analyzer = analyzer;
		}
		
		private QuestionClassifierTrainFold generateExamples(String dataFilepath, String outputDirpath) throws UIMAException {
			assert dataFilepath != null;
			assert outputDirpath != null;
			
			FileManager fm = new FileManager();
		
			QuestionWithIdReader questionReader = new QuestionWithIdReader(dataFilepath, "\t");
		
			Set<String> categories = Commons.analyzeQuestionsWithIdAndCollectCategories(questionReader, analyzer);
			//Set<String> categories = Commons.collectCategoriesFromQuestionsWithId(questionReader);
			//System.out.println("categories: " + categories);
			
			TokenTreeProvider treeProvideer = new ConstituencyTreeProvider();
			
			TreeSerializer ts = new TreeSerializer();
			
			JCas cas = JCasFactory.createJCas();
			
			questionReader = new QuestionWithIdReader(dataFilepath, "\t");
			
			Iterator<CategoryContent> questions = questionReader.iterator();
			
			String parametersList = Commons.getParameterList();
			
			while (questions.hasNext()) {
				CategoryContent question = questions.next();
				
				// System.out.printf("doctxt(%s): %s\n", question.getId(), question.getContent());
				analyzer.analyze(cas, new SimpleContent(question.getId(), question.getContent()));
				
				String tree = ts.serializeTree(treeProvideer.getTree(cas), parametersList);
				System.out.printf("tree(%s): %s\n", question.getId(), tree);
				
				
				for (String  category : categories) { 
					String label = category.equals(question.getCategory()) ? "+1" : "-1";
					String outputFile = FilenameUtils.normalize(outputDirpath + "/" + category + ".train");
					String example = label + " |BT| " + tree + " |ET|";

					fm.writeLn(outputFile, example);
				}
			}			
			fm.closeFiles();
			
			return this;
		}
		
	}

}
