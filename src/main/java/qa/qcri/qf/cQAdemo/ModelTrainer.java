package qa.qcri.qf.cQAdemo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.uniroma2.sag.kelp.data.dataset.SimpleDataset;
import it.uniroma2.sag.kelp.data.example.Example;
import it.uniroma2.sag.kelp.data.example.SimpleExample;
import it.uniroma2.sag.kelp.data.label.Label;
import it.uniroma2.sag.kelp.data.label.StringLabel;
import it.uniroma2.sag.kelp.learningalgorithm.classification.liblinear.LibLinearLearningAlgorithm;
import it.uniroma2.sag.kelp.predictionfunction.classifier.BinaryLinearClassifier;
import it.uniroma2.sag.kelp.predictionfunction.classifier.BinaryMarginClassifierOutput;
import it.uniroma2.sag.kelp.utils.JacksonSerializerWrapper;
import it.uniroma2.sag.kelp.utils.ObjectSerializer;
import it.uniroma2.sag.kelp.utils.evaluation.BinaryClassificationEvaluator;

/**
 * 
 * 
 * @author gmartino
 *
 */

public class ModelTrainer {

	private static final String TRAIN_FILENAME = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-train.xml.csv.klp";
	private static final String TEST_FILENAME = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-test.xml.csv.klp";
	private static final String MODEL_FILE_NAME = TRAIN_FILENAME + ".model";
	
	private static final String VECTORIAL_LINEARIZATION_NAME = "semevalfeatures";
	private static final String POSITIVE_CLASS_NAME = "Good";
	private static final float CP = 1;
	private static final float CN = 1;
	private BinaryLinearClassifier model;
	private DenseVectorFromListOfDouble fv;
	private StringLabel positiveClass;
	
	public ModelTrainer() {
		this.model = null;
		this.fv = new DenseVectorFromListOfDouble();
	}

	public String getModelFileName() {
		return MODEL_FILE_NAME;
	}
		
	/**
	 * Saves a model in Kelp format to file MODEL_FILENAME 
	 * @throws IOException
	 */
	public void saveModelToFile() throws IOException {
		if (this.model==null) {
			System.out.println("ERROR: model is null, thus " + 
					"it cannot be saved on file");
			System.exit(1);
		}
		ObjectSerializer serializer = new JacksonSerializerWrapper();
		serializer.writeValueOnFile(model, MODEL_FILE_NAME);
	}
	

	/**
	 * Loads a model in Kelp format from file MODEL_FILENAME.    
	 * @return true whether the model has been successfully loaded, false otherwise  
	 */
	public boolean loadModelFromFile(String modelFileName) {
		ObjectSerializer serializer = new JacksonSerializerWrapper();
		File file = new File(modelFileName);
		try {
			this.model = serializer.readValue(file, BinaryLinearClassifier.class);
		}catch (Exception e) {
			return false;
		}
		return true;
	}

	private String getRepresentationNameFromModel() {
		return "semevalfeatures";
	}
	
	/**
	 * Performs binary classification learning on TRAIN_FILENAME dataset. 
	 * Learning uses liblinear and it is influenced by CP and CN (weight
	 * for positive and negative examples, respectively).
	 * The model is then saved to file (see saveModelToFile())   
	 * @throws Exception
	 */
	private void trainSystem(String trainFileName) throws Exception {

		SimpleDataset trainingSet = new SimpleDataset();
		trainingSet.populate(trainFileName);
		for (Label l : trainingSet.getClassificationLabels()) {
			System.out.println("Training Label " + l.toString() + " "
					+ trainingSet.getNumberOfPositiveExamples(l));
			System.out.println("Training Label " + l.toString() + " "
					+ trainingSet.getNumberOfNegativeExamples(l));
		}
		LibLinearLearningAlgorithm liblinear = 
				new LibLinearLearningAlgorithm(positiveClass, CP,CN,
						VECTORIAL_LINEARIZATION_NAME);
		liblinear.learn(trainingSet);
		model = (BinaryLinearClassifier) liblinear.getPredictionFunction();
		saveModelToFile(); 
	}
	
	/**
	 * Classifies a test set using current model (which is loaded if necessary). 
	 * @return the classification accuracy
	 * @throws Exception
	 */
	private float classifyTestSet(String modelFileName, String testFileName, 
			StringLabel positiveClass) throws Exception {
		
		SimpleDataset testSet = new SimpleDataset();
		if (model==null) {
			loadModelFromFile(modelFileName);
		}
		testSet.populate(testFileName);
		BinaryClassificationEvaluator evaluator = 
				new BinaryClassificationEvaluator(positiveClass);
		for (Example e : testSet.getExamples()) {
			BinaryMarginClassifierOutput predict = model.predict(e);
			evaluator.addCount(e, predict);
		}
		return evaluator.getAccuracy();
	}

	
	public float getExampleScoreFromFeatureVector(ArrayList<Double> featureValues) {
		
		SimpleExample ex = new SimpleExample();
		float score;
		ex.addRepresentation(getRepresentationNameFromModel(), 
				fv.createKelpDenseVectorFromArray(featureValues));
		//ex = fv.createKelpExampleFromVector(featureValues);
		return model.predict(ex).getScore(positiveClass);
		
	}
	
	public static void main(String[] args) throws Exception {

		ModelTrainer trainer = new ModelTrainer();
		trainer.positiveClass = new StringLabel(POSITIVE_CLASS_NAME);
		System.out.println("Training system..."); //add more info
		trainer.trainSystem(TRAIN_FILENAME);
		
	}

}

