package qa.qcri.qf.features.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.NGramStringIterable;
import svmlighttk.SVMVector;

public class BowProvider {	

	private final static int DEFAULT_MIN_N = 1;
	private final static int DEFAULT_MAX_N = 1;
	private final static String DEFAULT_PARAMETERS_LIST = RichNode.OUTPUT_PAR_TOKEN;
	
	private int minN;
	private int maxN;
	private Alphabet alphabet;	// Store features dict
	private String parametersList;
	
	public BowProvider() {
		this(DEFAULT_MIN_N, DEFAULT_MAX_N);
	}
	
	public BowProvider(int minN, int maxN) {
		this(DEFAULT_PARAMETERS_LIST, minN, maxN);
	}
	
	public BowProvider(String parametersList, int minN, int maxN) {
		this(new Alphabet(), parametersList, DEFAULT_MIN_N, DEFAULT_MAX_N);
	}
	
	public BowProvider(Alphabet alphabet, String parametersList, int minN, int maxN) {
		this.minN = minN;
		this.maxN = maxN;
		this.alphabet = alphabet;
		this.parametersList = parametersList;
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
		FeatureSequence featureSeq = new FeatureSequence(alphabet);
		
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
