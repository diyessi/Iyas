package qa.qcri.qf.cQAdemo;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import qa.qcri.qf.emnlp2015.SubjectBodyAggregator;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.semeval2015_3.FeatureExtractor;
import qa.qcri.qf.semeval2015_3.PairFeatureFactoryEnglish;
import qa.qcri.qf.semeval2015_3.Question;
import qa.qcri.qf.semeval2015_3.Question.Comment;
import qa.qcri.qf.semeval2015_3.textnormalization.JsoupUtils;
import qa.qcri.qf.semeval2015_3.textnormalization.TextNormalizer;
import qa.qcri.qf.semeval2015_3.textnormalization.UserProfile;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import util.Stopwords;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpChunker;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;


public class CommentSelectionDatasetCreator {

	private static final boolean GENERATE_MASSIMO_FEATURES = true;
	private static final boolean GENERATE_ALBERTO_AND_SIMONE_FEATURES = true;

	
	private static final boolean ONLY_BAD_AND_GOOD_CLASSES = false;
	
	public static final Boolean THREE_CLASSES = true;
	
	
	//True if we want to compute comment-to-comment similarities
	private static final boolean INCLUDE_SIMILARITIES = false;
	
	//True if the combination is a concat; false if it is a subtract
	public static final Boolean COMBINATION_CONCAT = false;
	
	//True if we want to subtract, without absolute value
	public static final boolean COMBINATION_SUBTR_NOABS = false;
	
	/** With this flag and value we limit the number fo comments per question 
	 * to be considered (this intends to reduce the impact of long threads. 
	 * */
	public static final boolean LIMIT_COMMENTS_PER_Q = false;
	public static final int LIMIT_COMMENTS = 20;
	public static boolean LIMIT_COMMENTS_ACTIVE = LIMIT_COMMENTS_PER_Q; 
	
	/**
	 * Set this option to true if you want to produce also data for
	 * SVMLightTK in order to train ad structural model with trees
	 * and feature vectors.
	 */
	public static final boolean PRODUCE_SVMLIGHTTK_DATA = false;
	public static final boolean PRODUCE_KELP_DATA = true;


	public static final boolean USE_QCRI_ALT_TOOLS = false;

	//	public static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
	//			+ "SemEval2015-Task3-English-data/tmp/CQA-QL-train.xml";
	public static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/kk/CQA-QL-train.xml";
	
	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/kk/CQA-QL-devel.xml";
	
	public static final String CQA_QL_TEST_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/kk/CQA-QL-test.xml";

	//	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
	//			+ "SemEval2015-Task3-English-data/tmp/CQA-QL-devel.xml";


	//public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
	//		+ "SemEval2015-Task3-English-data/tmp/test_task3_English.xml";

	/*public static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-train.xml";

	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-devel.xml";*/

