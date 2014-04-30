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

public class QuestionClassifierItTest {

	private static JCas cas;
	
	@BeforeClass
	public static void setUp() throws UIMAException {
		
		Analyzer analyzer = Commons.instantiateQuestionClassifierAnalyzer("it");
		analyzer.setPersistence(new UIMAFilePersistence("CASes/test_it"));
		
		Analyzable content = new SimpleContent("question-classifier-test",
				"Dov'è la Torre Tornado ?");

		cas = JCasFactory.createJCas();

		analyzer.analyze(cas, content);		

		/*
		SimplePipeline.runPipeline(cas,
				createEngineDescription(QuestionClassifier.class));
		*/
		
		SimplePipeline.runPipeline(cas, createEngineDescription(
				QuestionClassifier.class,
				QuestionClassifier.PARAM_LANGUAGE, "it",
				QuestionClassifier.PARAM_MODELS_DIRPATH, "data/question-classifier/models-ptk_it"));
	}

	@Test
	public void testQuestionClass() throws UIMAException {
		String questionClass = JCasUtil.selectSingle(cas, QuestionClass.class)
				.getQuestionClass();

		Assert.assertEquals("LOCATION", questionClass);
	}
}
