package qa.qcri.qf.emnlp2015;

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

import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.semeval2015_3.FeatureExtractor;
import qa.qcri.qf.semeval2015_3.PairFeatureFactoryEnglish;
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
import qa.qf.qcri.cqa.CQAabstractElement;
import qa.qf.qcri.cqa.CQAcomment;
import qa.qf.qcri.cqa.CQAinstance;
import qa.qf.qcri.cqa.CQAquestion;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Opposite to the original version, this one uses the new CQA* classes for 
 * question and comment representations inside of a CQAinstance.
 * 
 * For the original implementation, as used in EMNLP 2015, see 
 * {@link CommentSelectionDatasetCreatorV2}
 * 
 * @author albarron
 *
 */
public class CommentSelectionDatasetCreatorV2 {

	protected static final boolean GENERATE_MASSIMO_FEATURES = true;
	protected static final boolean GENERATE_ALBERTO_AND_SIMONE_FEATURES = true;

	protected static final boolean ONLY_BAD_AND_GOOD_CLASSES = true;
	protected static final Boolean THREE_CLASSES = false;
	
	/** True if we want to compute comment-to-comment similarities */
	protected static final boolean INCLUDE_SIMILARITIES = false;
	
	/** True if the combination is a concat; false if it is a subtract */
	protected static final Boolean COMBINATION_CONCAT = false;
	
	/** True if we want to subtract, without absolute value */
	protected static final boolean COMBINATION_SUBTR_NOABS = false;
	
	/** 
	 * These flag and value limit the number of comments per question 
	 * to be considered (intends to reduce the impact of long threads). 
	 */
	private static final boolean LIMIT_COMMENTS_PER_Q = false;
	protected static final int LIMIT_COMMENTS = 20;
	protected static boolean LIMIT_COMMENTS_ACTIVE = LIMIT_COMMENTS_PER_Q; 
	
	/**
	 * True if you want to produce also data for SVMLightTK in order to train a 
	 * structural model with trees and feature vectors.
	 */
	protected static final boolean PRODUCE_SVMLIGHTTK_DATA = false;
	protected static final boolean PRODUCE_KELP_DATA = true;

	protected static final boolean USE_QCRI_ALT_TOOLS = false;

	//TODO ABC, SEP 9. THESE VARIABLES SHOULDN'T BE PUBLIC!!! 
	//	public static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
	//			+ "SemEval2015-Task3-English-data/tmp/CQA-QL-train.xml";
	protected static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-train.xml";
	
	protected static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-devel.xml";
	
