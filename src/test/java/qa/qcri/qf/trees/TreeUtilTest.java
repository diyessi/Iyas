package qa.qcri.qf.trees;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.List;

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
import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class TreeUtilTest {

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

		Analyzable content = new SimpleContent("sample-content",
				"The apple is on the table and Barack Obama is the president of United States of America.");

		cas = JCasFactory.createJCas();

		ae.analyze(cas, content);
	}

	@Test
	public void testGetNodes() throws UIMAException {
		
		String expectedOutput = "ROOT S NP DT the NN apple VP VBZ be PP IN on NP DT "
				+ "the NN table NP NNP Barack NNP Obama VP VBZ be NP DT the NN president "
				+ "PP IN of NP NNP United NNPS States PP IN of NP NNP America";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });

		RichNode posChunkTree = RichTree.getPosChunkTree(cas);

		List<RichNode> nodes = TreeUtil.getNodes(posChunkTree);
		
		String output = "";
		
		for(RichNode node : nodes) {
			output += " " + node.getRepresentation(lemma);
		}
		
		Assert.assertTrue(output.trim().equals(expectedOutput));
	}

	@Test
	public void testGetNodesWithFilter() throws UIMAException {
		
		String expectedOutput = "the apple be on the table Barack Obama be the president "
				+ "of United States of America";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });

		RichNode posChunkTree = RichTree.getPosChunkTree(cas);

		List<RichNode> nodes = 	TreeUtil.getNodesWithFilter(posChunkTree, new Function<RichNode, Boolean>() {
			@Override
			public Boolean apply(RichNode input) {
				return input.isLeaf();
			}
		});
		
		String output = "";
		
		for(RichNode node : nodes) {
			output += " " + node.getRepresentation(lemma);
		}
		
		Assert.assertTrue(output.trim().equals(expectedOutput));
	}
	
	@Test
	public void testLeavesParentsChunks() throws UIMAException {
		
		String expectedOutput = "DT NN VBZ IN DT NN NNP NNP VBZ DT NN IN NNP NNPS IN NNP";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });

		RichNode posChunkTree = RichTree.getPosChunkTree(cas);

		List<RichNode> nodes = 	TreeUtil.getLeavesParents(posChunkTree);
		
		String output = "";
		
		for(RichNode node : nodes) {
			output += " " + node.getRepresentation(lemma);
		}
		
		Assert.assertTrue(output.trim().equals(expectedOutput));
	}
	
	@Test
	public void testLeavesGrandParentsChunks() throws UIMAException {
		
		String expectedOutput = "NP VP PP NP NP VP NP PP NP PP NP";

		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });

		RichNode posChunkTree = RichTree.getPosChunkTree(cas);

		List<RichNode> nodes = 	TreeUtil.getLeavesGrandParents(posChunkTree);
		
		String output = "";
		
		for(RichNode node : nodes) {
			output += " " + node.getRepresentation(lemma);
		}
		
		System.out.println(output);
		
		Assert.assertTrue(output.trim().equals(expectedOutput));
	}
	
}
