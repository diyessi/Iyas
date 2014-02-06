package qa.qcri.qf.pipeline;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class Pipeline {

	private static JCas cas;

	@BeforeClass
	public static void setUp() throws UIMAException {
		Analyzer ae = new Analyzer(new UIMAFilePersistence("CASes/test"));

		ae.addAEDesc(createEngineDescription(BreakIteratorSegmenter.class))
				.addAEDesc(createEngineDescription(StanfordParser.class))
				.addAEDesc(createEngineDescription(IllinoisChunker.class));

		Analyzable content = new SimpleContent("sample-content",
				"The apple is on the table.");

		cas = JCasFactory.createJCas();

		ae.analyze(cas, content);
	}

	@Test
	public void testTokenPosChunkTree() throws UIMAException {

		String lowercase = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_TOKEN });

		TreeSerializer ts = new TreeSerializer();
		RichNode posChunkTree = RichTree.getPosChunkTree(cas);

		Assert.assertEquals(
				"(ROOT (S (NP (DT (The))(NN (apple)))(VP (VBZ (is)))(PP (IN (on)))(NP (DT (the))(NN (table)))))",
				ts.serializeTree(posChunkTree, lowercase));
	}

	@Test
	public void testLowercasePosChunkTree() throws UIMAException {

		String lowercase = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });

		TreeSerializer ts = new TreeSerializer();
		RichNode posChunkTree = RichTree.getPosChunkTree(cas);

		Assert.assertEquals(
				"(ROOT (S (NP (DT (the))(NN (apple)))(VP (VBZ (is)))(PP (IN (on)))(NP (DT (the))(NN (table)))))",
				ts.serializeTree(posChunkTree, lowercase));
	}

	@Test
	public void testLemmaPosChunkTree() throws UIMAException {

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });

		TreeSerializer ts = new TreeSerializer();
		RichNode posChunkTree = RichTree.getPosChunkTree(cas);

		/**
		 * Lemmas are lowercased by the annotator
		 */

		Assert.assertEquals(
				"(ROOT (S (NP (DT (the))(NN (apple)))(VP (VBZ (be)))(PP (IN (on)))(NP (DT (the))(NN (table)))))",
				ts.serializeTree(posChunkTree, lemma));
	}

	@Test
	public void testLemmaLowercasePosChunkTree() throws UIMAException {

		String lemmaLowercase = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA,
						RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });

		TreeSerializer ts = new TreeSerializer();
		RichNode posChunkTree = RichTree.getPosChunkTree(cas);

		Assert.assertEquals(
				"(ROOT (S (NP (DT (the))(NN (apple)))(VP (VBZ (be)))(PP (IN (on)))(NP (DT (the))(NN (table)))))",
				ts.serializeTree(posChunkTree, lemmaLowercase));
	}

}