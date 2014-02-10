package qa.qcri.qf.features.providers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.NGramStringIterable;
import de.tudarmstadt.ukp.dkpro.core.stopwordremover.StopWordSet;
import edu.berkeley.nlp.util.Logger;

/**
 * Generates n-grams FeatureVectors or FeatureSequences.
 * 
 */
public class BowProvider {
	
	final static int MIN_N = 1;
	
	final static int MAX_N = 1;
	
	final static boolean FILTER_STOPWORDS = false;
	
	final static String STOPWORDS_FILEPATH = "tools/stoplist/en.txt";
	
	final static String TOKEN_FORMAT_PARAM_LIST = RichNode.OUTPUT_PAR_TOKEN;
	
	private final int minN;
	
	private final int maxN;
	
	private Alphabet alphabet;
	
	private final String tokenFrmtParamList;
	
	private final boolean filterStopwords;

	private final StopWordSet stopwordSet;
	
	/**
	 * The de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.StopWord class has a bug:
	 * it does not proper init its inner structer when the 0-arg constructor is called.
	 * So we need a var which records if the 0-arg constructor has been called.
	 */
	private boolean emptyStoplist = false;
	
	/**
	 * Constructs a new BowPrivder instance
	 * 
	 * @param alphabet The underlying alphabet to use for features repr
	 * @param tokensFrmtParamList A string holding the tokens formatting rules
	 * @param minN The smallest ngrams size to keep track
	 * @param maxN The largest ngrams size to keep track
	 * @param stoplistFile The stopwords list filepath
	 * @param filterStopwords True if stopwords removal should be removed
	 */
	BowProvider(Alphabet alphabet, String tokensFrmtParamList, int minN, int maxN, String stoplistFile, boolean filterStopwords) {
		if (alphabet == null) {
			throw new NullPointerException("alphabet is null");
		}
		if (tokensFrmtParamList == null) {
			throw new NullPointerException("parametersList is null");
		}
		if (minN > maxN) {
			throw new IllegalArgumentException("minN > maxN");
		}
		if (stoplistFile == null) {
			throw new NullPointerException("stoplistFile is null");
		}
		
		this.minN = minN;
		this.maxN = maxN;
		this.alphabet = alphabet;
		this.tokenFrmtParamList = tokensFrmtParamList;
		this.filterStopwords = filterStopwords;		
		
		StopWordSet stoplist = null;
		
		if (filterStopwords) {
			try {
				String stoplistAbsFile = new File(stoplistFile).getAbsolutePath();
				stoplist = new StopWordSet(
					new String[]{ stoplistAbsFile });
			} catch (IOException e) {
				Logger.warn("Error while reading stopwords file: " + stoplistFile);
				stoplist = new StopWordSet();
				emptyStoplist = true;
			}
		}
		
		// if filterStopwords is false, create an empty stopwordSet
		if (stoplist == null) {
			stoplist = new StopWordSet();
			emptyStoplist = true;
		}
		this.stopwordSet = stoplist;
	}
	
	/**
	 * Returns a vector of n-gram features from a cas.
	 *  
	 * @param cas A uima cas object
	 * @return The n-grams FeatureVector object
	 */
	public FeatureVector getFeatureVector(JCas cas) {
		if (cas == null) {
			throw new NullPointerException("cas is null");
		}
		
		return
				new FeatureVector(
						getNGramFeatureSeqFromCas(cas));
	}
	
	private List<String> getNGrams(List<String> tokens) { 
		assert tokens != null;
		
		List<String> ngrams = new ArrayList<>();
		for (String ngram : new NGramStringIterable(
				tokens.toArray(new String[0]), minN, maxN)) {
			ngrams.add(ngram);
		}
		return ngrams;		
	}
	
	public List<String> getNGramsFromCas(JCas cas) {
		if (cas == null) {
			throw new NullPointerException("cas is null");
		}
		
		List<Token> tokens = new ArrayList<>();
		for (Token token : JCasUtil.select(cas, Token.class)) {
			tokens.add(token);			
		}
		List<String> filteredFrmtTokens = filterAndFrmtTokens(tokens);
		return getNGrams(filteredFrmtTokens);
	}
	
	private List<String> filterAndFrmtTokens(List<Token> tokens) {
		assert tokens != null;
				
		List<String> filteredFrmtTokens = new ArrayList<>();
		for (Token token : tokens) {
			String text = new RichTokenNode(token).getRepresentation(tokenFrmtParamList);
			
			if (text != null &&
				(!filterStopwords || emptyStoplist || !stopwordSet.contains(text)))
				filteredFrmtTokens.add(text);
		}
		return filteredFrmtTokens;
	}
	
	private FeatureSequence getNGramFeatureSeq(List<String> ngrams) {
		assert ngrams != null;
		
		FeatureSequence featureSeq = new FeatureSequence(alphabet);
		for (String ngram : ngrams) { 
			featureSeq.add(ngram);
		}
		return featureSeq;
	}
	
	/**
	 * Construct a a new n-grams feature sequence from a cas.
	 * 
	 * @param cas A uima cas object
	 * @return The n-grams FeatureSequence object
	 */
	public FeatureSequence getNGramFeatureSeqFromCas(JCas cas) {
		if (cas == null) {
			throw new NullPointerException("cas is null");
		}		
		// Create a new sequence of features
		List<String> ngrams = getNGramsFromCas(cas);
		return getNGramFeatureSeq(ngrams);
	}

}
