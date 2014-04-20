package it.unitn.limosine.italian.syntax.constituency;

import static org.junit.Assert.*;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;

public class BerkeleyWrapperTest {
	
	private JCas cas = null;
	
	String sent = "Renault ha una completa gamma di veicoli elettrici.";
	
	private String expectedSentParse = "(ROOT (S (NP (SPN (Renault)))(VXI (VI (ha)))(NP (RS (una))(ADJP (AS (completa)))(SS (gamma))(PX (E (di))(NP (SP (veicoli))(ADJP (AP (elettrici)))))))(XPS (.)))";
	
	@Before
	public void setUp() throws Exception { 
		SimpleContent content = new SimpleContent("test", sent);
		cas = initCas(content);
	}	

	private JCas initCas(SimpleContent content) throws Exception {
		assert content != null;
		
		JCas cas = JCasFactory.createJCas();
		Analyzer analyzer = new Analyzer();
		analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor")));
		analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription("desc/Limosine/BerkeleyITDescriptor")));
		analyzer.analyze(cas, content);		
		
		return cas;
	}

	@Test
	public void testAnnotate() {
		TokenTree constituencyTree = RichTree.getConstituencyTree(cas);
		TreeSerializer ts = new TreeSerializer();
		String serializedConstTree = ts.serializeTree(constituencyTree, RichNode.OUTPUT_PAR_TOKEN);
		
		assertEquals(expectedSentParse, serializedConstTree);
	}

}
