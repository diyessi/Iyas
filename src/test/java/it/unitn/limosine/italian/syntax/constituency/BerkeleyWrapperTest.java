package it.unitn.limosine.italian.syntax.constituency;

import static org.junit.Assert.*;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;

public class BerkeleyWrapperTest {
	
	private JCas cas = null;
	
	String msg = "Renault ha una completa gamma di veicoli elettrici.";
	private String expectedParse = "(ROOT (S (NP (SPN (Renault)))(VXI (VI (ha)))(NP (RS (una))(ADJP (AS (completa)))(SS (gamma))(PX (E (di))(NP (SP (veicoli))(ADJP (AP (elettrici)))))))(XPS (.)))";
	
	@Before
	public void setUp() throws Exception { 
		SimpleContent content = new SimpleContent("test", msg);
		cas = initCas(content);
	}	

	private JCas initCas(SimpleContent content) throws Exception {
		assert content != null;
		
		JCas cas = JCasFactory.createJCas();
		Analyzer analyzer = new Analyzer();
		analyzer.addAEDesc(createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor"));
		analyzer.addAEDesc(createEngineDescription("desc/Limosine/BerkeleyITDescriptor"));
		analyzer.analyze(cas, content);		
		
		return cas;
	}

	@Test
	public void testAnnotate() {
		TokenTree constituencyTree = RichTree.getConstituencyTree(cas);
		TreeSerializer ts = new TreeSerializer();
		String serializedConstTree = ts.serializeTree(constituencyTree, RichNode.OUTPUT_PAR_TOKEN);
		
		//System.out.println("expected: " + expectedParse);
		//System.out.println("serialzd: " + serializedConstTree);
		assertEquals(expectedParse, serializedConstTree);
	}

}
