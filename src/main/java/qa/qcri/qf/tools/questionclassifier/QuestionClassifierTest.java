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

public class QuestionClassifierTest {
	
	public static final String TEST_CASES_DIRECTORY = Commons.QF_DIRECTORY + "test-CASes/";

	public static final String TEST_QUESTIONS_PATH = Commons.QF_DIRECTORY + "TREC_10.label";

	public static void main(String[] args) throws UIMAException {		
		Analyzer ae = Commons.instantiateAnalyzer(new UIMAFilePersistence(
				TEST_CASES_DIRECTORY));
		
		Set<String> categories = Commons.analyzeAndCollectCategories(TEST_QUESTIONS_PATH, ae);
		
		String parameterList = Commons.getParameterList();
		
		FileManager fm = new FileManager();
		
		TokenTreeProvider treeProvider = new ConstituencyTreeProvider();
		
		TreeSerializer ts = new TreeSerializer();
		
		JCas cas = JCasFactory.createJCas();
		
		Iterator<CategoryContent> questions = new QuestionReader(TEST_QUESTIONS_PATH).iterator();
		while(questions.hasNext()) {
			CategoryContent question = questions.next();		
			ae.analyze(cas, question);			
			
			String tree = ts.serializeTree(treeProvider.getTree(cas), parameterList);
			String example = "|BT| " + tree + " |ET|";
			
			for(String category : categories) {
				System.out.println("Classify for " + category + ": " + example);
			}
		}
		
		fm.closeFiles();
	}
}
