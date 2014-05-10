package qa.qcri.qf.tools.questionclassifier;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistenceWithEncoding;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.providers.ConstituencyTreeProvider;
import qa.qcri.qf.trees.providers.TokenTreeProvider;

public class QuestionClassifierItTrain {
	
	public static final String TRAIN_CASES_DIRECTORY = "CASes/question-classifier/train.it/";
	
	public static final String TRAIN_QUESTIONS_PATH = Commons.QF_DIRECTORY + "qc-it_train10.txt";
	
	//public static final String TRAIN_QUESTIONS_PATH = Commons.QF_DIRECTORY + "train_1229.it.label";

	
	public static final String TRAIN_DIRECTORY = Commons.QF_DIRECTORY + "train.it/";
	
	public static void main(String[] args) throws UIMAException, IOException {
		// Check that the questions file does exist.
		File questionsFile = new File(TRAIN_QUESTIONS_PATH);
		if (!questionsFile.isFile()) { 
			System.out.println("File '" + questionsFile.getAbsolutePath() + "' does not exist or is not readable, please check the path.");
			System.exit(1);
		}
		
		Analyzer ae = Commons.instantiateQuestionClassifierAnalyzer("it");
		//ae.setPersistence(new UIMAFilePersistenceWithEncoding(TRAIN_CASES_DIRECTORY, "UTF-8"));
		ae.setPersistence(new UIMAFilePersistence(TRAIN_CASES_DIRECTORY));
				
		Set<String> categories = Commons.analyzeAndCollectCategories(TRAIN_QUESTIONS_PATH, ae);
		
		System.out.printf("Found (%d) categories: %s", categories.size(), categories);
		
		String parameterList = Commons.getParameterList();
		
		FileManager fm = new FileManager();
		
		TokenTreeProvider treeProvider = new ConstituencyTreeProvider();
		
		TreeSerializer ts = new TreeSerializer();
		
		JCas cas = JCasFactory.createJCas();
		
		Iterator<CategoryContent> questions = new QuestionReader(TRAIN_QUESTIONS_PATH).iterator();
		while (questions.hasNext()) { 
			CategoryContent question = questions.next();
			System.out.println("doctxt: " + question.getContent());
			ae.analyze(cas, question);
			
			String tree = ts.serializeTree(treeProvider.getTree(cas), parameterList);
			
			System.out.println("tree: " + tree);
			
			for (String category : categories) {
				String label = category.equals(question.getCategory()) ? "+1" : "-1";
				String outputFile = TRAIN_DIRECTORY + category + ".train.it";
				String example = label + " |BT| " + tree + " |ET|";
				
				fm.writeLn(outputFile, example);
			}					
		}
		
		fm.closeFiles();		
	}
}
