package qa.qcri.qf.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;

import qa.qcri.qf.features.representation.PosChunkTreeRepresentation;
import qa.qcri.qf.features.representation.Representation;
import qa.qcri.qf.features.representation.TokenRepresentation;
import qa.qcri.qf.features.similarity.PTKSimilarity;
import util.Pair;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;

import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
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
 * This class provides the features to compute on pair of objects
 */
public class PairFeatureFactory {

	/**
	 * Mallet Alphabet is used to index features
	 */
	private Alphabet alphabet;

	/**
	 * Some DKPro features works only if provided with list of tokens Thus, it
	 * is necessary to distinguish them and provide them with the right input
	 */
	private List<Pair<TermSimilarityMeasure, Representation>> measuresOnStrings;
	private List<Pair<TextSimilarityMeasure, Representation>> measuresOnLists;

	/**
	 * Instantiates the feature factory with a feature index that should be
	 * shared among the modules working on the same datasets
	 * 
	 * @param alphabet
	 *            the Mallet alphabet object.
	 */
	public PairFeatureFactory(Alphabet alphabet) {
		this.alphabet = alphabet;
	}

	public void setupMeasures(JCas aCas, JCas bCas, String parameterList) {
		this.measuresOnStrings = new ArrayList<>();
		this.measuresOnLists = new ArrayList<>();

		/**
		 * Prepares the token and tree representations we want to use for
		 * computing the features (e.g. we would like to compute cosine
		 * similarity between stems and lemmas
		 */
		Representation tokens = new TokenRepresentation(parameterList);
		Representation trees = new PosChunkTreeRepresentation(parameterList);

		/**
		 * DKPro 2012 STS best system features
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
		this.addMeasureOnLists(new WordNGramJaccardMeasure(2), tokens);
		this.addMeasureOnLists(new WordNGramJaccardMeasure(3), tokens);
		this.addMeasureOnLists(new WordNGramJaccardMeasure(4), tokens);
		this.addMeasureOnLists(new WordNGramContainmentMeasure(1), tokens);
		this.addMeasureOnLists(new WordNGramContainmentMeasure(2), tokens);

		/**
		 * Map<String, Double> idfValues;
		 * this.addPair(new CharacterNGramMeasure(2, idfValues);
		 * this.addPair(new CharacterNGramMeasure(3, idfValues);
		 * this.addPair(new CharacterNGramMeasure(4, idfValues);
		 * 
		 * ESA_Wiktionary ESA_WordNet
		 **/
		
		/**
		 * Additional DKPro features
		 */
		this.addMeasureOnStrings(new CosineSimilarity(), tokens);

		/**
		 * iKernels features
		 */
		this.addMeasureOnStrings(new PTKSimilarity(), trees);
	}

	public FeatureVector getPairFeatures(JCas aCas, JCas bCas,
			String parameterList) {
		this.setupMeasures(aCas, bCas, parameterList);
		
		AugmentableFeatureVector fv = new AugmentableFeatureVector(this.alphabet);

		for (Pair<TermSimilarityMeasure, Representation> measureAndRepresentation : this.measuresOnStrings) {
			TermSimilarityMeasure measure = measureAndRepresentation.getA();
			Representation representation = measureAndRepresentation.getB();
			Pair<String, String> representations = representation.getRepresentation(aCas, bCas);
			
			String featureName = measure.getName() + "_" + representation.getName();
			try {
				double featureValue = measure.getSimilarity(representations.getA(), representations.getB());
				fv.add(featureName, featureValue);
			} catch (SimilarityException e) {
				fv.add(featureName, 0.0);
				e.printStackTrace();
			}
		}

		for (Pair<TextSimilarityMeasure, Representation> measureAndRepresentation : this.measuresOnLists) {
			TextSimilarityMeasure measure = measureAndRepresentation.getA();
			Representation representation = measureAndRepresentation.getB();
			Pair<String, String> representations = representation.getRepresentation(aCas, bCas);
			
			String featureName = measure.getName() + "_" + representation.getName();
			try {
				double featureValue = measure.getSimilarity(
						Lists.newArrayList(representations.getA().split(" ")),
						Lists.newArrayList(representations.getB().split(" ")));
				fv.add(featureName, featureValue);
			} catch (SimilarityException e) {
				fv.add(featureName, 0.0);
				e.printStackTrace();
			}
		}

		return fv;
	}

	private void addMeasureOnStrings(TermSimilarityMeasure measure,
			Representation representation) {
		this.measuresOnStrings.add(new Pair<TermSimilarityMeasure,
				Representation>(measure, representation));
	}

	private void addMeasureOnLists(TextSimilarityMeasure measure,
			Representation representation) {
		this.measuresOnLists.add(new Pair<TextSimilarityMeasure,
				Representation>(measure, representation));
	}

}
