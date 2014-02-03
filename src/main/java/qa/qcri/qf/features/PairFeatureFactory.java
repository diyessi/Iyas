package qa.qcri.qf.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;

import qa.qcri.qf.features.representation.PosChunkTreeRepresentation;
import qa.qcri.qf.features.representation.Representation;
import qa.qcri.qf.features.representation.TokenRepresentation;
import qa.qcri.qf.features.similarity.PTKSimilarity;
import util.Pair;

import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.similarity.algorithms.api.TermSimilarityMeasure;
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
	
	private List<Pair<TermSimilarityMeasure, Representation>> measuresOnStrings;
	private List<Pair<TextSimilarityMeasure, Representation>> measuresOnLists;
	
	public PairFeatureFactory() {
		
	}
	
	public void setupMeasures(JCas aCas, JCas bCas, String parameterList) {
		
		Representation tokens = new TokenRepresentation(aCas, bCas, parameterList);
		Representation trees = new PosChunkTreeRepresentation(aCas, bCas, parameterList);
		
		this.measuresOnStrings = new ArrayList<>();
		this.measuresOnLists = new ArrayList<>();
		
		this.addMeasureOnStrings(new CosineSimilarity(), tokens);
		
		/**
		 * DKPpro 2012 STS best system features
		 * 
		 * String features
		 */
		this.addMeasureOnStrings(new GreedyStringTiling(3), tokens);
		this.addMeasureOnStrings(new LongestCommonSubsequenceComparator(), tokens);
		this.addMeasureOnStrings(new LongestCommonSubsequenceNormComparator(), tokens);
		this.addMeasureOnStrings(new LongestCommonSubstringComparator(), tokens);
		/**
		 * n-grams
		 */
		this.addMeasureOnLists(new WordNGramJaccardMeasure(1), tokens);
		this.addMeasureOnLists(new WordNGramJaccardMeasure(2), tokens); // It should be stopword filtered
		this.addMeasureOnLists(new WordNGramJaccardMeasure(3), tokens);
		this.addMeasureOnLists(new WordNGramJaccardMeasure(4), tokens); // It should be stopword filtered
		
		this.addMeasureOnLists(new WordNGramContainmentMeasure(1), tokens); // It should be stopword filtered
		this.addMeasureOnLists(new WordNGramContainmentMeasure(2), tokens); // It should be stopword filtered
		
		this.addMeasureOnStrings(new PTKSimilarity(), trees);
		
		/**
		Map<String, Double> idfValues;
		this.addPair(new CharacterNGramMeasure(2, idfValues);
		this.addPair(new CharacterNGramMeasure(3, idfValues);
		this.addPair(new CharacterNGramMeasure(4, idfValues);
		
		ESA_Wiktionary
		ESA_WordNet
		
		**/
	}

	public PairFeatures getPairFeatures(JCas aCas, JCas bCas, String parameterList) {
		this.setupMeasures(aCas, bCas, parameterList);
		
		PairFeatures pf = new PairFeatures();
		
		for(Pair<TermSimilarityMeasure, Representation> measure : this.measuresOnStrings) {
			pf.computeFeature(measure.getA(),
					measure.getB().getRepresentation().getA(),
					measure.getB().getRepresentation().getB());
		}
		
		for(Pair<TextSimilarityMeasure, Representation> measure : this.measuresOnLists) {
			pf.computeFeature(measure.getA(),
					Lists.newArrayList(measure.getB().getRepresentation().getA().split(" ")),
					Lists.newArrayList(measure.getB().getRepresentation().getB().split(" ")));
		}
		
		return pf;
	}
	
	private void addMeasureOnStrings(TermSimilarityMeasure measure, Representation representation) {
		this.measuresOnStrings.add(
				new Pair<TermSimilarityMeasure, Representation>(measure, representation)
				);
	}
	
	private void addMeasureOnLists(TextSimilarityMeasure measure, Representation representation) {
		this.measuresOnLists.add(
				new Pair<TextSimilarityMeasure, Representation>(measure, representation)
				);
	}
	
	
	
}
