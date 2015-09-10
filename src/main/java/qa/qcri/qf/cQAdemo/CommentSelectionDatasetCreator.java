package qa.qcri.qf.cQAdemo;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

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
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import util.Stopwords;
import cc.mallet.types.AugmentableFeatureVector;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpChunker;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommentSelectionDatasetCreator 
    extends qa.qcri.qf.emnlp2015.CommentSelectionDatasetCreator{

//	/**
//	 * Set this option to true if you want to produce also data for
//	 * SVMLightTK in order to train ad structural model with trees
//	 * and feature vectors.
//	 */
	
	public static final boolean WRITE_FEATURES_TO_FILE = true;

	//TODO ABC, SEP 9. Confirm why these variables are global (but not defined)
	private JCas questionCas; //added by Giovanni
	private boolean firstRow; //added by Giovanni
	private String suffix; //added by Giovanni
	private TreeSerializer ts; //added by Giovanni
	private String plainTextOutputPath;//added by Giovanni
	private String goodVSbadOutputPath;//added by Giovanni
	private String pairwiseOutputPath;//added by Giovanni
	private String kelpFilePath;//added by Giovanni

	public CommentSelectionDatasetCreator() {
		super();
		this.firstRow = true;
	}

	public static void main(String[] args) throws IOException, UIMAException, SimilarityException {
		/**
		 * Setup logger
		 */
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		
		/**
		 * Run the code for the English tasks
		 */
		new ABCCommentSelectionDatasetCreator().runForEnglish();
	}

	//TODO ABC, Sep 2009. What Gio did here sounds reasonable: create a new method 
	//to setup the uimatools and remove all those noisy lines from this method. 
	//That should be transfered to the emnlp class. Discuss with him. The same applies
	//to method processEnglishFile (next)
	@Override
	public void runForEnglish() throws UIMAException, IOException {

		System.out.println("EXTRACTING THE USER SIGNATURES");
		Document docTrain = JsoupUtils.getDoc(CQA_QL_TRAIN_EN);
		Document docDevel = JsoupUtils.getDoc(CQA_QL_DEV_EN);
		Document docTest = JsoupUtils.getDoc(CQA_QL_TEST_EN);
		
		this.userProfiles = UserProfile.createUserProfiles(docTrain, docDevel, docTest);
		
		this.setupUimaTools();
		
		try {
			this.processEnglishFile(docTrain, CQA_QL_TRAIN_EN, "train");	
			this.processEnglishFile(docDevel, CQA_QL_DEV_EN, "devel");
			LIMIT_COMMENTS_ACTIVE = false;
			this.processEnglishFile(docTest, CQA_QL_TEST_EN, "test");
		} catch (UIMAException | IOException
				| SimilarityException e) {
			e.printStackTrace();
		}
		//TODO ABC, Sep 2009. . Document properly?
		System.out.println("TOTAL NUMBER OF REMOVED SIGNATURES: " + UserProfile.getRemovedSignatures());
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

		setOutputFiles(dataFile);
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
		this.ts = new TreeSerializer().enableRelationalTags().useRoundBrackets();

		/**
		 * Instantiate CASes
		 */
		//JCas questionCas = JCasFactory.createJCas();
		this.questionCas = JCasFactory.createJCas();

		//WriteFile out = new WriteFile(dataFile + ".csv");

		doc.select("QURAN").remove();
		doc.select("HADEETH").remove();

		//boolean firstRow = true;

		/**
		 * Consume data
		 */
		Elements questions = doc.getElementsByTag("Question");		
		int numberOfQuestions = questions.size();
		int questionNumber = 1;

		for (Element question : questions) {
			getCommentFeatureRepresentation(question); 
			//writeFeaturesToFile(AugmentableFeatureVector fv, JCas commentCas, String suffix, 
			//		TreeSerializer ts, String qid, String cid, String cgold, String cgold_yn)
		}
		
		for (Element question : questions) {

			/*Comment-level features to be combined*/
			List<List<Double>>	listFeatures = new ArrayList<List<Double>>();
			
			
			Question q = new Question();
			System.out.println("[INFO]: Processing " + questionNumber++ + " out of " + numberOfQuestions);
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
			q.setQbody(qbody);

			/**
			 * Setup question CAS
			 */
			questionCas.reset();
			questionCas.setDocumentLanguage("en");
			String questionText = SubjectBodyAggregator.getQuestionText(qsubject, qbody);
			fm.writeLn(plainTextOutputPath, "---------------------------- QID: " + qid + " USER:" + quserid);
			fm.writeLn(plainTextOutputPath, questionText);
			questionCas.setDocumentText(questionText);

			/**
			 * Run the UIMA pipeline
			 */

			SimplePipeline.runPipeline(questionCas, this.analysisEngineList);
	

			//this.analyzer.analyze(questionCas, new SimpleContent("q-" + qid, qsubject + ". " + qbody));

			/**
			 * Parse comment nodes
			 */
			Elements comments = question.getElementsByTag("Comment");
	
			/**
			 * Extracting context statistics for context Features
			 */
			
			for (Element comment : comments) {
				
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
	
			List<HashMap<String, Double>> albertoSimoneFeatures;
			if (GENERATE_ALBERTO_AND_SIMONE_FEATURES){
				albertoSimoneFeatures = FeatureExtractor.extractFeatures(q);
			}
	
			int commentIndex = 0;
			
			List<JCas> allCommentsCas = new ArrayList<JCas>();
			for (Comment comment : q.getComments()) {
	
				String cid = comment.getCid();
				String cuserid = comment.getCuserid();
				String cgold = comment.getCgold();

				
//				if(ONLY_BAD_AND_GOOD_CLASSES){
//					if(cgold.equalsIgnoreCase("good")){
//						cgold = GOOD;
//					}else{
//						cgold = BAD;
//					}
//				}

				String cgold_yn = comment.getCgold_yn();
				String csubject = comment.getCsubject();
				String cbody = comment.getCbody();
				/**
				 * Setup comment CAS
				 */
				JCas commentCas = JCasFactory.createJCas();
				commentCas.setDocumentLanguage("en");
				String commentText = SubjectBodyAggregator.getCommentText(csubject, cbody);
//				if(commentText.contains("&")){
//					System.out.println(commentText);
//				}
				commentCas.setDocumentText(commentText);
				fm.writeLn(plainTextOutputPath, "- CID: " + cid.replace("_", "-") + " USER:" + cuserid);
				fm.writeLn(plainTextOutputPath, commentText);
				/**
				 * Run the UIMA pipeline
				 */
	
				SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
				//this.analyzer.analyze(commentCas, new SimpleContent("c-" + cid, csubject + ". " + cbody));

				AugmentableFeatureVector fv;
				if (GENERATE_MASSIMO_FEATURES){
					fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(questionCas, commentCas, PARAMETER_LIST);

				} else {
					fv = new AugmentableFeatureVector(this.alphabet);
				}

				if (GENERATE_ALBERTO_AND_SIMONE_FEATURES){

					HashMap<String, Double> featureVector = albertoSimoneFeatures.get(commentIndex);
					
					for (String featureName : FeatureExtractor.getAllFeatureNames()){
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
				
				if (firstRow) {
					//header for Good vs Bad
					this.fm.write(goodVSbadOutputPath, "qid,cgold,cgold_yn");
					for (int i = 0; i < fv.numLocations(); i++) {
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
						
					for (int i = 0; i < numFeatures; i++) {
						int featureIndex = i + 1;
						this.fm.write(pairwiseOutputPath, ",f" + featureIndex);
					}
					this.fm.writeLn(pairwiseOutputPath, "");

					firstRow = false;
				}

				List<Double> features = this.serializeFv(fv);
				listFeatures.add(features);

				this.fm.writeLn(goodVSbadOutputPath, 
						cid + "," + cgold + "," + cgold_yn + "," 
						+ Joiner.on(",").join(features));

				/**
				 * Produce also the file needed to train structural models
				 */
				if (PRODUCE_SVMLIGHTTK_DATA) {
					produceSVMLightTKExample(questionCas, commentCas, suffix, ts,
							qid, cid, cgold, cgold_yn, features);
				}
				if (PRODUCE_KELP_DATA){
					produceKelpExample(questionCas, commentCas, kelpFilePath, ts,
							qid, cid, cgold, cgold_yn, features);
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

	private String getFileSuffix() {
		return this.suffix;
	}
	
	//TODO ABC, Sep 9. Gio: this is never used and returns the parameter. 
	//Is there some reason to maintain it?
	private void setFileSuffix(String suffix) {
		this.suffix = suffix;
	}
	
	private void setupUimaTools() throws IOException, UIMAException {

		/*
		Document docTrain = JsoupUtils.getDoc(CQA_QL_TRAIN_EN);
		Document docDevel = JsoupUtils.getDoc(CQA_QL_DEV_EN);
		Document docTest = JsoupUtils.getDoc(CQA_QL_TEST_EN);

		this.userProfiles = UserProfile.createUserProfiles(docTrain, docDevel, docTest);
		//////user profiles are built on the training+dev+test semeval2015 datasets! 
		*/
		
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
		for (String stopword : ".|...|\\|,|?|!|#|(|)|$|%|&".split("\\|")) {
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

	}
	
	private void setOutputFiles(String dataFile) {
		this.plainTextOutputPath = dataFile + "plain.txt";
		this.goodVSbadOutputPath = dataFile + ".csv";
		this.pairwiseOutputPath = dataFile + getPairwiseSuffix();
		this.kelpFilePath = dataFile + ".klp";		
	}
	
	private void writeFeaturesToFile(AugmentableFeatureVector fv, JCas commentCas,  
			String qid, String cid, String cgold, String cgold_yn) {
		
		List<Double> features = this.serializeFv(fv);

		if (this.firstRow) {
			//header for Good vs Bad
			this.fm.write(this.goodVSbadOutputPath, "qid,cgold,cgold_yn");
			for(int i = 0; i < fv.numLocations(); i++) {
				int featureIndex = i + 1;
				this.fm.write(this.goodVSbadOutputPath, ",f" + featureIndex);
			}
			this.fm.writeLn(this.goodVSbadOutputPath, "");
			
			//header for pairwise
			this.fm.write(this.pairwiseOutputPath, "qid,cgold");
			int numFeatures = fv.numLocations();
			if (COMBINATION_CONCAT){
				numFeatures *=2;
			}
			if (INCLUDE_SIMILARITIES){
				numFeatures += PairFeatureFactoryEnglish.NUM_SIM_FEATURES;
			}
				
			for (int i = 0; i < numFeatures; i++) {
				int featureIndex = i + 1;
				this.fm.write(this.pairwiseOutputPath, ",f" + featureIndex);
			}
			this.fm.writeLn(this.pairwiseOutputPath, "");
			this.firstRow = false;
		}
		
		this.fm.writeLn(this.goodVSbadOutputPath, 
				cid + "," + cgold + "," + cgold_yn + "," 
				+ Joiner.on(",").join(features));	
		
		if (PRODUCE_SVMLIGHTTK_DATA) {
			produceSVMLightTKExample(this.questionCas, commentCas, this.getFileSuffix(), this.ts,
					qid, cid, cgold, cgold_yn, features);
		}
		if (PRODUCE_KELP_DATA){
			produceKelpExample(this.questionCas, commentCas, this.kelpFilePath, this.ts,
					qid, cid, cgold, cgold_yn, features);
		}
		
		//return features;
	}
	
	
	public ArrayList<Double> getCommentFeatureRepresentation(Element thread) throws UIMAException, IOException {
		
		return this.getCommentFeatureRepresentation(thread, thread.getElementsByTag("QBody").get(0).text());
		
	}
	
	public ArrayList<Double> getCommentFeatureRepresentation(Element thread, String userQuestion) throws IOException, UIMAException {
		
		/*Comment-level features to be combined*/
		List<List<Double>>	listFeatures = new ArrayList<List<Double>>();
		
		Question q = new Question();
		/**
		 * Parse question node
		 */
		String qid = thread.attr("QID");
		String qcategory = thread.attr("QCATEGORY");
		String qdate = thread.attr("QDATE");
		String quserid = thread.attr("QUSERID");
		String qtype = thread.attr("QTYPE");
		String qgold_yn = thread.attr("QGOLD_YN");		
		String qsubject = JsoupUtils.recoverOriginalText(thread.getElementsByTag("QSubject").get(0).text());
		qsubject = TextNormalizer.normalize(qsubject);
		String qbody = thread.getElementsByTag("QBody").get(0).text();
		qbody = JsoupUtils.recoverOriginalText(UserProfile.removeSignature(qbody, userProfiles.get(quserid)));
		qbody = TextNormalizer.normalize(qbody);
		//			questionCategories.add(qcategory);

		q.setQid(qid);
		q.setQcategory(qcategory);
		q.setQdate(qdate);
		q.setQuserId(quserid);
		q.setQtype(qtype);
		q.setQgoldYN(qgold_yn);
		q.setQbody(qbody);
		
		Elements comments = thread.getElementsByTag("Comment");
		
		for (Element comment : comments) {
			
			String cid = comment.attr("CID");
			String cuserid = comment.attr("CUSERID");
			String cgold = comment.attr("CGOLD");
			//Replacing the labels for the "macro" ones: GOOD vs BAD
			if (ONLY_BAD_AND_GOOD_CLASSES){
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
		
		return this.getCommentFeatureRepresentation(q, userQuestion);
	}
	
	public ArrayList<Double> getCommentFeatureRepresentation(Question thread) throws IOException, UIMAException {
		return this.getCommentFeatureRepresentation(thread, "");
	}
	
	public ArrayList<Double> getCommentFeatureRepresentation(Question thread, String userQuestion) throws IOException, UIMAException {
		
		ArrayList<Double> featureMap = new ArrayList<Double>();

		/*Comment-level features to be combined*/
		List<List<Double>>	listFeatures = new ArrayList<List<Double>>();
		
		//Question q = new Question();
		/**
		 * Parse question node
		 */
		String qid = thread.getQid();
		String quserid = thread.getQuserid();
		String qsubject = TextNormalizer.normalize(thread.getQsubject());
		String qbody = thread.getQbody(); 
		qbody = JsoupUtils.recoverOriginalText(UserProfile.removeSignature(qbody, userProfiles.get(quserid)));
		qbody = TextNormalizer.normalize(qbody);
		//			questionCategories.add(qcategory);
		/**
		 * Setup question CAS
		 */
		this.questionCas.reset();
		this.questionCas.setDocumentLanguage("en");
		String questionText = SubjectBodyAggregator.getQuestionText(qsubject, qbody);
		//fm.writeLn(plainTextOutputPath, "---------------------------- QID: " + qid + " USER:" + quserid);
		//fm.writeLn(plainTextOutputPath, questionText);
		this.questionCas.setDocumentText(questionText);

		/**
		 * Run the UIMA pipeline
		 */

		SimplePipeline.runPipeline(questionCas, this.analysisEngineList);


		//this.analyzer.analyze(questionCas, new SimpleContent("q-" + qid, qsubject + ". " + qbody));

		/**
		 * Parse comment nodes
		 */
		//Elements comments = thread.getElementsByTag("Comment");

		/**
		 * Extracting context statistics for context Features
		 */
		
		for (Comment comment : thread.getComments()) {
			if (ONLY_BAD_AND_GOOD_CLASSES){
				String cgold = comment.getCgold();
				if(cgold.equalsIgnoreCase("good")){
					cgold = GOOD;
				} else {
					cgold = BAD;
				}
				comment.setGold(cgold);
			}
			String csubject = TextNormalizer.normalize(comment.getCsubject());
			String cbody = TextNormalizer.normalize(comment.getCbody());
		}
		List<HashMap<String, Double>> albertoSimoneFeatures;
		if (GENERATE_ALBERTO_AND_SIMONE_FEATURES){
			albertoSimoneFeatures = FeatureExtractor.extractFeatures(thread);
		}

		int commentIndex = 0;
		List<JCas> allCommentsCas = new ArrayList<JCas>();
		
		for (Comment comment : thread.getComments()) {
			String cid = comment.getCid();
			String cuserid = comment.getCuserid();
			String cgold = comment.getCgold();
			String cgold_yn = comment.getCgold_yn();
			String csubject = comment.getCsubject();
			String cbody = comment.getCbody();
			/**
			 * Setup comment CAS
			 */
			JCas commentCas = JCasFactory.createJCas();
			commentCas.setDocumentLanguage("en");
			String commentText = SubjectBodyAggregator.getCommentText(csubject, cbody);
			commentCas.setDocumentText(commentText);
			/**
			 * Run the UIMA pipeline
			 */
			SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
			//this.analyzer.analyze(commentCas, new SimpleContent("c-" + cid, csubject + ". " + cbody));

			AugmentableFeatureVector fv;
			if (GENERATE_MASSIMO_FEATURES){
				fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(questionCas, commentCas, PARAMETER_LIST);
			} else {
				fv = new AugmentableFeatureVector(this.alphabet);
			}

			if (GENERATE_ALBERTO_AND_SIMONE_FEATURES){
				HashMap<String, Double> featureVector = albertoSimoneFeatures.get(commentIndex);
				
				for (String featureName : FeatureExtractor.getAllFeatureNames()){
					Double value = featureVector.get(featureName);
					double featureValue =0;
					if (value!=null){
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
	
			List<Double> features = this.serializeFv(fv);
			listFeatures.add(features);
			
			if (WRITE_FEATURES_TO_FILE) {
				this.writeFeaturesToFile(fv, commentCas, qid, cid, cgold, cgold_yn);
			}
			/**
			 * Produce also the file needed to train structural models
			 */			
			allCommentsCas.add(commentCas);
			
		}
		
		if (WRITE_FEATURES_TO_FILE) {
			this.fm.write(this.pairwiseOutputPath, computePairwiseFeatures(thread, listFeatures, allCommentsCas));
			//out.writeLn(computePairwiseFeatures(q, listFeatures);
		}
		
		return featureMap;
	}

}
