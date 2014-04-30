package it.unitn.limosine;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import it.unitn.limosine.types.syntax.CoNLL2008DependencyTree;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.uimafit.util.JCasUtil;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;

public class AnnotatorTest {
	
	public final static String TEXT = "Mr. Barack Obama is in New York. He is working at the White"
			+ " House on the new health plan.";

	@Test
	public void testLimosine() throws UIMAException, IOException {
		
		new File("CASes/test/limosine-test").delete();
		
		Analyzer ae = new Analyzer(new UIMAFilePersistence("CASes/test"));
		
		ae.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription("desc/Limosine/pipelines/FullSeMod")));
		
		JCas cas = JCasFactory.createJCas();
		
		Analyzable content = new SimpleContent("limosine-test", TEXT);
		
		try {
			ae.analyze(cas, content);
			
			for(CoNLL2008DependencyTree tree : JCasUtil.select(cas, CoNLL2008DependencyTree.class)) {
				System.out.println(tree.getRawParse());
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	
	}

	/*
	 * .addTypeSystemForCas("desc/PipelineTypeSystem.xml")
	 * .addTypeSystemForCas("desc/types/CorefTypeSystem.xml")
	 * .addTypeSystemForCas("desc/types/EMDTypeSystem.xml")
	 * .addTypeSystemForCas("desc/types/LinkingTypeSystem.xml")
	 * .addTypeSystemForCas("desc/types/NERTypeSystem.xml")
	 * .addTypeSystemForCas("desc/types/OpinionMiningTypeSystem.xml")
	 * .addTypeSystemForCas("desc/types/ParseTreePosTypeSystem.xml")
	 * .addTypeSystemForCas("desc/types/RelationExtractionTypeSystem.xml")
	 * .addTypeSystemForCas("desc/types/SentenceTokenTypeSystem.xml")
	 */

	/**
	public static String getDependenciesFromRawParse(JCas cas) {
		SentenceTreeMerger dependencyTreeMerger = new SentenceTreeMerger("ROOT");
		
		for(CoNLL2008DependencyTree depTree : JCasUtil.select(cas, CoNLL2008DependencyTree.class)) {
			String rawParse = depTree.getRawParse();
			
			List<String[]> nodesInfo = new ArrayList<>();
			for(String nodeInfo : rawParse.split("\n")) {
				nodesInfo.add(nodeInfo.split("\t"));
			}
			
			Tree rootNode = null;
			
			Map<String, Tree> idToNode = new HashMap<>();
			for(String[] nodeInfo : nodesInfo) {
				String lexical = nodeInfo[1].replace("(", "[").replace(")", "]");
				idToNode.put(nodeInfo[0], TreeUtil.createNode(lexical));
				if(nodeInfo[8].equals("0")) {
					rootNode = idToNode.get(nodeInfo[0]);
				}
			}
			
			for(String[] nodeInfo : nodesInfo) {
				String label = nodeInfo[9];
				String nodeId = nodeInfo[0];
				String headId = nodeInfo[8];
				
				if(!headId.equals("0")) {
					Tree headNode = idToNode.get(headId);
					Tree childNode = idToNode.get(nodeId);
					Tree rel = TreeUtil.createNode(label);
					rel.addChild(childNode);
					headNode.addChild(rel);
				}
			}
			
			Tree tree = TreeUtil.createNode("ROOT-0");
			tree.addChild(rootNode);
			
			dependencyTreeMerger.addTree(TreeUtil.serializeTree(tree));
		}
		
		return dependencyTreeMerger.getMergedTree();
	}
	**/
}
