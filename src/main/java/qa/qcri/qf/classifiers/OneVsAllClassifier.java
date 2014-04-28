package qa.qcri.qf.classifiers;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * The class can instantiate new models with their associated
 * identifier/category and add them in a pool of classifiers.
 * 
 * An instance can be classified using the classifiers in the pool, and the
 * returned category is the one yielding the highest confidence value.
 */
public class OneVsAllClassifier {

	private Map<String, Classifier> models;
	private ClassifierFactory factory;

	public OneVsAllClassifier(ClassifierFactory factory) {
		this.models = new HashMap<>();
		this.factory = factory;
	}

	/**
	 * Adds a model in the pool. The id of the classifier is the id returned on
	 * question classification if the classifier yields the highest confidence
	 * 
	 * @param id
	 *            the id of the classifier
	 * @param path
	 *            the path of the classifier model
	 */
	public void addModel(String id, String path) {
		Classifier model = this.factory.createClassifier(path);
		this.models.put(id, model);
	}

	/**
	 * Performs the One-vs-All classification
	 * 
	 * @param instance
	 *            the instance to classify. This is an example of its format
	 *            "|BT| (ROOT (SBARQ (WHNP (WHADJP (WRB How)(JJ many))(NNS people))(SQ (VP (VBP live)(PP (IN in)(NP (NNP Chile)))))(. ?))) |ET|"
	 * @return the id of the classifier giving the highest confidence
	 */
	public String getMostConfidentModel(String instance) {
		String cat = "";
		double currentConfidence = Double.NEGATIVE_INFINITY;

		for (String id : this.models.keySet()) {
			Classifier svm = this.models.get(id);
			double confidence = svm.classify(instance);
			
			// Just for DEBUG
			// System.err.println("classifier: \"" + id + "\", score: " + confidence);
			if (confidence > currentConfidence) {
				currentConfidence = confidence;
				cat = id;
			}
		}

		return cat;
	}
}