	protected static final String CQA_QL_TEST_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-test.xml";

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
	protected static final String PARAMETER_LIST = Joiner.on(",").join(
			new String[] { RichNode.OUTPUT_PAR_LEMMA, RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
	
	protected static final String GOOD = "GOOD";
	protected static final String BAD = "BAD";

	protected static final String LABEL_MATCH = "EQUAL";
	protected static final String LABEL_NO_MATCH = "DIFF";
	
	public static final String LANG_ENGLISH = "ENGLISH";

	protected Set<String> a_labels = new HashSet<>();
	protected Set<String> b_labels = new HashSet<>();

	protected FileManager fm;

	protected AnalysisEngine[] analysisEngineList;

	protected PairFeatureFactoryEnglish pfEnglish;

	protected Alphabet alphabet;
	protected Stopwords stopwords;
	protected Analyzer analyzer;
	
	protected Map<String,UserProfile> userProfiles;

	public CommentSelectionDatasetCreatorV2() throws UIMAException, IOException {
		this.fm = new FileManager();
		this.alphabet = new Alphabet();
		setupUimaTools();
	}
	
	public static void main(String[] args) 
	    throws IOException, UIMAException, SimilarityException {
		/** Setup logger */
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		
		/** Run the code for the English tasks */
		new CommentSelectionDatasetCreatorV2().runForEnglish();
	}

	public void runForEnglish() throws UIMAException, IOException {
	  //TODO This should be changed to logger?
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
		setupUimaTools();
		
		try {
			this.processEnglishFile(docTrain, CQA_QL_TRAIN_EN, "train");	
			this.processEnglishFile(docDevel, CQA_QL_DEV_EN, "devel");
			LIMIT_COMMENTS_ACTIVE = false;
			this.processEnglishFile(docTest, CQA_QL_TEST_EN, "test");
		} catch (UIMAException | IOException
				| SimilarityException e) {
			e.printStackTrace();
		}
		
		System.out.println("TOTAL NUMBER OF REMOVED SIGNATURES: " + 
		     UserProfile.getRemovedSignatures());
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
	//TODO the method should be private
	public void processEnglishFile(Document doc, String dataFile, String suffix)
			throws ResourceInitializationException, UIMAException, IOException,
			AnalysisEngineProcessException, SimilarityException {

		
	  String plainTextOutputPath = dataFile + "plain.txt";
    String goodVSbadOutputPath = dataFile + ".csv";
    String pairwiseOutputPath = dataFile + getPairwiseSuffix();
    String kelpFilePath = dataFile + ".klp";
    
		/** Marker which adds relational information to a pair of trees	 */
		MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(
				new MarkTwoAncestors());

		/** Load stopwords for english */
		marker.useStopwords(Stopwords.STOPWORD_EN);

		/** Tree serializer for converting tree structures to string */
		TreeSerializer ts = new TreeSerializer().enableRelationalTags().useRoundBrackets();

		///** Instantiate CASes */ assigned in the for loop
		//JCas questionCas = JCasFactory.createJCas();
		
		//WriteFile out = new WriteFile(dataFile + ".csv");
		//TODO ABC, Sep 10th 2015. Do we really need this? It seems like a bad patch
		doc.select("QURAN").remove();
		doc.select("HADEETH").remove();

		boolean firstRow = true;

		/** Consume data */
		Elements questions = doc.getElementsByTag("Question");		
		int numberOfQuestions = questions.size();
		int qNumber = 1;

		for (Element question : questions) {
		  System.out.println("[INFO]: Processing " + qNumber++ 
		    + " out of " + numberOfQuestions);
		
		  CQAinstance cqainstance = qElementToObject(question);
		  
		 
		  getFeaturesFromThread(cqainstance);
		  // TODO MOVE FROM HERE TO getFeaturesFromThread. 
		  // FOR THAT the printing operations have to be moved out and
		  //question and comment must have a method to extract header+body. 
		  //Move them from SubjectBodyAggregator
		  //AQUI VOY
			/** Setup question CAS */
		  
			//questionCas.reset();
		  JCas questionCas = cqaElementToCas(cqainstance.getQuestion());

			fm.writeLn(plainTextOutputPath, "---------------------------- QID: " 
          + cqainstance.getQuestion().getId() + " USER:" + cqainstance.getQuestion().getUserId());
			// TODO When the cas was loaded inside this method, questionText was
      // assigned to questionCas and then used to writeLn. Confirm they 
      // are the same now (@see questionToCas)
			//fm.writeLn(plainTextOutputPath, questionText);
			fm.writeLn(plainTextOutputPath, questionCas.getDocumentText());
			
			/** Run the UIMA pipeline */
			SimplePipeline.runPipeline(questionCas, this.analysisEngineList);

			//this.analyzer.analyze(questionCas, new SimpleContent("q-" + qid, qsubject + ". " + qbody));
	
			/*Comment-level features to be combined*/
      List<List<Double>>  listFeatures = new ArrayList<List<Double>>();
			List<Map<String, Double>> albertoSimoneFeatures;
			if(GENERATE_ALBERTO_AND_SIMONE_FEATURES){ //TODO RENAME THIS PLEASE
				albertoSimoneFeatures = FeatureExtractor.extractFeatures(cqainstance);
			}
	
			int commentIndex = 0;
			List<JCas> allCommentsCas = new ArrayList<JCas>();
			for (CQAcomment c : cqainstance.getComments()) {	
				/** Setup comment CAS */
			  
			  JCas commentCas = cqaElementToCas(c);
			  
				/** Run the UIMA pipeline */	
				SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
				//this.analyzer.analyze(commentCas, new SimpleContent("c-" + cid, csubject + ". " + cbody));

				AugmentableFeatureVector fv;
				if (GENERATE_MASSIMO_FEATURES) {
					fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(questionCas, 
					    commentCas, PARAMETER_LIST);
				} else {
					fv = new AugmentableFeatureVector(this.alphabet);
				}

				if (GENERATE_ALBERTO_AND_SIMONE_FEATURES){
					Map<String, Double> featureVector = albertoSimoneFeatures.get(commentIndex);					
					for (String featureName : FeatureExtractor.getAllFeatureNames()) {
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if (value!=null) {
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

				/** Produce outputs */
				writeToPlainTextOutput(plainTextOutputPath, c, commentCas);
				
				
//				String goodVSbadOutputPath = dataFile + ".csv";
//				String pairwiseOutputPath 
				
				//FIXME Once we fix that issue with the features, we can know this info
				//in advance and fix the output, probably out of the method
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
				    c.getId() + "," + c.getGold() + "," + c.getGold_yn() + "," 
						+ Joiner.on(",").join(features));

				/** Produce also the file needed to train structural models */
				if (PRODUCE_SVMLIGHTTK_DATA) {
					produceSVMLightTKExample(questionCas, commentCas, suffix, ts,
							cqainstance.getQuestion().getId(),	c.getId(), c.getGold(),c.getGold_yn(),
							features);
				}
				if (PRODUCE_KELP_DATA) {
					produceKelpExample(questionCas, commentCas, kelpFilePath, ts,
							cqainstance.getQuestion().getId(), c.getId(), c.getGold(),c.getGold_yn(), features);
				}
				allCommentsCas.add(commentCas);
			}
			//TODO MOVE UP TO HERE
			
			this.fm.write(pairwiseOutputPath, computePairwiseFeatures(cqainstance, listFeatures, allCommentsCas));
			//out.writeLn(computePairwiseFeatures(q, listFeatures);
		}

		//		Iterator<String> iterator = questionCategories.iterator();
		//		while(iterator.hasNext()){
		//			System.out.println("CATEGORY_" + iterator.next());
		//		}

		
		this.fm.closeFiles();
	}
	
	
	private void getFeaturesFromThread(CQAinstance q) {
	  
	}
	

//  TODO move the writing to another class?	
	private void writeToPlainTextOutput(String path, CQAcomment c, JCas commentCas){
    fm.writeLn(path, 
        "- CID: " + c.getId().replace("_", "-") 
        + " USER:" + c.getUserId());
    
    // TODO When the cas was loaded inside this method, commentText was
    // assigned to commentCas and then used to writeLn. Confirm they 
    // are the same now {@see commentToCas}
    //fm.writeLn(plainTextOutputPath, commentText);
    fm.writeLn(path, commentCas.getDocumentText());

	}
	
	protected String computePairwiseFeatures(CQAinstance q, List<List<Double>>features,
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
	
  protected List<Double> serializeFv(FeatureVector fv) {
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

	
	protected void produceSVMLightTKExample(JCas questionCas, JCas commentCas,
			String suffix, TreeSerializer ts, String qid, String cid,
			    String cgold, String cgold_yn, List<Double> features) {
		/**
		 * Produce output for SVMLightTK
		 */

		TokenTree questionTree = RichTree.getPosChunkTree(questionCas);
		String questionTreeString = ts.serializeTree(questionTree);

		TokenTree commentTree = RichTree.getPosChunkTree(commentCas);
		String commentTreeString = ts.serializeTree(commentTree);

		for (String label : this.a_labels) {
			String svmLabel = "-1";
			if(label.equals(cgold)) {
				svmLabel = "+1";
			}

			String output = svmLabel + " ";
			output += " |BT| " + questionTreeString + " |BT| " + commentTreeString + " |ET| ";

			String featureString = "";

			for (int i = 0; i < features.size(); i++) {
				int featureIndex = i + 1;
				Double feature = features.get(i);
				if(!feature.isNaN() && !feature.isInfinite() && feature.compareTo(0.0) != 0) {

					if (Math.abs(feature) > 1e100) {
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

		for (String label : this.b_labels) {

			if (cgold_yn.equals("Not Applicable")) {
				continue;
			}

			String svmLabel = "-1";
			if (label.equals(cgold_yn)) {
				svmLabel = "+1";
			}

			String output = svmLabel + " |BT| " + questionTreeString
					+ " |BT| " + commentTreeString + " |ET| ";

			String featureString = "";

			for (int i = 0; i < features.size(); i++) {
				int featureIndex = i + 1;
				Double feature = features.get(i);
				if (!feature.isNaN() && !feature.isInfinite() && feature.compareTo(0.0) != 0) {

					if (Math.abs(feature) > 1e100) {
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

	protected void produceKelpExample(JCas questionCas, JCas commentCas,
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

		for (int i = 0; i < features.size(); i++) {
			int featureIndex = i + 1;
			Double feature = features.get(i);
			if (!feature.isNaN() && !feature.isInfinite() && feature.compareTo(0.0) != 0) {

				if (Math.abs(feature) > 1e100) {
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

	protected String getPairwiseSuffix(){
		String suffix = ".";
		if (INCLUDE_SIMILARITIES)
			suffix += "sim.";
		if (COMBINATION_CONCAT) 
			suffix += "concat.";
		
		return suffix + "pairwise.csv";
	}
	
	
	 /**
   * Gets the contents of the question and feed it into a Question object
   * @param qelement
   * @return object instance with the question data
   */
  private CQAinstance qElementToObject (Element qelement) {
    String id = qelement.attr("QID");
    String category = qelement.attr("QCATEGORY");
    String date = qelement.attr("QDATE");
    String userid = qelement.attr("QUSERID");
    String type = qelement.attr("QTYPE");
    String goldYN = qelement.attr("QGOLD_YN");
    String subject = TextNormalizer.normalize( 
            JsoupUtils.recoverOriginalText(
                    qelement.getElementsByTag("QSubject").get(0).text()) 
              );
    //TODO we don't normalise the subject?
    String body = qelement.getElementsByTag("QBody").get(0).text();
    //FIXME make it use useprofiles as below
    //body = JsoupUtils.recoverOriginalText(
    //            UserProfile.removeSignature(body, 
    //                          userProfiles.get(userid)));
    body = TextNormalizer.normalize(body);
    CQAquestion q = new CQAquestion(id,  date, userid, type, goldYN, subject, body);
    CQAinstance cqa = new CQAinstance(q, category);
    
    /** Parse comment nodes */
    for (Element comment : qelement.getElementsByTag("Comment")) {      
      String cid = comment.attr("CID");
      String cuserid = comment.attr("CUSERID");
      String cgold = comment.attr("CGOLD");
      
      if (ONLY_BAD_AND_GOOD_CLASSES) {
        cgold = (cgold.equalsIgnoreCase("good")) ? GOOD : BAD;
      }
      String cgold_yn = comment.attr("CGOLD_YN");
      String csubject = JsoupUtils.recoverOriginalText(
                    comment.getElementsByTag("CSubject").get(0).text());
      csubject = TextNormalizer.normalize(csubject);
      String cbody = comment.getElementsByTag("CBody").get(0).text();
      //FIXME make the following line work
      //cbody = JsoupUtils.recoverOriginalText(
      //          UserProfile.removeSignature(cbody, userProfiles.get(cuserid)));
      cbody = TextNormalizer.normalize(cbody);
      cqa.addComment(cid, cuserid, cgold, cgold_yn, csubject, cbody);
    }
    return cqa;
  }
  
  
  /**
   * Takes an element, either a question or a comment and generates a JCas of 
   * its whole text (subject + body)
   * @param element either a question or a comment
   * @return JCas instance of the text in the element
   * @throws UIMAException
   */
  protected JCas cqaElementToCas(CQAabstractElement element) throws UIMAException {
    JCas jcas = JCasFactory.createJCas();
    jcas.setDocumentLanguage("en");
    jcas.setDocumentText(element.getWholeText());
    //FIXME replace above command with this one (and make it work): jcas.setDocumentText(element.getWholeTextNormalized(userProfiles));
    return jcas;
  }
  
  /**
   * @deprecated use {@link cqaElementToCas} instead
   * @param cqa
   * @return
   * @throws UIMAException
   */
  @Deprecated
  private JCas questionToCas (CQAinstance cqa) throws UIMAException{
    JCas questionCas = JCasFactory.createJCas();
    questionCas.setDocumentLanguage("en");
    questionCas.setDocumentText(cqa.getQuestion().getWholeText());
    return questionCas;
  }
//  
//  private JCas commentToCas (CQAcomment comment) throws UIMAException{
//    JCas commentCas = JCasFactory.createJCas();
//    commentCas.setDocumentLanguage("en");
////    String commentText = comment.getWholeText();
//    commentCas.setDocumentText(comment.getWholeText());
//    return commentCas;
//  }
    
  /**
   * @deprecated use {@link cqaElementToCas} instead
   * @param comment
   * @return
   * @throws UIMAException
   */
  @Deprecated
  private JCas commentToCas (Comment comment) throws UIMAException{
    JCas commentCas = JCasFactory.createJCas();
    commentCas.setDocumentLanguage("en");
    String commentText = SubjectBodyAggregator.getCommentText(
            comment.getCsubject(), comment.getCbody() );
//    if(commentText.contains("&")){
//      System.out.println(commentText);
//    }
    commentCas.setDocumentText(commentText);
    return commentCas;
  }
  
	 private String standardCombination(CQAinstance cqainstance, List<List<Double>>features,
	      List<JCas> allCommentsCas){
	    
	    StringBuffer sb = new StringBuffer();
	    
	    List<CQAcomment> comments =cqainstance.getComments();

	    for (int i=0; i < comments.size() -1 ; i++) {
	      CQAcomment comment1 = comments.get(i);
	      
	      for (int j=i+1; j < comments.size(); j++){
	        CQAcomment comment2 = comments.get(j);
	        
	        sb.append(comment1.getId())
	          .append("-")
	          .append(comment2.getId())
	          .append(",");
	        sb.append(getClassLabel(comment1.getGold(), comment2.getGold()));
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
	          
//	          System.out.println(
//	              ids.get(i) + ","+
//	              labels.get(i) + ","+
//	              ids.get(j)+","+
//	              labels.get(j) + ","+
//	              Joiner.on(",").join(this.serializeFv(fv))
//	          );
	          
	          sb.append(",")
	            .append(Joiner.on(",").join(this.serializeFv(fv)));
	          
	        }

	        //out.writeLn(sb.toString());
	        sb.append("\n");
	      }
	    }
	    return sb.toString();
	  }
	  
	  //TODO ABC, Sep 9. ???????
	  private String garbage(CQAinstance q, List<List<Double>>features){
//	    StringBuffer sb = new StringBuffer();
//	    List<Comment> comments =q.getComments();
//	    for (int i=0; i < comments.size() -1 ; i++){
//	      Comment comment1 = comments.get(i);
//	      for (int j = comments.size()-1; j>=0; j--){
//	        Comment comment2 = comments.get(j);
//	        if (i==j)
//	          continue;
	//
//	        sb.append(comment1.getCid())
//	        .append("-")
//	        .append(comment2.getCid())
//	        .append(",");
//	        
//	        sb.append(getClassLabel(comment1.getCgold(), comment2.getCgold()));
//	        
////	        if (comment1.getCgold().get(i).equals(comment2.getCgold())){
////	          sb.append(LABEL_MATCH);
////	          if (THREE_CLASSES) {
////	            if (listCgold.get(i).toLowerCase().equals("good")){
////	              sb.append("_GOOD");
////	            } else {
////	              sb.append("_BAD");
////	            }
////	          }
////	        }else { 
////	          sb.append(LABEL_NO_MATCH);
////	        }
//	        
//	        sb.append(",");
	//
//	        sb.append(Joiner.on(",").join(
//	            difference(listFeatures.get(i), listFeatures.get(j))));
	//
	//
//	        out.writeLn(sb.toString());
//	        sb.delete(0, sb.length());
//	      }
//	    }
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
	    } else { 
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


	private void setupUimaTools() throws IOException, UIMAException {
    /*
    Document docTrain = JsoupUtils.getDoc(CQA_QL_TRAIN_EN);
    Document docDevel = JsoupUtils.getDoc(CQA_QL_DEV_EN);
    Document docTest = JsoupUtils.getDoc(CQA_QL_TEST_EN);

    this.userProfiles = UserProfile.createUserProfiles(docTrain, docDevel, docTest);
    //////user profiles are built on the training+dev+test semeval2015 datasets! 
    */
    
//    for(Entry<String, UserProfile> entry: userProfiles.entrySet()){
//      if(entry.getValue().getSignatures().size()>0){
//        System.out.println("---------- SIGNATURES FOR USER: " + entry.getKey() + " ----------");
//        for(String signature : entry.getValue().getSignatures()){
//          System.out.println("____\n" + JsoupUtils.recoverOriginalText(signature));
//        }
//      }
//    }
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

	
	
}
