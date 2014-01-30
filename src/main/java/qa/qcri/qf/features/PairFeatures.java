package qa.qcri.qf.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qa.qcri.qf.features.cosine.LemmasCosineSimilarity;
import qa.qcri.qf.features.cosine.LowerCaseTokensCosineSimilarity;
import qa.qcri.qf.features.cosine.TokensCosineSimilarity;
import qa.qcri.qf.pipeline.UimaUtil;
import qa.qcri.qf.trees.RichTokenNode;
import util.Pair;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;

/**
 * 
 * Class for computing features related to a pair of objects. Implements some
 * caching for the most used structures extracted from the objects' CASes.
 * 
 * A new feature must implement the PairFeature interface. Then the
 * addFeature(String featureName) method must be augmented to instantiate the
 * feature implementation and associates it to its name.
 * 
 * In the future a way to easily support features requiring additional resources
 * should be implemented
 * 
 * List of features:
 * 
 * from http://code.google.com/p/dkpro-similarity-asl/
 * 
 * TokensCosineSimilarity LemmasCosineSimilarity LowerCaseTokensCosineSimilarity
 * 
 * ...please add other features
 * 
 */

public class PairFeatures {

	private JCas aCas;

	private JCas bCas;

	private List<RichTokenNode> aTokens;

	private List<RichTokenNode> bTokens;

	private Map<String, PairFeature> nameToFeature;

	private Map<String, Double> computedFeatures;

	private List<String> featureVocabulary;

	private final Logger logger = LoggerFactory.getLogger(PairFeatures.class);

	public PairFeatures(JCas aCas, JCas bCas) {
		this.aCas = aCas;
		this.bCas = bCas;
		this.aTokens = UimaUtil.getRichTokens(aCas);
		this.bTokens = UimaUtil.getRichTokens(bCas);

		this.nameToFeature = new HashMap<>();
		this.computedFeatures = new HashMap<>();
		this.featureVocabulary = new ArrayList<>();
	}

	/**
	 * Instantiates the features object with the given name registers them
	 * 
	 * This acts as a factory method which must be augmented every time a new
	 * feature is introduced into the framework
	 * 
	 * @param featureName
	 *            the name of the feature
	 * @return the PairFeatures object instance for chaining
	 */
	public PairFeatures addFeature(String featureName) {

		boolean addFeatureToVocabulary = true;

		switch (featureName) {
		case TokensCosineSimilarity.NAME:
			this.nameToFeature.put(featureName, new TokensCosineSimilarity(
					this.aTokens, this.bTokens));
			break;
		case LemmasCosineSimilarity.NAME:
			this.nameToFeature.put(featureName, new LemmasCosineSimilarity(
					this.aTokens, this.bTokens));
			break;
		case LowerCaseTokensCosineSimilarity.NAME:
			this.nameToFeature.put(featureName,
					new LowerCaseTokensCosineSimilarity(this.aTokens,
							this.bTokens));
			break;
		default:
			addFeatureToVocabulary = false;
			break;
		}

		if (addFeatureToVocabulary) {
			this.featureVocabulary.add(featureName);
		}

		return this;
	}

	/**
	 * Iterates through the instantiated features and computes their values
	 * 
	 * @return the map which associates feature names to their computed value
	 */
	public Map<String, Double> computeFeatures() {
		for (String featureName : this.featureVocabulary) {
			try {
				Double value = this.nameToFeature.get(featureName).getValue();
				this.computedFeatures.put(featureName, value);
			} catch (SimilarityException e) {
				logger.error("ERROR: cannot compute feature " + featureName
						+ "on pairs:\n" + this.aCas.getDocumentText() + "\n"
						+ this.bCas.getDocumentText() + "\n");
			}
		}
		return this.computedFeatures;
	}

	/**
	 * Indexes the features
	 * 
	 * Note: features are indexed starting by 0
	 * 
	 * @return a list of features with associated indexes
	 */
	public List<Pair<Integer, Double>> getIndexedFeatures() {

		/**
		 * If features are not present, it tries to compute them
		 */
		if (this.computedFeatures.isEmpty()) {
			this.computeFeatures();
		}

		/**
		 * Keeps the features vocabulary alphabetically ordered
		 */
		Collections.sort(this.featureVocabulary);

		List<Pair<Integer, Double>> indexedFeatures = new ArrayList<>();

		for (int i = 0; i < this.featureVocabulary.size(); i++) {
			Double featureValue = this.computedFeatures
					.get(this.featureVocabulary.get(i));

			/**
			 * We do not add features not computed due to errors and features
			 * with value equal to zero
			 */
			if (featureValue != null && featureValue.compareTo(0.0) != 0) {
				indexedFeatures.add(new Pair<Integer, Double>(i, featureValue));
			}
		}

		return indexedFeatures;
	}

	/**
	 * Serializes the features in a common format used by most machine learning
	 * tools
	 * 
	 * @param startingIndex
	 *            the value added to the indexed feature
	 * @return a string containing the indexed features <index>:<value>
	 */
	public String serializeIndexedFeatures(int startingIndex) {
		List<Pair<Integer, Double>> indexedFeatures = this.getIndexedFeatures();
		StringBuilder sb = new StringBuilder(1024 * 4);
		boolean first = true;
		for (Pair<Integer, Double> indexedFeature : indexedFeatures) {
			if (first) {
				first = !first;
			} else {
				sb.append(" ");
			}
			sb.append(startingIndex + indexedFeature.getA());
			sb.append(":");
			sb.append(indexedFeature.getB());
		}
		return sb.toString();
	}
}
