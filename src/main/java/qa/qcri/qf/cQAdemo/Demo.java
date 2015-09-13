package qa.qcri.qf.cQAdemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import qa.qcri.qf.emnlp2015.CommentSelectionDatasetCreator;
import qa.qcri.qf.semeval2015_3.textnormalization.JsoupUtils;
import qa.qcri.qf.semeval2015_3.textnormalization.UserProfile;
import qa.qcri.qf.semeval2015_3.Question;
import it.uniroma2.sag.kelp.data.dataset.SimpleDataset;
import it.uniroma2.sag.kelp.data.example.Example;
import it.uniroma2.sag.kelp.data.example.SimpleExample;
import it.uniroma2.sag.kelp.data.label.Label;
import it.uniroma2.sag.kelp.data.label.StringLabel;
import it.uniroma2.sag.kelp.data.representation.vector.DenseVector;
import it.uniroma2.sag.kelp.data.representation.vector.SparseVector;
import it.uniroma2.sag.kelp.learningalgorithm.classification.liblinear.LibLinearLearningAlgorithm;
import it.uniroma2.sag.kelp.predictionfunction.classifier.BinaryLinearClassifier;
import it.uniroma2.sag.kelp.predictionfunction.classifier.BinaryMarginClassifierOutput;
import it.uniroma2.sag.kelp.predictionfunction.classifier.Classifier;
import it.uniroma2.sag.kelp.utils.JacksonSerializerWrapper;
import it.uniroma2.sag.kelp.utils.ObjectSerializer;
import it.uniroma2.sag.kelp.utils.evaluation.BinaryClassificationEvaluator;


public class Demo {

	private static final boolean TRAIN_SYSTEM = false;
	private static final boolean COMPUTE_FEATURE_REPRESENTATION = false;
	private static final String TRAIN_FILENAME = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-train.xml.csv.klp";
	private static final String TEST_FILENAME = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-test.xml.csv.klp";
	private static final String MODEL_FILENAME = TRAIN_FILENAME + ".model";
	private static final String VECTORIAL_LINEARIZATION_NAME = "semevalfeatures";
	private static final float CP = 1;
	private static final float CN = 1;
	private StringLabel positiveClass = new StringLabel("GOOD");
	private BinaryLinearClassifier model;
	private CommentSelectionDatasetCreator featureMapper; 
	
	public Demo() {
		this.model = null;
		this.featureMapper = new CommentSelectionDatasetCreator();
	}

	/**
	 * Performs binary classification learning on TRAIN_FILENAME dataset. 
	 * Learning uses liblinear and it is influenced by CP and CN (weight
	 * for positive and negative examples, respectively).
	 * The model is then saved to file (see saveModelToFile())   
	 * @throws Exception
	 */
	private void trainSystem() throws Exception {
		
		if (COMPUTE_FEATURE_REPRESENTATION) {
			this.featureMapper.runForEnglish();
		}
		SimpleDataset trainingSet = new SimpleDataset();
		trainingSet.populate(TRAIN_FILENAME);
		for (Label l : trainingSet.getClassificationLabels()) {
			System.out.println("Training Label " + l.toString() + " "
					+ trainingSet.getNumberOfPositiveExamples(l));
			System.out.println("Training Label " + l.toString() + " "
					+ trainingSet.getNumberOfNegativeExamples(l));
		}
		LibLinearLearningAlgorithm liblinear = new LibLinearLearningAlgorithm(positiveClass,
																CP,CN,VECTORIAL_LINEARIZATION_NAME);
		liblinear.learn(trainingSet);
		this.model = (BinaryLinearClassifier) liblinear.getPredictionFunction();
		this.saveModelToFile(); 
	}

	/**
	 * Saves a model in Kelp format to file MODEL_FILENAME 
	 * @throws IOException
	 */
	private void saveModelToFile() throws IOException {
		if (this.model==null) {
			System.out.println("ERROR: model is null, thus it cannot be saved on file");
			System.exit(1);
		}
		ObjectSerializer serializer = new JacksonSerializerWrapper();
		serializer.writeValueOnFile(this.model, MODEL_FILENAME);
	}
	
