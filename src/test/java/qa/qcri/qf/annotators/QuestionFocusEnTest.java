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

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.type.QuestionFocus;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class QuestionFocusEnTest {
	private static JCas cas;

	@BeforeClass
	public static void setUp() throws UIMAException {
		Analyzer ae = new Analyzer(new UIMAFilePersistence("CASes/test"));

		ae.addAEDesc(createEngineDescription(BreakIteratorSegmenter.class))
				.addAEDesc(createEngineDescription(StanfordParser.class))
				.addAEDesc(createEngineDescription(QuestionFocusClassifier.class,
						QuestionFocusClassifier.PARAM_LANGUAGE, "en",
						QuestionFocusClassifier.PARAM_MODEL_PATH, "data/question-focus_en/svm.model"));

		Analyzable content = new SimpleContent("question-focus-test",
				"What United States President had dreamed that he was assassinated");

		cas = JCasFactory.createJCas();

		ae.analyze(cas, content);
	}

	@Test
	public void testQuestionClass() throws UIMAException {
		Token questionFocus = JCasUtil.selectSingle(cas, QuestionFocus.class)
				.getFocus();

		Assert.assertEquals("President", questionFocus.getCoveredText());
	}
}
