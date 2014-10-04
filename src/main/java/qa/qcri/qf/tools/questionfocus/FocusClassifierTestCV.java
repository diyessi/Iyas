package qa.qcri.qf.tools.questionfocus;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
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
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import util.Pair;

public class FocusClassifierTestCV {
	
	private static final String HELP_OPT = "help";
	private static final String LANG_OPT = "lang";
	private static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String CASES_DIRECTORY_OPT = "casesDir";
	private static final String TEST_QUESTIONS_CV_DIRECTORY_OPT = "testQuestionsCVDirectory";
	private static final String TEST_MODELS_CV_DIRECTORY_OPT = "testModelsCVDirectory";
	// private static final String TEST_OUTPUT_CV_DIRECTORY_OPT = "testOutputCVDirpath";
	
	private static Logger logger = LoggerFactory.getLogger(FocusClassifierTestCV.class);
	
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
			
			Set<String> allowedTags = Focus.getAllowedTagsByLanguage(lang);
			List<String> foldNames = new ArrayList<>();
			List<FocusClassifierTest> tests = new ArrayList<>();
			for (File testFile : new File(testQuestionsCVDirpath).listFiles()) { 
				String testFilename =  FilenameUtils.getBaseName(testFile.getName());
				
				foldNames.add(testFilename);
				String modelFilepath = FilenameUtils.normalize(testModelsCVDirpath + "/" + testFilename + "/svm.model");
				
				System.out.println("test file: " + testFile.getPath() + ", model file:" + modelFilepath);

				QuestionWithIdReader questionsWithAnnotatedFocus = new QuestionWithIdReader(testFile.getPath(), "\t");
				Commons.analyzeQuestionsWithId(questionsWithAnnotatedFocus, analyzer);
				
				//FocusClassifierTestFold fctf = new FocusClassifierTestFold(allowedTags, analyzer);
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
	
	private static class FocusClassifierTestFold {
		
		private static TreeSerializer treeSerializer = new TreeSerializer();
		
		private int nullPredictionsNumber;
		private int totalPredictionsNumber;		
		private int correctPredictionsNumber;
		
		private final Analyzer analyzer;		
		private final Set<String> allowedTags;		
		
		static {
			treeSerializer.enableAdditionalLabels();
		}
		
		public FocusClassifierTestFold(Set<String> allowedTags, Analyzer analyzer) {
			if (allowedTags == null)
				throw new NullPointerException("allowedTags is null");
			if (analyzer == null)
				throw new NullPointerException("analyzer is null");
			
			this.analyzer = analyzer;
			this.allowedTags = allowedTags;
		}
		
		public FocusClassifierTestFold generatePredictions(String testFilepath, String modelFilepath) 
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
			
			QuestionWithIdReader questionsReader = new QuestionWithIdReader(testFilepath, "\t");
			
			Iterator<QuestionWithAnnotatedFocus> questionsWithAnnotatedFocus = questionsReader.iterator();
			
			JCas cas = JCasFactory.createJCas();
			
			while (questionsWithAnnotatedFocus.hasNext()) { 
				QuestionWithAnnotatedFocus questionWithAnnotatedFocus = questionsWithAnnotatedFocus.next();
				
				if (questionWithAnnotatedFocus.isImplicit() || 
					 questionWithAnnotatedFocus.getFocusNumber() == 1) {
					
					String focus = questionWithAnnotatedFocus.getFocus();
					
					String question = questionWithAnnotatedFocus.stripFocus();
					
					analyzer.analyze(cas, 
							new SimpleContent(questionWithAnnotatedFocus.getId(), question));
					
					TokenTree tree = RichTree.getConstituencyTree(cas);
					
					List<Pair<String, RichTokenNode>> examples = 
							generateExamples(tree, treeSerializer);
					
					RichTokenNode focusNode = predictFocusNode(classifier, examples);
					
					if (focusNode == null) { 
						nullPredictionsNumber++;
						continue;
					}
					
					//System.out.println("focus: " + focus);
					if (focus.equals(focusNode.getValue())) { 
						correctPredictionsNumber++;
					} else {
						logger.warn(question + " Predicted " + focusNode.getValue() 
								+ ". Was " + focus);
					}
					
					totalPredictionsNumber++;
				}
			}
			
			return this;
		}
		
		public RichTokenNode predictFocusNode(Classifier classifier, List<Pair<String, RichTokenNode>> examples) {
			if (classifier == null)
				throw new NullPointerException("classifier is null");
			if (examples == null)
				throw new NullPointerException("examples is null");
			
			RichTokenNode focusNode = null;
						
			Double maxPrediction = Double.NEGATIVE_INFINITY;
			
			for (Pair<String, RichTokenNode> example : examples) {
				Double prediction = classifier.classify(example.getA());
				
				//String focus = example.getB().getValue();
				// JUST FOR DEBUG
				//System.out.println("example: " + example.getA());
				//System.err.println("focus: \"" + focus + "\", score: " + prediction);

				if (prediction > maxPrediction) {
					maxPrediction = prediction;
					focusNode = example.getB();
				}
			}
			
			return focusNode;
		}
		
		public List<Pair<String, RichTokenNode>> generateExamples(TokenTree tree,
				TreeSerializer ts) {
			if (tree == null)
				throw new NullPointerException("tree is null");
			if (ts == null)
				throw new NullPointerException("ts is null");
			
			List<Pair<String, RichTokenNode>> examples = new ArrayList<>();
			
			for (RichTokenNode node : tree.getTokens()) {
				RichNode posTag = node.getParent();
				
				if (!allowedTags.contains(posTag.getValue())) continue;
				
				posTag.addAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
				
				boolean isFocus = node.getMetadata().containsKey(Commons.QUESTION_FOCUS_KEY);
				
				String label = isFocus ? "+1" : "-1";
				
				String taggedTree = ts.serializeTree(tree, Commons.getParameterList());
				
				String example = "";
				example += label;
				example += "|BT| ";
				example += taggedTree;
				example += " |ET|";
				
				examples.add(new Pair<>(example, node));
				
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
		
	}
	
}
