package qa.qcri.qf.features;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;

/**
 * 
 * Interface implemented by classes computing similarities between two objects
 * 
 */
public interface PairFeature {

	/**
	 * 
	 * @return the name of the feature. If a feature can be instantiated with
	 *         different parameters, the name should contain them, in order to
	 *         identify the features from the same class but with varying
	 *         configuration
	 */
	public String getName();

	/**
	 * 
	 * @return the feature value
	 * @throws SimilarityException
	 *             it is an exception which may be thrown by DKPro Similarity
	 *             objects
	 */
	public double getValue() throws SimilarityException;

}
