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

	private Map<TextSimilarityMeasure, Double> measureToValue;

	private List<String> featureVocabulary;

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

		this.featureVocabulary = new ArrayList<>();
	}

	/**
	 * Computes the given TextSimilarityMeasure and store the values
	 * 
	 * @param measure
	 * @return the PairFeatures class instance for chaining
	 */
	public PairFeatures computeFeature(TextSimilarityMeasure measure) {

		this.featureVocabulary.add(measure.getName());

		try {
			double similarity = measure.getSimilarity(this.aRepr, this.bRepr);
			this.measureToValue.put(measure, similarity);
		} catch (SimilarityException e) {
			this.measureToValue.put(measure, 0.0);

			logger.error("ERROR: cannot compute feature " + measure.getName()
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
		Collections.sort(this.featureVocabulary);

		Map<String, Integer> featureToIndex = new HashMap<>();
		for (int i = 0; i < this.featureVocabulary.size(); i++) {
			featureToIndex.put(this.featureVocabulary.get(i), i);
		}

		List<Pair<Integer, Double>> indexedFeatures = new ArrayList<>();

		for (TextSimilarityMeasure measure : this.measureToValue.keySet()) {
			Double featureValue = this.measureToValue.get(measure);

			/**
			 * We do not add features not computed due to errors and features
			 * with value equal to zero
			 */
			if (featureValue != null && featureValue.compareTo(0.0) != 0) {
				indexedFeatures.add(new Pair<Integer, Double>(featureToIndex
						.get(measure.getName()), featureValue));
			}
		}

		Collections.sort(indexedFeatures, Collections.reverseOrder(
				new PairCompareOnA<Integer, Double>()));

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
