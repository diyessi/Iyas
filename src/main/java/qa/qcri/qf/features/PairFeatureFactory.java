package qa.qcri.qf.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;

import qa.qcri.qf.features.representation.PosChunkTreeRepresentation;
import qa.qcri.qf.features.representation.Representation;
import qa.qcri.qf.features.representation.TokenRepresentation;
import qa.qcri.qf.features.similarity.PTKSimilarity;
import qa.qcri.qf.features.similarity.adaptor.MeasureAdaptor;
import qa.qcri.qf.features.similarity.adaptor.TermMeasureAdaptor;
import qa.qcri.qf.features.similarity.adaptor.TextMeasureAdaptor;
import util.Pair;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;
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
	 * is necessary to distinguish them and provide them with the right input.
	 * The MeasureAdaptor wraps different measures and hides the underlying
	 * implementation
	 */
	private List<Pair<MeasureAdaptor, Representation>> measures;

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
		this.measures = new ArrayList<>();

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
		this.addTermMeasure(new GreedyStringTiling(3), tokens);
		this.addTermMeasure(new LongestCommonSubsequenceComparator(), tokens);
		this.addTermMeasure(new LongestCommonSubsequenceNormComparator(), tokens);
		this.addTermMeasure(new LongestCommonSubstringComparator(), tokens);
		/**
		 * n-grams
		 */
		this.addTextMeasure(new WordNGramJaccardMeasure(1), tokens);
		this.addTextMeasure(new WordNGramJaccardMeasure(2), tokens);
		this.addTextMeasure(new WordNGramJaccardMeasure(3), tokens);
		this.addTextMeasure(new WordNGramJaccardMeasure(4), tokens);
		this.addTextMeasure(new WordNGramContainmentMeasure(1), tokens);
		this.addTextMeasure(new WordNGramContainmentMeasure(2), tokens);

		/**
		 * Map<String, Double> idfValues; this.addPair(new
		 * CharacterNGramMeasure(2, idfValues); this.addPair(new
		 * CharacterNGramMeasure(3, idfValues); this.addPair(new
		 * CharacterNGramMeasure(4, idfValues);
		 * 
		 * ESA_Wiktionary ESA_WordNet
		 **/

		/**
		 * Additional DKPro features
		 */
		this.addTermMeasure(new CosineSimilarity(), tokens);

		/**
		 * iKernels features
		 */
		this.addTermMeasure(new PTKSimilarity(), trees);
	}

	public FeatureVector getPairFeatures(JCas aCas, JCas bCas,
			String parameterList) {
		this.setupMeasures(aCas, bCas, parameterList);

		AugmentableFeatureVector fv = new AugmentableFeatureVector(
				this.alphabet);

		for (Pair<MeasureAdaptor, Representation> measureAndRepresentation : this.measures) {
			MeasureAdaptor measure = measureAndRepresentation.getA();
			Representation representation = measureAndRepresentation.getB();
			Pair<String, String> representations = representation
					.getRepresentation(aCas, bCas);

			String featureName = measure.getName(representation);
			double featureValue = measure.getSimilarity(representations);

			fv.add(featureName, featureValue);
		}

		return fv;
	}

	private void addTextMeasure(TextSimilarityMeasure measure,
			Representation representation) {
		this.measures.add(new Pair<MeasureAdaptor, Representation>(
				new TextMeasureAdaptor(measure), representation));
	}

	private void addTermMeasure(TermSimilarityMeasure measure,
			Representation representation) {
		this.measures.add(new Pair<MeasureAdaptor, Representation>(
				new TermMeasureAdaptor(measure), representation));
	}

}
