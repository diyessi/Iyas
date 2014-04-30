package qa.qcri.qf.pipeline;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.Test;

import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class ConstituencyTreeTest {

	@Test
	public void testConstituencyTree() throws UIMAException {
		
		String expectedOutput = "(ROOT (S (NP (PRP$ (My))(NN (dog)))(ADVP (RB (also)))"
				+ "(VP (VBZ (likes))(VP (VBG (eating))(NP (NN (sausage)))))(. (.)))"
				+ "(FRAG (WHNP (WHNP (WP (What)))(PP (IN (about))(NP (PRP$ (your))"
				+ "(NN (cat)))))(. (?))))";
		
		Analyzer ae = instantiateAnalyzer();
		JCas cas = JCasFactory.createJCas();

		ae.analyze(cas, new SimpleContent("constituency-test",
				"My dog also likes eating sausage. What about your cat?"));
		
		TokenTree tree = RichTree.getConstituencyTree(cas);
		TreeSerializer ts = new TreeSerializer();

		Assert.assertEquals(expectedOutput,
				ts.serializeTree(tree, ""));
	}

	private Analyzer instantiateAnalyzer() throws UIMAException {
		Analyzer ae = new Analyzer(new UIMAFilePersistence("CASes/test/"));
		
		AnalysisEngine stanfordSegmenter = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordSegmenter.class));
		
		AnalysisEngine stanfordPosTagger = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordPosTagger.class));
		
		AnalysisEngine stanfordLemmatizer = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordLemmatizer.class));
		
		AnalysisEngine stanfordParser = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordParser.class));

		ae.addAE(stanfordSegmenter)
			.addAE(stanfordPosTagger)
			.addAE(stanfordLemmatizer)
			.addAE(stanfordParser);

		return ae;
	}

}
