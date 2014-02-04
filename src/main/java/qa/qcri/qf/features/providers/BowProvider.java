package qa.qcri.qf.features.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.trees.nodes.RichTokenNode;
import qa.qcri.qf.trees.nodes.TokenTextGetterFactory;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.NGramStringIterable;
import svmlighttk.SVMVector;

public class BowProvider {	

	private final static int DEFAULT_MIN_N = 1;
	private final static int DEFAULT_MAX_N = 1;
	private static final String DEFAULT_TOKEN_TEXT_TYPE = TokenTextGetterFactory.LEMMA;
	
	private Alphabet featureDict;
	private int minN;
	private int maxN;
	private String parametersList;
	
	public BowProvider() {
		this(DEFAULT_TOKEN_TEXT_TYPE, DEFAULT_MIN_N, DEFAULT_MAX_N);
	}
	
	public BowProvider(String parametersList) {
		this(parametersList, DEFAULT_MIN_N, DEFAULT_MAX_N);
	}
	
	public BowProvider(String parametersList, int minN, int maxN) {
		this.minN = minN;
		this.maxN = maxN;
		this.featureDict = new Alphabet();
		this.parametersList = parametersList;
		//this.tokenTextGetter = TokenTextGetterFactory.getTokenTextGetter(tokenTextType);
	}
	
	public SVMVector getSVMVector(JCas cas, Map<String, Double> idf) {
		SVMVector vec = new SVMVector(getFeatureVector(cas));
		return vec;
	}
	
	public FeatureVector getFeatureVector(JCas cas) {
		FeatureVector fv = new FeatureVector(getNGramFeatureSeqFromCas(cas));
		return fv;
	}
	
	public FeatureSequence getNGramFeatureSeqFromCas(JCas cas) {
		FeatureSequence featureSeq = new FeatureSequence(featureDict);
		
		List<String> tokens = new ArrayList<>();
		for (Token token : JCasUtil.select(cas, Token.class)) {
			String text = new RichTokenNode(token).getRepresentation(parametersList);
			if (text != null) {
				tokens.add(text);
			}
		}
		NGramStringIterable ngramsStringIt = new NGramStringIterable(tokens, minN, maxN);
		for (String ngram : ngramsStringIt) { 
			featureSeq.add(ngram);			
		}		
		return featureSeq;
	}

}
