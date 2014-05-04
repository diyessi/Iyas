package qa.qcri.qf.treemarker;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import junit.framework.Assert;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.Test;
import org.uimafit.util.JCasUtil;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.annotators.QuestionClassifier;
import qa.qcri.qf.annotators.QuestionFocusClassifier;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.type.QuestionClass;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class MarkerTest {
	
	private static JCas cas;

	@BeforeClass
	public static void setUp() throws UIMAException {
		Analyzer ae = new Analyzer(new UIMAFilePersistence("CASes/test"));
		
		AnalysisEngine breakIteratorSegmenter = AnalysisEngineFactory.createEngine(
				createEngineDescription(BreakIteratorSegmenter.class));
		
		AnalysisEngine stanfordParser = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordParser.class));
		
		AnalysisEngine illinoisChunker = AnalysisEngineFactory.createEngine(
				createEngineDescription(IllinoisChunker.class));
		
		AnalysisEngine stanfordNamedEntityRecognizer = AnalysisEngineFactory
				.createEngine(createEngineDescription(StanfordNamedEntityRecognizer.class,
						StanfordNamedEntityRecognizer.PARAM_LANGUAGE, "en",
						StanfordNamedEntityRecognizer.PARAM_VARIANT, "muc.7class.distsim.crf"));

		AnalysisEngine questionClassifier = AnalysisEngineFactory
				.createEngine(createEngineDescription(QuestionClassifier.class,
						QuestionClassifier.PARAM_LANGUAGE, "en",
						QuestionClassifier.PARAM_MODELS_DIRPATH, "data/question-classifier_en/models-ptk"));

		AnalysisEngine questionFocusClassifier = AnalysisEngineFactory
				.createEngine(createEngineDescription(QuestionFocusClassifier.class,
						QuestionFocusClassifier.PARAM_LANGUAGE, "en",
						QuestionFocusClassifier.PARAM_MODEL_PATH, "data/question-focus_en/svm.model"));

		ae.addAE(breakIteratorSegmenter)
			.addAE(stanfordParser)
			.addAE(illinoisChunker)
			.addAE(stanfordNamedEntityRecognizer)
			.addAE(questionFocusClassifier)
			.addAE(questionClassifier);

		Analyzable content = new SimpleContent("marker-test",
				"What United States President had dreamed that he was assassinated");

		cas = JCasFactory.createJCas();

		ae.analyze(cas, content);
	}

	@Test
	public void testFocusMarker() throws UIMAException {
		
		TokenTree tree = RichTree.getPosChunkTree(cas);
		
		Marker.markFocus(cas, tree);
		
		TreeSerializer ts = new TreeSerializer().enableAdditionalLabels();
		
		String expectedOutput = "(ROOT (S (NP-FOCUS (WDT (What))(NNP (United))(NNPS (States))"
				+ "(NN (President)))(VP (VBD (had))(VBN (dreamed)))(SBAR (IN (that)))"
				+ "(NP (PRP (he)))(VP (VBD (was))(VBN (assassinated)))))";
		
		Assert.assertEquals(expectedOutput, ts.serializeTree(tree));
	}
	
	@Test
	public void testTypedFocusMarker() throws UIMAException {
		
		TokenTree tree = RichTree.getPosChunkTree(cas);
		
		QuestionClass questionClass = JCasUtil.selectSingle(cas, QuestionClass.class);
		
		Marker.markFocus(cas, tree, questionClass);
		
		TreeSerializer ts = new TreeSerializer().enableAdditionalLabels();
		
		String expectedOutput = "(ROOT (S (NP-FOCUS-HUM (WDT (What))(NNP (United))"
				+ "(NNPS (States))(NN (President)))(VP (VBD (had))"
				+ "(VBN (dreamed)))(SBAR (IN (that)))(NP (PRP (he)))(VP (VBD (was))(VBN (assassinated)))))";
		
		Assert.assertEquals(expectedOutput, ts.serializeTree(tree));
	}
	
	@Test
	public void testNamedEntitiesMarker() throws UIMAException {
		
		TokenTree tree = RichTree.getPosChunkTree(cas);
		
		Marker.markNamedEntities(cas, tree, "");
		
		TreeSerializer ts = new TreeSerializer().enableAdditionalLabels();
		
		String expectedOutput = "(ROOT (S (NP-LOCATION (WDT (What))(NNP-LOCATION (United))"
				+ "(NNPS-LOCATION (States))(NN (President)))(VP (VBD (had))(VBN (dreamed)))"
				+ "(SBAR (IN (that)))(NP (PRP (he)))(VP (VBD (was))(VBN (assassinated)))))";
		
		Assert.assertEquals(expectedOutput, ts.serializeTree(tree));
	}
	
}
