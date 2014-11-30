package qa.qcri.qf.features.providers;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.trees.nodes.RichNode;
import util.Stopwords;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

/**
 * Test for BowProvider class.
 *
 */
public class BowProviderTest {
	
	private JCas cas = null;

	@Before
	public void setUp() throws Exception {
		String str = "This is a testing string.";
		SimpleContent content = new SimpleContent("0", str);
		this.cas = initCas(content);
	}
	
	private JCas initCas(Analyzable content) throws Exception {
		assert content != null;
		
		JCas cas = JCasFactory.createJCas();
		Analyzer analyzer = new Analyzer();
		analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription(BreakIteratorSegmenter.class)));
		analyzer.analyze(cas, content);
		
		return cas;
	}

	@Test
	public void testGetNGrams() {
		BowProvider bowProvider = new BowProviderBuilder()
			.build();
		
		List<String> ngrams = bowProvider.getNGramsFromCas(cas);
		assertArrayEquals(new String[]{ "This", "is", "a", "testing", "string", "." }, ngrams.toArray());
	}
	
	@Test
	public void testGetNGramsFromCasMinNAndMaxNEquals2() {
		BowProvider bowProvider = new BowProviderBuilder()
			.setMinN(2)
			.setMaxN(2)
			.build();
		
		List<String> ngrams = bowProvider.getNGramsFromCas(cas);
		assertArrayEquals(new String[] { "This is", "is a", "a testing", "testing string", "string ." }, ngrams.toArray());
	}
	
	@Test
	public void testGetNgramsFromCasWithStoplist() {
		BowProvider bowProvider = new BowProviderBuilder()
			.setStopwords(new Stopwords(Stopwords.STOPWORD_EN))
			.build();
		
		List<String> ngrams = bowProvider.getNGramsFromCas(cas);
		assertArrayEquals(new String[] { "This", "testing", "string", "." }, ngrams.toArray());	
	}
	
	public void testNGramsFromCasWithStoplistAndTokenParamFrmtList() {
		BowProvider bowProvider = new BowProviderBuilder()
			.setTokenFrmtParamList(RichNode.OUTPUT_PAR_TOKEN_LOWERCASE + "," + RichNode.OUTPUT_PAR_TOKEN)
			.setStopwords(new Stopwords(Stopwords.STOPWORD_EN))
			.build();
		
		List<String> ngrams = bowProvider.getNGramsFromCas(cas);
		assertArrayEquals(new String[] { "testing", "string", "." }, ngrams.toArray());
	}
	
	@Test
	public void testGetTokensWithStoplistWithNEquals2() {
		BowProvider bowProvider = new BowProviderBuilder()
			.setStopwords(new Stopwords(Stopwords.STOPWORD_EN))
			.setMinN(2)
			.setMaxN(2)
			.build();
		
		List<String> ngrams = bowProvider.getNGramsFromCas(cas);
		assertArrayEquals(new String[]{ "This testing", "testing string", "string ." }, ngrams.toArray());
	}
	
	@Test
	public void testGetTokensWithMinNEq1AndMaxNEq2WithStoplist() { 
		BowProvider bowProvider = new BowProviderBuilder()
			.setStopwords(new Stopwords(Stopwords.STOPWORD_EN))
			.setMinN(1)
			.setMaxN(2)
			.build();
		
		List<String> ngrams = bowProvider.getNGramsFromCas(cas);
		assertArrayEquals(new String[]{ "This", "testing", "string", ".", "This testing", "testing string", "string ." }, ngrams.toArray());
	}
	

	/* Shallow test on FeatureSequence string representations. */
	@Test
	public void testGetNGramFeatureSeqFromCas() {
		StringBuilder sb = new StringBuilder();
		sb.append("0: testing (0)\n");
		sb.append("1: string (1)\n");
		sb.append("2: . (2)\n");		
		String fseqStr = sb.toString();
		
		BowProvider bowProvider = new BowProviderBuilder()
			.setTokenFrmtParamList(RichNode.OUTPUT_PAR_TOKEN_LOWERCASE + "," + RichNode.OUTPUT_PAR_TOKEN)
			.setStopwords(new Stopwords(Stopwords.STOPWORD_EN))
			.build();
		
		FeatureSequence fseq = bowProvider.getNGramFeatureSeqFromCas(cas);
		assertEquals(fseqStr, fseq.toString());
	}

	/* Shallow test on FeatureSequence string representations with alphabet alreay provided. */
	@Test
	public void testGetNGramFeatureSeqFromCasWithAlphabet() {
		Alphabet alphabet = new Alphabet();
		alphabet.lookupIndex("this"); // 0: not present
		alphabet.lookupIndex("is");   // 1: not present
		alphabet.lookupIndex("a"); 	  // 2: not present
		alphabet.lookupIndex("testing"); // 3: present
		alphabet.lookupIndex("string");  // 4: present
		alphabet.lookupIndex("."); 		 // 5: present
		
		StringBuilder sb = new StringBuilder();
		sb.append("0: testing (3)\n");  // feature with id=3 in position 0
		sb.append("1: string (4)\n");	// feature with id=4 in position 1
		sb.append("2: . (5)\n");	// feature with id=5 in position 2
		String fseqStr = sb.toString();
		
		BowProvider bowProvider = new BowProviderBuilder()
			.setTokenFrmtParamList(RichNode.OUTPUT_PAR_TOKEN_LOWERCASE + "," + RichNode.OUTPUT_PAR_TOKEN)		
			.setStopwords(new Stopwords(Stopwords.STOPWORD_EN))
			.setAlphabet(alphabet)
			.build();		
		
		FeatureSequence fseq = bowProvider.getNGramFeatureSeqFromCas(cas);
		assertEquals(fseqStr,  fseq.toString());		
	}

}
