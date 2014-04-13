package it.unitn.limosine.italian.syntax.constituency;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.trees.Tree;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;

public class ConstituencyFactoryTest {
	
	private String berkeleyParse = "(TOP (S (NP (SPN Renault))(VXI (VI ha))(NP (RS una)(ADJP (AS completa))(SS gamma)(PX (E di)(NP (SP veicoli)(ADJP (AP elettrici))))))(XPS .))";

	private String expectedConstituencyTree = "(ROOT (S (NP (SPN (Renault)))(VXI (VI (ha)))(NP (RS (una))(ADJP (AS (completa)))(SS (gamma))(PX (E (di))(NP (SP (veicoli))(ADJP (AP (elettrici)))))))(XPS (.)))";
	
	private String doctxt = "Renault ha una completa gamma di veicoli elettrici.";
	
	private String[] toks = {
			"Renault",
			"ha",
			"una",
			"completa",
			"gamma",
			"di",
			"veicoli",
			"elettrici",
			"."
	};
	
	private String[] poss = {
			"SPN",
			"VI",
			"RS",
			"AS",
			"SS",
			"E",
			"SP",
			"AP",
			"XPS"
	};
	
	private JCas cas;
	
	@Before
	public void setUp() throws UIMAException {
		cas = JCasFactory.createJCas();
		cas.setDocumentText(doctxt);
		initTokensAndPOSs(cas, doctxt);
	}

	private void initTokensAndPOSs(JCas cas, String doctxt) { 
		assert cas != null;
		assert doctxt != null;
		
		int begin = 0;
		for (int i = 0; i < toks.length; i++) { 
			String tok = toks[i];
			String p = poss[i];
			begin = doctxt.indexOf(tok);
			Token token = new Token(cas, begin, begin + tok.length());
			POS pos = new POS(cas,  begin, begin + tok.length());
			pos.setPosValue(p);
			token.addToIndexes();
			pos.addToIndexes();
			token.setPos(pos);
		
			begin += tok.length();					
		}		
	}	
	
	@Test
	public void test() {
		/*
		Tree tree = Tree.valueOf(berkeleyParse);
		tree.setSpans();
		
		System.out.println(tree.toString());
		
		List<Token> tokens = new ArrayList<>();
		
		for (Token token : JCasUtil.select(cas, Token.class)) {
			tokens.add(token);
			System.out.println(token.getCoveredText() + "/" + token.getPos().getPosValue());
		}
		*/
		ConstituencyFactory.buildConstituents(cas, berkeleyParse);
		
		/*
		ConstituencyFactory.buildConstituentsFromStanfordTree(cas, tree, tokens);
		for (Constituent cons : JCasUtil.select(cas, Constituent.class)) {
			System.out.println(cons.getConstituentType() + ", " + cons.getBegin() + ", " + cons.getEnd() + ", " + cons.getCoveredText());
		}
		*/
		
		TokenTree constituencyTree = RichTree.getConstituencyTree(cas);
		TreeSerializer ts = new TreeSerializer();
		String serializedTree = ts.serializeTree(constituencyTree, RichNode.OUTPUT_PAR_TOKEN);
		//System.out.println(serializedTree);
		
		assertEquals(expectedConstituencyTree, serializedTree);
	}

}
