package qa.qcri.qf.trees.pruning;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.pruning.strategies.PruneNodeWithoutLabel;
import qa.qcri.qf.trees.pruning.strategies.PruneNodeWithoutMetadata;
import qa.qcri.qf.trees.pruning.strategies.PrunePosNotStartingWith;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class PrunerTest {

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

		ae.addAE(breakIteratorSegmenter)
			.addAE(stanfordParser)
			.addAE(illinoisChunker);

		Analyzable content = new SimpleContent("pruner-test",
				"The apple is on the table and Barack Obama is the president of United States of America.");

		cas = JCasFactory.createJCas();

		ae.analyze(cas, content);
	}

	@Test
	public void testNotNounsPruner() throws UIMAException {
		
		String expectedOutput = "(ROOT (S (NP (NN (apple)))(NP (NN (table)))(NP (NNP (Barack))(NNP (Obama)))"
				+ "(NP (NN (president)))(NP (NNP (United))(NNPS (States)))(NP (NNP (America)))))";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });
		
		TreeSerializer ts = new TreeSerializer();
		
		RichNode posChunkTree = RichTree.getPosChunkTree(cas);
		
		new PosChunkPruner(-1).prune(posChunkTree,
				new PrunePosNotStartingWith("N"));
		
		String output = ts.serializeTree(posChunkTree, lemma);
	
		Assert.assertTrue(output.equals(expectedOutput));
	}
	
	@Test
	public void testNotNounsPrunerWithRadiusZero() throws UIMAException {
		
		String expectedOutput = "(ROOT (S (NP (NN (apple)))(NP (NN (table)))(NP (NNP (Barack))(NNP (Obama)))"
				+ "(NP (NN (president)))(NP (NNP (United))(NNPS (States)))(NP (NNP (America)))))";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });
		
		TreeSerializer ts = new TreeSerializer();
		
		RichNode posChunkTree = RichTree.getPosChunkTree(cas);
		
		new PosChunkPruner(0).prune(posChunkTree,
				new PrunePosNotStartingWith("N"));
		
		String output = ts.serializeTree(posChunkTree, lemma);
	
		Assert.assertTrue(output.equals(expectedOutput));
	}
	
	@Test
	public void testNotProperNounsPrunerWithRadiusTwo() throws UIMAException {
		
		String expectedOutput = "(ROOT (S (NP (DT (the))(NN (table)))(NP (NNP (Barack))(NNP (Obama)))"
				+ "(VP (VBZ (be)))(NP (DT (the))(NN (president)))(PP (IN (of)))(NP (NNP (United))"
				+ "(NNPS (States)))(PP (IN (of)))(NP (NNP (America)))))";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });
		
		TreeSerializer ts = new TreeSerializer();
		
		RichNode posChunkTree = RichTree.getPosChunkTree(cas);
		
		new PosChunkPruner(2).prune(posChunkTree,
				new PrunePosNotStartingWith("NNP"));
		
		String output = ts.serializeTree(posChunkTree, lemma);
	
		Assert.assertTrue(output.equals(expectedOutput));
	}
	
	@Test
	public void testNodeWithoutRelLabelPruner() throws UIMAException {
		
		String expectedOutput = "(ROOT (S (NP (DT (the))(NN (table)))(NP (NNP-NAME (Barack))"
				+ "(NNP-NAME (Obama)))(VP (VBZ (be)))(NP (DT (the)))))";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });
		
		TreeSerializer ts = new TreeSerializer();
		
		TokenTree posChunkTree = RichTree.getPosChunkTree(cas);
		
		posChunkTree.getTokens().get(6).getParent().addAdditionalLabel("NAME");
		posChunkTree.getTokens().get(7).getParent().addAdditionalLabel("NAME");
		
		new PosChunkPruner(2).prune(posChunkTree,
				new PruneNodeWithoutLabel("NAME"));
		
		String output = ts.enableAdditionalLabels()
				.serializeTree(posChunkTree, lemma);
	
		Assert.assertTrue(output.equals(expectedOutput));
	}
	
	@Test
	public void testNodeWithoutMetadataPruner() throws UIMAException {
		
		String expectedOutput = "(ROOT (S (NP (DT (the))(NN (table)))(NP-REL (NNP-REL (Barack))"
				+ "(NNP-REL (Obama)))(VP (VBZ (be)))(NP (DT (the)))))";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });
		
		TreeSerializer ts = new TreeSerializer();
		
		TokenTree posChunkTree = RichTree.getPosChunkTree(cas);
		
		posChunkTree.getTokens().get(6).getParent().getParent()
			.getMetadata().put(RichNode.REL_KEY, RichNode.REL_KEY);
		posChunkTree.getTokens().get(6).getParent()
			.getMetadata().put(RichNode.REL_KEY, RichNode.REL_KEY);
		posChunkTree.getTokens().get(7).getParent()
			.getMetadata().put(RichNode.REL_KEY, RichNode.REL_KEY);
		
		new PosChunkPruner(2).prune(posChunkTree,
				new PruneNodeWithoutMetadata(RichNode.REL_KEY));
		
		String output = ts.enableRelationalTags()
				.serializeTree(posChunkTree, lemma);
	
		Assert.assertTrue(output.equals(expectedOutput));
	}


}
