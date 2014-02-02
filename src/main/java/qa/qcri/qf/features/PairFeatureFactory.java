package qa.qcri.qf.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.WordNGramContainmentMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.WordNGramJaccardMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.GreedyStringTiling;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubsequenceComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubsequenceNormComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubstringComparator;

/**
 * 
 *
 */
public class PairFeatureFactory {
	
	List<TextSimilarityMeasure> measures;
	
	public PairFeatureFactory() {
		this.measures = new ArrayList<>();
		
		this.measures.add(new CosineSimilarity());
		
		/**
		 * DKPpro 2012 STS best system features
		 * 
		 * String features
		 */
		this.measures.add(new GreedyStringTiling(3));
		this.measures.add(new LongestCommonSubsequenceComparator());
		this.measures.add(new LongestCommonSubsequenceNormComparator());
		this.measures.add(new LongestCommonSubstringComparator());
		/**
		 * n-grams
		 */
		this.measures.add(new WordNGramJaccardMeasure(1));
		this.measures.add(new WordNGramJaccardMeasure(2)); // It should be stopword filtered
		this.measures.add(new WordNGramJaccardMeasure(3));
		this.measures.add(new WordNGramJaccardMeasure(4)); // It should be stopword filtered
		
		this.measures.add(new WordNGramContainmentMeasure(1)); // It should be stopword filtered
		this.measures.add(new WordNGramContainmentMeasure(2)); // It should be stopword filtered
		
		/**
		Map<String, Double> idfValues;
		this.measures.add(new CharacterNGramMeasure(2, idfValues);
		this.measures.add(new CharacterNGramMeasure(3, idfValues);
		this.measures.add(new CharacterNGramMeasure(4, idfValues);
		
		ESA_Wiktionary
		ESA_WordNet
		
		**/
		
		
	}

	public PairFeatures getPairFeatures(JCas aCas, JCas bCas, String parameterList) {
		PairFeatures pf = new PairFeatures(aCas, bCas, parameterList);
		
		for(TextSimilarityMeasure measure : measures) {
			pf.computeFeature(measure);
		}
		
		return pf;
	}
	
	
	
}
