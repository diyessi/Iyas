package qa.qcri.qf.chunker;

import static org.junit.Assert.*;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.Before;
import org.junit.Test;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.trec.AnalyzerFactory;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;

public class ConstituencyTreeChunkerTest {
	
	private JCas jcas = null;

	@Before
	public void setUp() throws Exception {
		jcas = JCasFactory.createJCas();
		jcas.setDocumentText("x y z");
		
		Sentence sent = new Sentence(jcas, 0, 5);
		sent.addToIndexes();
		
		Token tokenA = new Token(jcas, 0, 1);
		tokenA.addToIndexes();
		POS pos = new POS(jcas, 0, 1);
		pos.setPosValue("A");
		tokenA.setPos(pos);
		pos.addToIndexes();
		
		Token tokenB = new Token(jcas, 2, 3);
		tokenB.addToIndexes();
		pos = new POS(jcas, 2, 3);
		pos.setPosValue("B");
		tokenB.setPos(pos);
		pos.addToIndexes();
		
		Token tokenC = new Token(jcas, 4, 5);
		tokenC.addToIndexes();
		pos = new POS(jcas, 4, 5);
		pos.setPosValue("C");
		tokenC.setPos(pos);
		pos.addToIndexes();
		
		newConstituent(jcas, 0, 5, "ROOT", newFSArray(jcas,						
				newConstituent(jcas, 0, 5, "NP", newFSArray(jcas,
						tokenA, 
						newConstituent(jcas, 2, 5, "NP", newFSArray(jcas,
								tokenB,
								tokenC
		))))));		
	}
	
	private FSArray newFSArray(JCas jcas, FeatureStructure... fs) {
		FSArray arr = new FSArray(jcas, fs.length);
		for (int i = 0; i < fs.length; i++) {
			arr.set(i, fs[i]);
		}
		return arr;
	}
	
	private Constituent newConstituent(JCas jcas, int begin, int end, String conType, FSArray children) { 
		Constituent con = new Constituent(jcas, begin, end);
		con.setConstituentType(conType);
		con.setChildren(children);
		
		con.addToIndexes();
		return con;
		
	}
	
	@Test
	public void testSomeSents() throws UIMAException {
		String[] sents = {
			/*
			"Qual è il nome della rara malattia neurologica con sintomi quali : movimenti involontari - LRB - tic - RRB - ,  bestemmie, e vocalizzazioni incoerenti - LRB - grugniti, grida, etc. - RRB - ?",
			"19 Chi era il leader della setta davidiana affrontata dall' FBI a Waco , in Texas nel 1993 ?",
			"The Iron Lady è un film diretto da Phyllida Lloyd, che racconta la vita dell'ex primo ministro britannico Margaret Thatcher, interpretata da Meryl Streep, che per la sua interpretazione ha ricevuto il suo terzo Oscar.",
			"La figura politica e umana di Margaret Thatcher ha trovato una vastissima rappresentazione nel mondo culturale. In una scena del film italiano della coppia Bud Spencer & Terence Hill, Non c'è due senza quattro, del 1984, si fa riferimento a lei allo scopo di fare della sottile ironia sulla sua figura politica.film1984 Film del 1984 Nel 2009 la BBC ha trasmesso il film Margaret, che racconta la parte finale della sua carriera politica, in cui la Thatcher è interpretata da Lindsay Duncan. Nel 2011 è stato girato un film a lei ispirato intitolato The Iron Lady diretto da Phyllida Lloyd, nel quale è stata impersonata da Meryl Streep che, grazie a questa interpretazione, il 26 febbraio 2012 ha ricevuto il suo terzo premio Oscar."
			*/
			"Desmond Tutu riceve il Premio Nobel per la pace",
			"6. La valle dei pini 9.1",
			"A Nord-Est del paese si erge il Monte Giano (1820 m). Da diversi chilometri di distanza si può notare sul monte la scritta \"DVX\" (duce, dal latino dux, ducis), composta da alberi di pino. La pineta, di circa otto ettari e di 20.000 pini, fu realizzata dalla Scuola Allievi Guardie Forestali di Cittaducale nel 1939, con il contributo in braccia di numerosi giovani del posto, come omaggio a Benito Mussolini. La scritta, visibile nelle giornate di poca foschia anche da Roma, è patrimonio artistico e monumento naturale unico in Italia e nel mondo ed è stata recentemente restaurata con i fondi regionali nell'estate del 2004."
		};
		
		Analyzer analyzer = AnalyzerFactory.newTrecPipeline("it", new UIMANoPersistence());
		for (String sent : sents) { 
			jcas.reset();
			jcas.setDocumentText(sent);
			analyzer.analyze(jcas, new SimpleContent("Xxx", sent));
			TokenTree tree = RichTree.getPosChunkTree(jcas);
			System.out.println(new TreeSerializer().useSquareBrackets().serializeTree(tree));
		}
	}

	@Test
	public void test() throws UIMAException {
		for (Token token : JCasUtil.select(jcas, Token.class)) 
			System.out.println("(" + token.getPos().getPosValue() + " " + token.getCoveredText() + ")");
	
		TokenTree conTree = RichTree.getConstituencyTree(jcas);
		System.out.println(new TreeSerializer().serializeTree(conTree));
		
		AnalysisEngine simpleChunker = createEngine(ConstituencyTreeChunker.class);
		simpleChunker.process(jcas);
		
		for (Chunk chunk : JCasUtil.select(jcas, Chunk.class)) { 
			System.out.println("(" + chunk.getChunkValue() + " " + chunk.getCoveredText() + ")");
		}		
		
		TokenTree posChunkTree = RichTree.getPosChunkTree(jcas);
		System.out.println(new TreeSerializer().serializeTree(posChunkTree));
		assertEquals(new TreeSerializer().serializeTree(posChunkTree), "(ROOT (S (A (A (x)))(NP (B (y))(C (z)))))");
	}

}
