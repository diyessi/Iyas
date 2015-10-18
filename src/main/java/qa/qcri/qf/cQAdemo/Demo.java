package qa.qcri.qf.cQAdemo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import edu.stanford.nlp.util.StringUtils;
import qa.qf.qcri.cqa.CQAinstance;

public class Demo {

	private static String MODEL_FILE_NAME = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/" 
			+ "CQA-QL-train.xml.klp.model";

	private CommentSelectionDatasetCreator featureMapper; 
	private ModelTrainer model;
	private QuestionRetriever qr;
	private QatarLivingURLMapping threadObjectBuilder;
	
	public Demo()  {
		try {
			this.featureMapper = new CommentSelectionDatasetCreator();
		} catch (UIMAException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.model = new ModelTrainer();
		this.qr = new QuestionRetriever();
		//this.threadObjectBuilder = new LinkToQuestionObjectMapper();
		this.threadObjectBuilder = new QatarLivingURLMapping("data/qatarliving/");
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
		
		List<CQAinstance> candidateAnswers;
			
		candidateAnswers = threadObjectBuilder.getQuestions(qr.getLinks(userQuestion));
		if (candidateAnswers.size()==0) {
			System.out.println("No similar questions were found in QatarLiving");
			//System.exit(0); //TODO delete this line if possible
		}
		return candidateAnswers;
	}	

	
	/**
	 * getQuestionAnswers
	 *  
	 * @param userQuestion, the question in plain text
	 * @throws UIMAException
	 * @throws IOException
	 */
	public List<CQAinstance> getQuestionAnswers(String userQuestion) 
			throws IOException {
		List<List<Double>> threadFeatures = new ArrayList<List<Double>>();
		List<CQAinstance> threads;
		float score;
		
		threads = retrieveCandidateAnswers(userQuestion);
		for(CQAinstance thread : threads) {
			try {
				threadFeatures = featureMapper.getCommentFeatureRepresentation(thread);
			} catch (UIMAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (int i=0; i<thread.getNumberOfComments(); i++) {
				score = model.getExampleScoreFromFeatureVector(threadFeatures.get(i));
				thread.getComment(i).setPrediction("", score); 
			}
		}
		
		return threads;
	}
	
	public boolean loadModel(String modelFileName) {
		return this.model.loadModelFromFile(modelFileName);
	}
	
	
	
	public static void main(String[] args) throws Exception {
		
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		String userQuestion;
		
		Demo demo = new Demo();
				
		if (!demo.loadModel(MODEL_FILE_NAME)) {	
			System.out.println("Error: cannot load model from file: " 
					+ MODEL_FILE_NAME);
			System.exit(1);
		}			
		//write on the log file from which file the model has been loaded
		
		if(args.length > 0) {
			userQuestion = StringUtils.join(args, " ");
		}else{
			System.out.println("Asking a deafult question");
			userQuestion = "How can I get a working visa in Qatar?";
		}
		System.out.println("Processing question: " + userQuestion);
		
		List<CQAinstance> threads = demo.getQuestionAnswers(userQuestion);

		OutputVisualization out = new OutputVisualization(threads);
		System.out.println("User Question: " + userQuestion);
		out.printOnCommandLine();
		System.out.println("Done");
		
	}

}

