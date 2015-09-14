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
import qa.qf.qcri.cqa.CQAinstance;
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

	private static String MODEL_FILE_NAME = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/" 
			+ "CQA-QL-train.xml.csv.klp.model";

	private CommentSelectionDatasetCreator featureMapper; 
	private ModelTrainer model;
	
	public Demo() {
		this.featureMapper = new CommentSelectionDatasetCreator();
		this.model = new ModelTrainer();
	}

	/**
	 * Retrieve a set of threads, each one containing a candidate answer to the 
	 * question typed by the user
	 * 
	 * @param userQuestion a String with the question typed by the user
	 * @return an array of Element objects, each one related to the thread 
	 * containing a candidate answer 
	 * @throws IOException 
	 */
	private List<CQAinstance> retrieveCandidateAnswers(String userQuestion) 
			throws IOException {
		
		ArrayList<CQAinstance> candidateAnswers = new ArrayList<CQAinstance>();
		QuestionRetriever qr = new QuestionRetriever();
		LinkToQuestionObjectMapper threadObjectBuilder = 
				new LinkToQuestionObjectMapper(); 
		
		for (CQAinstance thread : threadObjectBuilder.getQuestions(qr.getLinks(userQuestion))) {
			candidateAnswers.add(thread);
		}
		if (candidateAnswers.size()==0) {
			System.out.println("No similar questions were found in QatarLiving");
			System.exit(0);
		}
		//temporary solution to provide fake candidate answers
		//Document doc = JsoupUtils.getDoc("semeval2015-3/data/SemEval2015-Task3-English-data/datasets/trialSearchResult.xml");
		//doc.select("QURAN").remove();
		//doc.select("HADEETH").remove();
		//candidateAnswers = doc.getElementsByTag("Question");
		//		
		return candidateAnswers;
	}	

	
	/**
	 * getQuestionAnswers
	 *  
	 * @param userQuestion, the question in plain text
	 * @throws UIMAException
	 * @throws IOException
	 */
	public ArrayList<Float> getQuestionAnswers(String userQuestion) 
			throws UIMAException, IOException {
		
		ArrayList<Float> scores = new ArrayList<Float>();
		
		//temporary
/*		ArrayList<Double> f = new ArrayList<Double>();
		for (int i=1;i<=82;i=i+1) {
			f.add(0.3);
		}
		scores.add(model.getExampleScoreFromFeatureVector(f));
*/		//delete from temporary to here
		
		for(CQAinstance thread : retrieveCandidateAnswers(userQuestion)) {
			//ArrayList<Double> f;
			//get feature representation for question and comments and classify them
			//TODO ALBERTO
			scores.add(model.getExampleScoreFromFeatureVector(f));
		}
		
		return scores;
	}
	
	private boolean loadModel(String modelFileName) {
		return this.model.loadModelFromFile(modelFileName);
	}
	
	
	
	public static void main(String[] args) throws Exception {
		
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		String userQuestion;
		
		if(args.length > 1) {
			MODEL_FILE_NAME = args[1];
		}
		Demo demo = new Demo();
				
		if (!demo.loadModel(MODEL_FILE_NAME)) {	
			System.out.println("Error: cannot load model from file: " 
					+ MODEL_FILE_NAME);
			System.exit(1);
		}			
		//write on the log file from which file the model has been loaded
		
		if(args.length > 0) {
			userQuestion = args[0];
		}else{
			System.out.println("Asking a deafult question");
			userQuestion = "What is your name?";
		}
		System.out.println("Processing question: " + userQuestion);
		
		ArrayList<Float> scores = demo.getQuestionAnswers(userQuestion);
		System.out.println(scores.get(0));
		System.out.println("Done");
		
	}

}