	/**
	 * Parameters for matching tree structures
	 */
	private static final String PARAMETER_LIST = Joiner.on(",").join(
			new String[] { RichNode.OUTPUT_PAR_LEMMA, RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
	
	private static final String GOOD = "GOOD";
	private static final String BAD = "BAD";

	private static final String LABEL_MATCH = "EQUAL";
	private static final String LABEL_NO_MATCH = "DIFF";
	
	public static final String LANG_ENGLISH = "ENGLISH";

	
	private Set<String> a_labels = new HashSet<>();

	private Set<String> b_labels = new HashSet<>();

	private FileManager fm;

	private AnalysisEngine[] analysisEngineList;

	private PairFeatureFactoryEnglish pfEnglish;

	private Alphabet alphabet;

	private Stopwords stopwords;

	private Analyzer analyzer;
	
	private Map<String,UserProfile> userProfiles;

	public static void main(String[] args) throws IOException, UIMAException, SimilarityException {
		/**
		 * Setup logger
		 */
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		
		/**
		 * Run the code for the English tasks
		 */
		new CommentSelectionDatasetCreator().runForEnglish();
	}

	public CommentSelectionDatasetCreator() {
		/**
		 * Default language
		 */

		this.fm = new FileManager();

		this.alphabet = new Alphabet();
	}


	public void runForEnglish() throws UIMAException, IOException {

		System.out.println("EXTRACTING THE USER SIGNATURES");
		Document docTrain = JsoupUtils.getDoc(CQA_QL_TRAIN_EN);
		Document docDevel = JsoupUtils.getDoc(CQA_QL_DEV_EN);
		Document docTest = JsoupUtils.getDoc(CQA_QL_TEST_EN);
		
		this.userProfiles = UserProfile.createUserProfiles(docTrain, docDevel, docTest);
//		for(Entry<String, UserProfile> entry: userProfiles.entrySet()){
//			if(entry.getValue().getSignatures().size()>0){
//				System.out.println("---------- SIGNATURES FOR USER: " + entry.getKey() + " ----------");
//				for(String signature : entry.getValue().getSignatures()){
//					System.out.println("____\n" + JsoupUtils.recoverOriginalText(signature));
//				}
//			}
//		}
		
		
		this.stopwords = new Stopwords(Stopwords.STOPWORD_EN);

		this.pfEnglish = new PairFeatureFactoryEnglish(this.alphabet);
		this.pfEnglish.setupMeasures(RichNode.OUTPUT_PAR_LEMMA, this.stopwords);

		/**
		 * Add some punctuation to the stopwords list
		 */
		for(String stopword : ".|...|\\|,|?|!|#|(|)|$|%|&".split("\\|")) {
			this.stopwords.add(stopword);
		}

		/**
		 * Specify A and B subtask labels 
		 */
		this.a_labels.add("Not English");
		this.a_labels.add("Good");
		this.a_labels.add("Potential");
		this.a_labels.add("Dialogue");
		this.a_labels.add("Bad");

		this.b_labels.add("No");
		this.b_labels.add("Yes");
		this.b_labels.add("Unsure");

		/**
		 * Create the analysis pipeline
		 */

		AnalysisEngine segmenter = createEngine(createEngineDescription(OpenNlpSegmenter.class));
		AnalysisEngine postagger = createEngine(createEngineDescription(OpenNlpPosTagger.class));
		AnalysisEngine chunker = createEngine(createEngineDescription(OpenNlpChunker.class));
		AnalysisEngine lemmatizer = createEngine(createEngineDescription(StanfordLemmatizer.class));

		this.analysisEngineList = new AnalysisEngine[4];
		this.analysisEngineList[0] = segmenter;
		this.analysisEngineList[1] = postagger;
		this.analysisEngineList[2] = chunker;
		this.analysisEngineList[3] = lemmatizer;

		this.analyzer = new Analyzer(new UIMAFilePersistence("CASes/semeval"));
		for(AnalysisEngine ae : this.analysisEngineList) {
			analyzer.addAE(ae);
		}

		try {
			this.processEnglishFile(docTrain, CQA_QL_TRAIN_EN, "train");	
			this.processEnglishFile(docDevel, CQA_QL_DEV_EN, "devel");
			LIMIT_COMMENTS_ACTIVE = false;
			this.processEnglishFile(docTest, CQA_QL_TEST_EN, "test");
		} catch (UIMAException | IOException
				| SimilarityException e) {
			e.printStackTrace();
		}
		
		System.out.println("TOTAL NUMBER OF REMOVED SIGNATURES: " + UserProfile.getRemovedSignatures());
	}


	
	/**
	 * Parses the questin and its comments and returns a handy object to get 
	 * access to all the necessary information.
	 * @param question
	 * @return
	 */
	private Question getFilledQuestion(Element question){
		Question q = new Question();
		
		/**
		 * Parse question node
		 */
		String qid = question.attr("QID");
		String qcategory = question.attr("QCATEGORY");
		String qdate = question.attr("QDATE");
		String quserid = question.attr("QUSERID");
		String qtype = question.attr("QTYPE");
		String qgold_yn = question.attr("QGOLD_YN");		
		String qsubject = JsoupUtils.recoverOriginalText(question.getElementsByTag("QSubject").get(0).text());
		qsubject = TextNormalizer.normalize(qsubject);
		String qbody = question.getElementsByTag("QBody").get(0).text();
		qbody = JsoupUtils.recoverOriginalText(UserProfile.removeSignature(qbody, userProfiles.get(quserid)));
		qbody = TextNormalizer.normalize(qbody);
		//			questionCategories.add(qcategory);

		q.setQid(qid);
		q.setQcategory(qcategory);
		q.setQdate(qdate);
		q.setQuserId(quserid);
		q.setQtype(qtype);
		q.setQgoldYN(qgold_yn);
		q.setQsubject(qsubject);
		q.setQbody(qbody);
		
		
		/**
		 * Parse comment nodes
		 */
		Elements comments = question.getElementsByTag("Comment");

		/**
		 * Extracting context statistics for context Features
		 */		
		for(Element comment : comments) {
			
			String cid = comment.attr("CID");
			String cuserid = comment.attr("CUSERID");
			String cgold = comment.attr("CGOLD");
			//Replacing the labels for the "macro" ones: GOOD vs BAD
			if(ONLY_BAD_AND_GOOD_CLASSES){
				if(cgold.equalsIgnoreCase("good")){
					cgold = GOOD;
				}else{
					cgold = BAD;
				}
			}
			String cgold_yn = comment.attr("CGOLD_YN");
			String csubject = JsoupUtils.recoverOriginalText(comment.getElementsByTag("CSubject").get(0).text());
			csubject = TextNormalizer.normalize(csubject);
			String cbody = comment.getElementsByTag("CBody").get(0).text();
			cbody = JsoupUtils.recoverOriginalText(UserProfile.removeSignature(cbody, userProfiles.get(cuserid)));
			cbody = TextNormalizer.normalize(cbody);
			q.addComment(cid, cuserid, cgold, cgold_yn, csubject, cbody);
		}
		
		return q;
		
	}
	
	
	/**
	 * Process the xml file and output a csv file with the results in the same directory
	 * @param dataFile the xml file to process
	 * @suffix suffix for identifying the data file 
	 * 
	 * @param doc
	 * @param dataFile
	 * @param suffix
	 * @throws ResourceInitializationException
	 * @throws UIMAException
	 * @throws IOException
	 * @throws AnalysisEngineProcessException
	 * @throws SimilarityException
	 */
	private void processEnglishFile(Document doc, String dataFile, String suffix)
			throws ResourceInitializationException, UIMAException, IOException,
			AnalysisEngineProcessException, SimilarityException {

		String plainTextOutputPath = dataFile + "plain.txt";
		String goodVSbadOutputPath = dataFile + ".csv";
		String pairwiseOutputPath = dataFile + getPairwiseSuffix();
		String kelpFilePath = dataFile + ".klp";

		/**
		 * Marker which adds relational information to a pair of trees
		 */
		MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(
				new MarkTwoAncestors());

		/**
		 * Load stopwords for english
		 */
		marker.useStopwords(Stopwords.STOPWORD_EN);

		/**
		 * Tree serializer for converting tree structures to string
		 */
		TreeSerializer ts = new TreeSerializer().enableRelationalTags().useRoundBrackets();

		/**
		 * Instantiate CASes
		 */
		JCas questionCas = JCasFactory.createJCas();
		
		//WriteFile out = new WriteFile(dataFile + ".csv");

		doc.select("QURAN").remove();
		doc.select("HADEETH").remove();

		boolean firstRow = true;

		/**
		 * Consume data
		 */
		Elements questions = doc.getElementsByTag("Question");		
		int numberOfQuestions = questions.size();
		int questionNumber = 1;

		for(Element question : questions) {

			System.out.println("[INFO]: Processing " + questionNumber++ + " out of " + numberOfQuestions);
			
			/*Comment-level features to be combined*/
			List<List<Double>>	listFeatures = new ArrayList<List<Double>>();
			
			Question q = getFilledQuestion(question);
						
			/**
			 * Setup question CAS
			 */
			questionCas.reset();
			questionCas.setDocumentLanguage("en");
			String questionText = SubjectBodyAggregator.getQuestionText(q.getQsubject(), q.getQbody());
			fm.writeLn(plainTextOutputPath, "---------------------------- QID: " + q.getQid() + " USER:" + q.getQuserid());
			fm.writeLn(plainTextOutputPath, questionText);
			questionCas.setDocumentText(questionText);

			/**
			 * Run the UIMA pipeline
			 */

			SimplePipeline.runPipeline(questionCas, this.analysisEngineList);
	
			//this.analyzer.analyze(questionCas, new SimpleContent("q-" + qid, qsubject + ". " + qbody));
	
			List<HashMap<String, Double>> albertoSimoneFeatures;
			if(GENERATE_ALBERTO_AND_SIMONE_FEATURES){
				//THIS FUNCTION EXTRACTS THE CONTEXT AND HEURISTICS YOU CAN USE
				albertoSimoneFeatures = FeatureExtractor.extractFeatures(q);
			}
	
			int commentIndex = 0;
			
			List<JCas> allCommentsCas = new ArrayList<JCas>();
			for(Comment comment : q.getComments()) {				
//				if(ONLY_BAD_AND_GOOD_CLASSES){
//					if(cgold.equalsIgnoreCase("good")){
//						cgold = GOOD;
//					}else{
//						cgold = BAD;
//					}
//				}

				/**
				 * Setup comment CAS
				 */
				JCas commentCas = JCasFactory.createJCas();
				commentCas.setDocumentLanguage("en");
				String commentText = SubjectBodyAggregator.getCommentText(
						comment.getCsubject(), comment.getCbody());
//				if(commentText.contains("&")){
//					System.out.println(commentText);
//				}
				commentCas.setDocumentText(commentText);
				fm.writeLn(plainTextOutputPath, 
							"- CID: " + comment.getCid().replace("_", "-") 
							+ " USER:" + comment.getCuserid());
				fm.writeLn(plainTextOutputPath, commentText);
				/**
				 * Run the UIMA pipeline
				 */
	
				SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
				//this.analyzer.analyze(commentCas, new SimpleContent("c-" + cid, csubject + ". " + cbody));

				AugmentableFeatureVector fv;
				if(GENERATE_MASSIMO_FEATURES){
					//THIS FUNCTION THAT EXTRACTS "MASSIMO'S" FEATURES
					fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(questionCas, commentCas, PARAMETER_LIST);

				}else{
					fv = new AugmentableFeatureVector(this.alphabet);
				}

				if(GENERATE_ALBERTO_AND_SIMONE_FEATURES){

					HashMap<String, Double> featureVector = albertoSimoneFeatures.get(commentIndex);
					
					for(String featureName : FeatureExtractor.getAllFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
					
						fv.add(featureName, featureValue);
					}

				}
				commentIndex++;

				/***************************************
				 * * * * PLUG YOUR FEATURES HERE * * * *
				 ***************************************/

				/**
				 * fv is actually an AugmentableFeatureVector from the Mallet library
				 * 
				 * Internally the features are named so you must specify an unique identifier.
				 * 
				 * An example:
				 * 
				 * ((AugmentableFeatureVector) fv).add("your_super_feature_id", 42);
				 * 
				 * or:
				 * 
				 * AugmentableFeatureVector afv = (AugmentableFeatureVector) fv;
				 * afv.add("your_super_feature_id", 42);
				 * 
				 */

				//((AugmentableFeatureVector) fv).add("quseridEqCuserid", quseridEqCuserid);

				/***************************************
				 * * * * THANKS! * * * *
				 ***************************************/

				/**
				 * Produce output line
				 */

//				String goodVSbadOutputPath = dataFile + ".csv";
//				String pairwiseOutputPath 
				
				if(firstRow) {
					//header for Good vs Bad
					this.fm.write(goodVSbadOutputPath, "qid,cgold,cgold_yn");
					for(int i = 0; i < fv.numLocations(); i++) {
						int featureIndex = i + 1;
						this.fm.write(goodVSbadOutputPath, ",f" + featureIndex);
					}
					this.fm.writeLn(goodVSbadOutputPath, "");
					
					//header for pairwise
					this.fm.write(pairwiseOutputPath, "qid,cgold");
					int numFeatures = fv.numLocations();
					if (COMBINATION_CONCAT){
						numFeatures *=2;
					}
					if (INCLUDE_SIMILARITIES){
						numFeatures += PairFeatureFactoryEnglish.NUM_SIM_FEATURES;
					}
						
					for(int i = 0; i < numFeatures; i++) {
						int featureIndex = i + 1;
						this.fm.write(pairwiseOutputPath, ",f" + featureIndex);
					}
					this.fm.writeLn(pairwiseOutputPath, "");

					firstRow = false;
				}

				List<Double> features = this.serializeFv(fv);
				listFeatures.add(features);

				this.fm.writeLn(goodVSbadOutputPath, 
						comment.getCid() + "," + comment.getCgold() + "," 
								+ comment.getCgold_yn() + "," 
						+ Joiner.on(",").join(features));

				/**
				 * Produce also the file needed to train structural models
				 */
				if(PRODUCE_SVMLIGHTTK_DATA) {
					produceSVMLightTKExample(questionCas, commentCas, suffix, ts,
							q.getQid(), comment.getCid(), comment.getCgold(), 
							comment.getCgold_yn(), features);
				}
				if(PRODUCE_KELP_DATA){
					produceKelpExample(questionCas, commentCas, kelpFilePath, ts,
							q.getQid(), comment.getCid(), comment.getCgold(), 
							comment.getCgold_yn(), features);
				}
				allCommentsCas.add(commentCas);
				
			}
			
			this.fm.write(pairwiseOutputPath, computePairwiseFeatures(q, listFeatures, allCommentsCas));
			//out.writeLn(computePairwiseFeatures(q, listFeatures);
		}

		//		Iterator<String> iterator = questionCategories.iterator();
		//		while(iterator.hasNext()){
		//			System.out.println("CATEGORY_" + iterator.next());
		//		}

		
		this.fm.closeFiles();
		
	}

	private String computePairwiseFeatures(Question q, List<List<Double>>features,
			List<JCas> allCommentsCas){
		
		if (features.size() <= 1 ||	//only one comment; otherwise nothing to do
			(LIMIT_COMMENTS_ACTIVE && features.size() >= LIMIT_COMMENTS)){	
			return "";
		}

		//in this case we subtract the features. 
		//as a result, comparing c1,c2 is NOT the same as c2,c1

		StringBuffer sb = new StringBuffer();

		if (COMBINATION_SUBTR_NOABS) {
			return garbage(q, features);
		} else {
			return standardCombination(q, features, allCommentsCas);
		}				
		
		//sb.append("\n");

		//sb.delete(0, sb.length());
		//				out.writeLn(cid + "," + cgold + "," + cgold_yn + "," + Joiner.on(",").join(features));	
	}
	
	
	private String standardCombination(Question q, List<List<Double>>features,
			List<JCas> allCommentsCas){
		
		StringBuffer sb = new StringBuffer();
		
		List<Comment> comments =q.getComments();

		for (int i=0; i < comments.size() -1 ; i++){
			Comment comment1 = comments.get(i);
			
			for (int j=i+1; j < comments.size(); j++){
				Comment comment2 = comments.get(j);
				
				sb.append(comment1.getCid())
				.append("-")
				.append(comment2.getCid())
				.append(",");
				sb.append(getClassLabel(comment1.getCgold(), comment2.getCgold()));
				sb.append(",");

				if (COMBINATION_CONCAT){
					sb.append(Joiner.on(",").join(
							concatVectors(features.get(i), features.get(j))));
				} else {
					sb.append(Joiner.on(",").join(
							absoluteDifference(features.get(i), features.get(j))));
				}
				
				//Simimarities
				if (INCLUDE_SIMILARITIES) {
					AugmentableFeatureVector fv;
					fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(
							allCommentsCas.get(i), allCommentsCas.get(j), PARAMETER_LIST);
					
//					System.out.println(
//							ids.get(i) + ","+
//							labels.get(i) + ","+
//							ids.get(j)+","+
//							labels.get(j) + ","+
//							Joiner.on(",").join(this.serializeFv(fv))
//					);
					
					sb.append(",")
						.append(Joiner.on(",").join(this.serializeFv(fv)));
					
				}

				//out.writeLn(sb.toString());
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
	private String garbage(Question q, List<List<Double>>features){
//		StringBuffer sb = new StringBuffer();
//		List<Comment> comments =q.getComments();
//		for (int i=0; i < comments.size() -1 ; i++){
//			Comment comment1 = comments.get(i);
//			for (int j = comments.size()-1; j>=0; j--){
//				Comment comment2 = comments.get(j);
//				if (i==j)
//					continue;
//
//				sb.append(comment1.getCid())
//				.append("-")
//				.append(comment2.getCid())
//				.append(",");
//				
//				sb.append(getClassLabel(comment1.getCgold(), comment2.getCgold()));
//				
////				if (comment1.getCgold().get(i).equals(comment2.getCgold())){
////					sb.append(LABEL_MATCH);
////					if (THREE_CLASSES) {
////						if (listCgold.get(i).toLowerCase().equals("good")){
////							sb.append("_GOOD");
////						} else {
////							sb.append("_BAD");
////						}
////					}
////				}else { 
////					sb.append(LABEL_NO_MATCH);
////				}
//				
//				sb.append(",");
//
//				sb.append(Joiner.on(",").join(
//						difference(listFeatures.get(i), listFeatures.get(j))));
//
//
//				out.writeLn(sb.toString());
//				sb.delete(0, sb.length());
//			}
//		}
		return "";
	}
	
	private String getClassLabel(String label1, String label2){
		String newLabel = new String();
		if (label1.equals(label2)){
			newLabel += LABEL_MATCH;
			if (THREE_CLASSES) {
				if (label1.toLowerCase().equals("good")){
					newLabel += "_GOOD";
				} else {
					newLabel += "_BAD";
				}
			}
		}else { 
			newLabel = LABEL_NO_MATCH;
		}
		return newLabel; 
	}
	
	
	private List<Double> absoluteDifference(List<Double> v1, List<Double> v2){
		if (v1.size() != v2.size()) {
			System.err.println("Vectors size mismatch");
			System.exit(1);
		}
		List<Double> diff = new ArrayList<Double>();
		for (int i=0; i < v1.size(); i++){
			diff.add(Math.abs(v1.get(i)-v2.get(i)));
		}		
		return diff;		
	}

	private List<Double> concatVectors(List<Double> v1, List<Double> v2){
		if (v1.size() != v2.size()) {
			
			System.err.println("Vectors size mismatch");
			System.exit(1);
		}
		List<Double> cc = new ArrayList<Double>();
		cc.addAll(v1);
		cc.addAll(v2);				
		return  cc;		
	}
	

	
	
	private void produceSVMLightTKExample(JCas questionCas, JCas commentCas,
			String suffix, TreeSerializer ts, String qid, String cid,
			String cgold, String cgold_yn, List<Double> features) {
		/**
		 * Produce output for SVMLightTK
		 */

		TokenTree questionTree = RichTree.getPosChunkTree(questionCas);
		String questionTreeString = ts.serializeTree(questionTree);

		TokenTree commentTree = RichTree.getPosChunkTree(commentCas);
		String commentTreeString = ts.serializeTree(commentTree);

		for(String label : this.a_labels) {
			String svmLabel = "-1";
			if(label.equals(cgold)) {
				svmLabel = "+1";
			}

			String output = svmLabel + " ";
			output += " |BT| " + questionTreeString + " |BT| " + commentTreeString + " |ET| ";

			String featureString = "";

			for(int i = 0; i < features.size(); i++) {
				int featureIndex = i + 1;
				Double feature = features.get(i);
				if(!feature.isNaN() && !feature.isInfinite() && feature.compareTo(0.0) != 0) {

					if(Math.abs(feature) > 1e100) {
						feature = 0.0;
					}

					featureString += featureIndex + ":" 
							+ String.format("%f", feature) + " ";
				}
			}

			output += featureString + "|EV|";

			output += " #" + qid + "\t" + cid;

			fm.writeLn("semeval2015-3/svmlighttk/a/" + suffix + "/"
					+ label.replaceAll(" ", "_") + ".svm",
					output.trim());
		}

		for(String label : this.b_labels) {

			if(cgold_yn.equals("Not Applicable")) {
				continue;
			}

			String svmLabel = "-1";
			if(label.equals(cgold_yn)) {
				svmLabel = "+1";
			}

			String output = svmLabel + " |BT| " + questionTreeString
					+ " |BT| " + commentTreeString + " |ET| ";

			String featureString = "";

			for(int i = 0; i < features.size(); i++) {
				int featureIndex = i + 1;
				Double feature = features.get(i);
				if(!feature.isNaN() && !feature.isInfinite() && feature.compareTo(0.0) != 0) {

					if(Math.abs(feature) > 1e100) {
						feature = 0.0;
					}

					featureString += featureIndex + ":" 
							+ String.format("%f", feature) + " ";
				}
			}

			output += featureString + "|EV|";

			output += " #" + qid + "\t" + cid;

			fm.writeLn("semeval2015-3/svmlighttk/b/" + suffix + "/"
					+ label.replaceAll(" ", "_") + ".svm",
					output);
		}
	}

	private void produceKelpExample(JCas questionCas, JCas commentCas,
			String outputPath, TreeSerializer ts, String qid, String cid,
			String cgold, String cgold_yn, List<Double> features) {
		/**
		 * Produce output for Kelp
		 */

		TokenTree questionTree = RichTree.getPosChunkTree(questionCas);
		String questionTreeString = ts.serializeTree(questionTree, RichNode.OUTPUT_PAR_SEMANTIC_KERNEL);

		TokenTree commentTree = RichTree.getPosChunkTree(commentCas);
		String commentTreeString = ts.serializeTree(commentTree, RichNode.OUTPUT_PAR_SEMANTIC_KERNEL);

		String output = cgold + " ";
		output += "|<||BT:tree| " + questionTreeString.replace('|', '-') + " |ET||,||BT:tree| " + commentTreeString.replace('|', '-') + " |ET| |>| ";

		String featureString = "|BV:features|";

		for(int i = 0; i < features.size(); i++) {
			int featureIndex = i + 1;
			Double feature = features.get(i);
			if(!feature.isNaN() && !feature.isInfinite() && feature.compareTo(0.0) != 0) {

				if(Math.abs(feature) > 1e100) {
					feature = 0.0;
				}

				featureString += featureIndex + ":"
						+ String.format("%f", feature).replace(',', '.') + " ";
			}
		}

		output += featureString + "|EV|";

		output += "|BS:info| #" + qid + "\t" + cid + "|ES|";

		fm.writeLn(outputPath,
				output.trim());
	}

	private String getPairwiseSuffix(){
		String suffix = ".";
		if (INCLUDE_SIMILARITIES)
			suffix += "sim.";
		if (COMBINATION_CONCAT) 
			suffix += "concat.";
		
		return suffix + "pairwise.csv";
	}
	

	public List<Double> serializeFv(FeatureVector fv) {
		List<Double> features = new ArrayList<>();
		int numLocations = fv.numLocations();
		int[] indices = fv.getIndices();
		for (int index = 0; index < numLocations; index++) {
			int featureIndex = indices[index];
			double value = fv.value(featureIndex);
			features.add(value);
		}
		return features;
	}

}
