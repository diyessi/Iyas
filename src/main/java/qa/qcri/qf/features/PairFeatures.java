package qa.qcri.qf.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qa.qcri.qf.pipeline.UimaUtil;
import qa.qcri.qf.trees.RichTokenNode;
import util.Pair;
import util.PairCompareOnA;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;

/**
 * 
 * Class for computing features related to a pair of objects. Implements some
 * caching for the most used structures extracted from the objects' CASes.
 * 
 * Features implement the DKPro TextSimilarityMeasure interface
 * 
 * In the future a way to easily support features requiring additional resources
 * should be implemented
 * 
 * List of features:
 * 
 * from http://code.google.com/p/dkpro-similarity-asl/
 * 
 * TokensCosineSimilarity LemmasCosineSimilarity LowerCaseTokensCosineSimilarity
 * Word1GramJaccardMeasure Word2GramJaccardMeasure Word3GramJaccardMeasure
 * 
 * ...please add other features
 * 
 */

public class PairFeatures {

	private JCas aCas;

	private JCas bCas;

	private List<RichTokenNode> aTokens;

	private List<RichTokenNode> bTokens;

	private List<String> aRepr;

	private List<String> bRepr;

	private Map<String, Double> measureToValue;

	private Map<String, Integer> featureVocabulary;

	private final Logger logger = LoggerFactory.getLogger(PairFeatures.class);

	public PairFeatures(JCas aCas, JCas bCas, String parameterList) {
		this.aCas = aCas;
		this.bCas = bCas;

		this.aTokens = UimaUtil.getRichTokens(aCas);
		this.bTokens = UimaUtil.getRichTokens(bCas);

		this.aRepr = UimaUtil.getRichTokensRepresentation(this.aTokens,
				parameterList);
		this.bRepr = UimaUtil.getRichTokensRepresentation(this.bTokens,
				parameterList);

		this.measureToValue = new HashMap<>();

		this.featureVocabulary = new HashMap<>();
	}

	/**
	 * Computes the given TextSimilarityMeasure and store the values
	 * 
	 * @param measure
	 * @return the PairFeatures class instance for chaining
	 */
	public PairFeatures computeFeature(TextSimilarityMeasure measure) {

		String measureName = measure.getName();

		if (this.featureVocabulary.containsKey(measureName)) {
			logger.warn(measureName + " is already in the feature vocabulary");
		}

		this.featureVocabulary.put(measureName, this.featureVocabulary.size());

		try {
			double similarity = measure.getSimilarity(this.aRepr, this.bRepr);
			this.measureToValue.put(measureName, similarity);
		} catch (SimilarityException e) {
			this.measureToValue.put(measureName, 0.0);

			logger.error("ERROR: cannot compute feature " + measureName
					+ "on pairs:\n" + this.aCas.getDocumentText() + "\n"
					+ this.bCas.getDocumentText() + "\n");
		}

		return this;
	}

	/**
	 * Indexes the features and returns them in order in ascending indexes
	 * 
	 * Note: features are indexed starting by 0
	 * 
	 * @return a list of features with associated indexes
	 */
	public List<Pair<Integer, Double>> getIndexedFeatures() {
		/**
		 * Keeps the features vocabulary alphabetically ordered
		 */
		
		List<Pair<Integer, Double>> features = new ArrayList<>();
		
		for(String featureName : this.featureVocabulary.keySet()) {
			
			Double featureValue = this.measureToValue.get(featureName);
			Integer featureIndex = this.featureVocabulary.get(featureName);
			
			/**
			 * We do not add features not computed due to errors and features
			 * with value equal to zero
			 */
			if (featureValue != null && featureValue.compareTo(0.0) != 0) {
				features.add(new Pair<Integer, Double>(featureIndex, featureValue));
			}
		}
		
		Collections.sort(features, Collections.reverseOrder(new PairCompareOnA<Integer, Double>()));

		return features;
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
