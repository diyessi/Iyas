package qa.qcri.qf.tools.questionclassifier;

import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.providers.ConstituencyTreeProvider;
import qa.qcri.qf.trees.providers.TokenTreeProvider;

public class QuestionClassifierTrain {

	public static final String TRAIN_CASES_DIRECTORY = Commons.QF_DIRECTORY + "train-CASes/";

	public static final String TRAIN_QUESTIONS_PATH = Commons.QF_DIRECTORY + "train_5500.label";
	
	public static final String TRAIN_DIRECTORY = Commons.QF_DIRECTORY + "train/";

	public static void main(String[] args) throws UIMAException {		
		Analyzer ae = Commons.instantiateAnalyzer(new UIMAFilePersistence(
				TRAIN_CASES_DIRECTORY));
		
		Set<String> categories = Commons.analyzeAndCollectCategories(TRAIN_QUESTIONS_PATH, ae);
		
		String parameterList = Commons.getParameterList();
		
		FileManager fm = new FileManager();
		
		TokenTreeProvider treeProvider = new ConstituencyTreeProvider();
		
		TreeSerializer ts = new TreeSerializer();
		
		JCas cas = JCasFactory.createJCas();
		
		Iterator<CategoryContent> questions = new QuestionReader(TRAIN_QUESTIONS_PATH).iterator();
		while(questions.hasNext()) {
			CategoryContent question = questions.next();		
			ae.analyze(cas, question);			
			
			String tree = ts.serializeTree(treeProvider.getTree(cas), parameterList);
			
			for(String category : categories) {
				String label = category.equals(question.getCategory()) ? "+1" : "-1";			
				String outputFile = TRAIN_DIRECTORY + category + ".train";
				String example = label + " |BT| " + tree + " |ET|";
				
				fm.writeLn(outputFile, example);
			}
		}
		
		fm.closeFiles();
	}
}