	/**
	 * Loads a model in Kelp format from file MODEL_FILENAME.    
	 * @return true whether the model has been successfully loaded, false otherwise  
	 */
	private boolean loadModelFromFile() {
		ObjectSerializer serializer = new JacksonSerializerWrapper();
		File file = new File(MODEL_FILENAME);
		try {
			this.model = serializer.readValue(file, BinaryLinearClassifier.class);
		}catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Classifies a test set using current model (which is loaded if necessary). 
	 * @return the classification accuracy
	 * @throws Exception
	 */
	private float classifyTestSet() throws Exception {
		
		SimpleDataset testSet = new SimpleDataset();
		if (this.model==null) {
			this.loadModelFromFile();
		}
		testSet.populate(TEST_FILENAME);
		BinaryClassificationEvaluator evaluator = new BinaryClassificationEvaluator(this.positiveClass);
		for (Example e : testSet.getExamples()) {
			BinaryMarginClassifierOutput predict = model.predict(e);
			evaluator.addCount(e, predict);
		}
		return evaluator.getAccuracy();
	}
	
	
	private SimpleExample createKelpExampleFromVector(ArrayList<Double> featureValues) {
		
		SparseVector sp = new SparseVector();
		
		int i = 0; 
		for (double entry : featureValues) {
			i = i+1;
			sp.setFeatureValue(Integer.toString(i), (float) entry);
		}
		//sp.addFeaturesFromList(featureValues);
		double[] featureArray = ArrayUtils.toPrimitive(featureValues.toArray(new Double[featureValues.size()]));
		DenseVector dv = new DenseVector(featureArray);
		
		SimpleExample ex = new SimpleExample();
		//ex.addRepresentation(VECTORIAL_LINEARIZATION_NAME, sp);
		ex.addRepresentation(VECTORIAL_LINEARIZATION_NAME, dv);
		return ex;		
	}
	
	/**
	 * getQuestionAnswers
	 *  
	 * @param userQuestion, the question in plain text
	 * @throws UIMAException
	 * @throws IOException
	 */
	public ArrayList<Float> getQuestionAnswers(String userQuestion) throws UIMAException, IOException {
		
		//temporary
/*		ArrayList<Question> threads = new ArrayList<Question>();
		Document doc = JsoupUtils.getDoc("semeval2015-3/data/SemEval2015-Task3-English-data/datasets/trialSearchResult.xml");
		Elements candidateAnswers = doc.getElementsByTag("Question");
		for (Element xmlThread : candidateAnswers) {
			//this.featureMapper.getCommentFeatureRepresentation(xmlThread);
		}
*/		//
		ArrayList<Double> f = new ArrayList<Double>();
		for (int i=1;i<=82;i=i+1) {
			f.add(0.3);
		}
		SimpleExample ex;
		ArrayList<Float> scores = new ArrayList<Float>();
		ex = createKelpExampleFromVector(f);
		scores.add(model.predict(ex).getScore(positiveClass));
		for(Question thread : this.retrieveCandidateAnswers(userQuestion)) {
			//get feature representation for question and comments and convert them to Kelp format
			//ex = createKelpExampleFromVector(featureMapper.getCommentFeatureRepresentation(thread, userQuestion));
			//scores.add(model.predict(ex).getScore(positiveClass));
		}
		
		return scores;
	}
	
	/**
	 * Retrieve a set of threads, each one containing a candidate answer to the question typed by the user
	 * 
	 * @param userQuestion a String with the question typed by the user
	 * @return an array of Element objects, each one related to the thread containing a candidate answer 
	 * @throws IOException 
	 */
	private List<Question> retrieveCandidateAnswers(String userQuestion) throws IOException {
		
		ArrayList<Question> candidateAnswers = new ArrayList<Question>();
		//retrieveLinksToRelatedQuestions(userQuestion);
		//if null then STOP
		//String[] = getQuestionIDsFromLinks(String[])
		//threadExtract()
		//for each link l, retrieve the corresponding thread in xml format
		//put xml file in a string?

		//temporary solution to provide fake candidate answers
		Document doc = JsoupUtils.getDoc("semeval2015-3/data/SemEval2015-Task3-English-data/datasets/trialSearchResult.xml");
		doc.select("QURAN").remove();
		doc.select("HADEETH").remove();
		//candidateAnswers = doc.getElementsByTag("Question");
		//		
		return candidateAnswers;
	}	
	
	public static void main(String[] args) throws Exception {
		
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		Demo demo = new Demo();
		
		/* Learn a model from the training set if the model can 
		 * not be loaded or learning is forced */
		if (TRAIN_SYSTEM || !demo.loadModelFromFile()) {
			if (TRAIN_SYSTEM) {
				System.out.println("Forcing system training...");
			}else {
				System.out.println("Saved model not found, training the system...");
			}
			demo.trainSystem();
		}
		
		if (!demo.loadModelFromFile()) {	
			System.out.println("Error: cannot load model from file: " + MODEL_FILENAME);
			System.exit(1);
		}			
		
		String userQuestion = "What is your name?";
		System.out.println("Processing question: " + userQuestion);
		ArrayList<Float> scores = demo.getQuestionAnswers(userQuestion);
		System.out.println(scores.get(0));
		System.out.println("Done");
		
		
	}

}

