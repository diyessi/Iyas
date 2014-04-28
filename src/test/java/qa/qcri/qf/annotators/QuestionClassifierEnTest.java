package qa.qcri.qf.annotators;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import junit.framework.Assert;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.Test;

import qa.qcri.qf.annotators.QuestionClassifier;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.tools.questionclassifier.Commons;
import qa.qcri.qf.type.QuestionClass;

public class QuestionClassifierEnTest {

	private static JCas cas;
	
	@BeforeClass
	public static void setUp() throws UIMAException {
		
		Analyzer analyzer = Commons.instantiateQuestionClassifierAnalyzer("en");
		analyzer.setPersistence(new UIMAFilePersistence("CASes/test_en"));
		
		Analyzable content = new SimpleContent("question-classifier-test",
				"Where is the Tornado Tower?");

		cas = JCasFactory.createJCas();

		analyzer.analyze(cas, content);
		
		SimplePipeline.runPipeline(cas, createEngineDescription(
				QuestionClassifier.class,
				QuestionClassifier.PARAM_LANGUAGE, "en",
				QuestionClassifier.PARAM_MODELS_DIRPATH, "data/question-classifier/models-ptk_en"));
	}

	@Test
	public void testQuestionClass() throws UIMAException {
		String questionClass = JCasUtil.selectSingle(cas, QuestionClass.class)
				.getQuestionClass();

		Assert.assertEquals("LOC", questionClass);
	}
}
