package qa.qcri.qf.trees;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.List;

import junit.framework.Assert;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.Test;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import qa.qcri.qf.type.QuestionFocus;
import util.Pair;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class TokenSelectorTest {

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
		
		AnalysisEngine stanfordNamedEntityRecognizer = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordNamedEntityRecognizer.class));

		ae.addAE(breakIteratorSegmenter)
			.addAE(stanfordParser)
			.addAE(illinoisChunker)
			.addAE(stanfordNamedEntityRecognizer);

		Analyzable content = new SimpleContent(
				"token-selector-test",
				"The BBC's Pumza Fihlani in court says Mr Dixon's testimony challenges the "
				+ "state's version that Ms Steenkamp would have had time to scream after "
				+ "the first bullet and that Mr Pistorius then changed aim and continued firing.");

		cas = JCasFactory.createJCas();

		ae.analyze(cas, content);
	}

	@Test
	public void testNamedEntities() {
		TokenTree tree = RichTree.getPosChunkTree(cas);

		List<Pair<NamedEntity, List<RichTokenNode>>> annsAndTokens = TokenSelector
				.selectTokenNodeCovered(cas, tree, NamedEntity.class);

		for (Pair<NamedEntity, List<RichTokenNode>> annAndTokens : annsAndTokens) {
			String neText = annAndTokens.getA().getCoveredText();
			
			Assert.assertEquals(neText, Joiner.on(" ").join(annAndTokens.getB()));
		}

	}
	
	@Test
	public void testAnAbsentAnnotation() {
		TokenTree tree = RichTree.getPosChunkTree(cas);

		List<Pair<QuestionFocus, List<RichTokenNode>>> annsAndTokens = TokenSelector
				.selectTokenNodeCovered(cas, tree, QuestionFocus.class);

		Assert.assertEquals(true, annsAndTokens.isEmpty());
	}

}
