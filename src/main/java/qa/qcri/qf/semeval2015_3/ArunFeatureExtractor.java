package qa.qcri.qf.semeval2015_3;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.fileutil.WriteFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
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


public class ArunFeatureExtractor {
	
	private static final boolean GENERATE_MASSIMO_FEATURES = true;
	private static final boolean GENERATE_ALBERTO_AND_SIMONE_FEATURES = true;
	private static final boolean CREATE_KELP_DATASETS = false;
	
	private static final boolean ONLY_BAD_AND_GOOD_CLASSES = false;
	private static final String GOOD = "GOOD";
	private static final String BAD = "BAD";
	
	
	public static final String LANG_ENGLISH = "ENGLISH";
	
	public static final String LANG_ARABIC = "ARABIC";
	
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
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-tmp.xml";
	
//	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
//			+ "SemEval2015-Task3-English-data/tmp/CQA-QL-devel.xml";
	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-tmp.xml";
	
	//public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
	//		+ "SemEval2015-Task3-English-data/tmp/test_task3_English.xml";
	
	/*public static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-train.xml";
	
	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-devel.xml";*/
	
	public static final String CQA_QL_TRAIN_AR = "semeval2015-3/data/"
			+ "SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-train.xml";
	
	public static final String CQA_QL_DEV_AR = "semeval2015-3/data/"
			+ "SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-dev.xml";
	
	private Set<String> a_labels = new HashSet<>();
	
	private Set<String> b_labels = new HashSet<>();
	
	private FileManager fm;
	
	private AnalysisEngine[] analysisEngineList;
	
	//private JCas preliminaryCas; // Used by the QCRI Arabic pipeline
	
	private PairFeatureFactoryEnglish pfEnglish;
	
	private Alphabet alphabet;
	
	private Stopwords stopwords;
	
	private Analyzer analyzer;
	
	public static void main(String[] args) throws IOException, UIMAException, SimilarityException {
		/**
		 * Setup logger
		 */
		org.apache.log4j.BasicConfigurator.configure();
		
		/**
		 * Run the code for the Arabic task
		 */
		//new AlbertoSimoneBaseline().runForArabic();
		
		/**
		 * Run the code for the English tasks
		 */
		new ArunFeatureExtractor().runForEnglish();
	}
	
	public ArunFeatureExtractor() {
		/**
		 * Default language
		 */
		
		this.fm = new FileManager();
		
		this.alphabet = new Alphabet();
	}
	
	
	public void runForEnglish() throws UIMAException {
		
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
			this.processEnglishFile(CQA_QL_TRAIN_EN, "dev");			
			this.processEnglishFile(CQA_QL_DEV_EN, "test");
		} catch (UIMAException | IOException
				| SimilarityException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Process the xml file and output a csv file with the results in the same directory
	 * @param dataFile the xml file to process
	 * @suffix suffix for identifying the data file 
	 * 
	 * @param suffix
	 * @throws ResourceInitializationException
	 * @throws UIMAException
	 * @throws IOException
	 * @throws AnalysisEngineProcessException
	 * @throws SimilarityException
	 */
	private void processEnglishFile(String dataFile, String suffix)
			throws ResourceInitializationException, UIMAException, IOException,
			AnalysisEngineProcessException, SimilarityException {

		/**
		 * Parameters for matching tree structures
		 */
		String parameterList = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA, RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
		
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
		JCas commentCas = JCasFactory.createJCas();
		
		WriteFile out = new WriteFile(dataFile + ".csv");
		WriteFile outKelp = new WriteFile(dataFile + ".klp");
		
		
		Document doc = Jsoup.parse(new File(dataFile), "UTF-8");
		
		doc.select("QURAN").remove();
		doc.select("HADEETH").remove();
		
		boolean firstRow = true;
		
		/**
		 * Consume data
		 */
		Elements questions = doc.getElementsByTag("Question");		
		int numberOfQuestions = questions.size();
		int questionNumber = 1;
		
		Map<String, Boolean> commentIsDialogue = new HashMap<>();
//		HashSet<String> questionCategories = new HashSet<String>();
		for(Element question : questions) {
			
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
			String qsubject = question.getElementsByTag("QSubject").get(0).text();
			String qbody = question.getElementsByTag("QBody").get(0).text();
			
//			questionCategories.add(qcategory);
			
			q.setQid(qid);
			q.setQcategory(qcategory);
			q.setQdate(qdate);
			q.setQuserId(quserid);
			q.setQtype(qtype);
			q.setQgoldYN(qgold_yn);
			q.setQsubject(qsubject);
			q.setQbody(qbody);
			
			boolean specificQuestionSelection = false;
			if(!q.getQid().equals("Q73") && specificQuestionSelection){
			//if(!q.getQid().equals("Q82") && specificQuestionSelection){
				continue;
			}
			
			/**
			 * Setup question CAS
			 */
			questionCas.reset();
			questionCas.setDocumentLanguage("en");
			questionCas.setDocumentText(qsubject + ". " + qbody);
			
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
			 * Extracting context statistics for Alberto-Simone Features
			 */
			
			
			int commentCounter = 0;
			for(Element comment : comments) {
				String cid = comment.attr("CID");
				String cuserid = comment.attr("CUSERID");
				String cgold = comment.attr("CGOLD");
				String cgold_yn = comment.attr("CGOLD_YN");
				String csubject = comment.getElementsByTag("CSubject").get(0).text();
				String cbody = comment.getElementsByTag("CBody").get(0).text();
				q.addComment(cid, cuserid, cgold, cgold_yn, csubject, cbody);
	
				commentCounter++;
			}
			List<HashMap<String, Double>> albertoSimoneFeatures;
			if(GENERATE_ALBERTO_AND_SIMONE_FEATURES){
				albertoSimoneFeatures = FeatureExtractor.extractFeatures(q);
			}
			
			if(specificQuestionSelection){
				System.out.println("###############################");
				System.out.println("QUESTION (" + q.getQid() + "): " + q.getQbody());
				for(int i=0; i<commentCounter; i++){
					
					HashMap<String, Double> featureVector = albertoSimoneFeatures.get(i);
					String cuserid = q.getComments().get(i).getCuserid();
					
					if(cuserid.equals(quserid)){
						System.out.println("USER: QUESTION AUTHOR");
					}else{
						System.out.println("USER: " + cuserid);
					}
					System.out.println(q.getComments().get(i).getCgold());
					System.out.println("COMMENT BODY: " + q.getComments().get(i).getCbody());
					System.out.println("ACTIVE FEATURES: ");
					for(Entry<String, Double> entry : featureVector.entrySet()){
						System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					System.out.println("------------");
					
					
				}
			}
			
			
			
			
			int commentIndex = 0;
			for(Element comment : comments) {
				String cid = comment.attr("CID");
				String cuserid = comment.attr("CUSERID");
				String cgold = comment.attr("CGOLD");
				
				//Replacing the labels for the "macro" ones: Good vs Bad
				 if(ONLY_BAD_AND_GOOD_CLASSES){
						if(cgold.equalsIgnoreCase("good")){
							cgold = GOOD;
						}else{
							cgold = BAD;
						}
					}
				
				
				String cgold_yn = comment.attr("CGOLD_YN");
				String csubject = comment.getElementsByTag("CSubject").get(0).text();
				String cbody = comment.getElementsByTag("CBody").get(0).text();
				
				
				
				/**
				 * Setup comment CAS
				 */
				commentCas.reset();
				commentCas.setDocumentLanguage("en");
				commentCas.setDocumentText(csubject + ". " + cbody);
				
				/**
				 * Run the UIMA pipeline
				 */
				SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
				
				//this.analyzer.analyze(commentCas, new SimpleContent("c-" + cid, csubject + ". " + cbody));
				
				if(CREATE_KELP_DATASETS){
					String label = "BAD";
					if(cgold.equals("Good")){
						label = cgold;
					}else if(cgold.equals("Potential")){
						label = cgold;
					}
					outKelp.write(label + " ");
				}

				AugmentableFeatureVector fv;
				if(GENERATE_MASSIMO_FEATURES){
					fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(questionCas, commentCas, parameterList);
					if(CREATE_KELP_DATASETS){

						outKelp.write("|BDV:massimo| ");
//						double [] values = fv.getValues();
//						outKelp.write(Double.toString(values[0]));
//						for(int i = 1; i < values.length; i++) {
//
//							out.write("," + values[i]);
//						}
						List<Double> features = this.serializeFv(fv);
						outKelp.write(Joiner.on(",").join(features) + " |EDV| ");
					}
				}else{
					fv = new AugmentableFeatureVector(this.alphabet);
				}

				if(GENERATE_ALBERTO_AND_SIMONE_FEATURES){
				
					HashMap<String, Double> featureVector = albertoSimoneFeatures.get(commentIndex);
					List<Double> features = new ArrayList<Double>();
					
					for(String featureName : FeatureExtractor.getPastFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					
					if(features.size()>0){
						if(CREATE_KELP_DATASETS){
							outKelp.write("|BDV:past|" + Joiner.on(",").join(features) + "|EDV| ");
						}
					}
					features.clear();
					
					for(String featureName : FeatureExtractor.getLsaQuestionFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					
					if(features.size()>0){
						if(CREATE_KELP_DATASETS){
							outKelp.write("|BDV:lsaQuestion|" + Joiner.on(",").join(features) + "|EDV| ");
						}
					}
					features.clear();
					
					for(String featureName : FeatureExtractor.getLsaCommentFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					
					if(features.size()>0){
						if(CREATE_KELP_DATASETS){
							outKelp.write("|BDV:lsaComment|" + Joiner.on(",").join(features) + "|EDV| ");
						}
					}
					features.clear();
					
					
					////
					for(String featureName : FeatureExtractor.getHeuristicFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					
					if(features.size()>0){
						if(CREATE_KELP_DATASETS){
							outKelp.write("|BDV:heuristics|" + Joiner.on(",").join(features) + "|EDV| ");
						}
					}
					features.clear();
					
					////
					
					for(String featureName : FeatureExtractor.getContextFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					if(features.size()>0){
						if(CREATE_KELP_DATASETS){
							outKelp.write("|BDV:albertoSimone|" + Joiner.on(",").join(features) + "|EDV|\n");
						}
					}
					
				}else{
					if(CREATE_KELP_DATASETS){
						outKelp.write("\n");
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
				
				boolean quseridEqCuserid = quserid.equals(cuserid);
				if(quseridEqCuserid) {
					commentIsDialogue.put(cid, true);
				}
				
				//((AugmentableFeatureVector) fv).add("quseridEqCuserid", quseridEqCuserid);
				
				/***************************************
				 * * * * THANKS! * * * *
				 ***************************************/
				
				/**
				 * Produce output line
				 */
				
				if(firstRow) {
					out.write("qid,cgold,cgold_yn");
					for(int i = 0; i < fv.numLocations(); i++) {
						int featureIndex = i + 1;
						out.write(",f" + featureIndex);
					}
					out.write("\n");
					
					firstRow = false;
				}
				
				List<Double> features = this.serializeFv(fv);
	
				out.writeLn(cid + "," + cgold + "," + cgold_yn + "," + Joiner.on(",").join(features));
				
				/**
				 * Produce also the file needed to train structural models
				 */
				if(PRODUCE_SVMLIGHTTK_DATA) {
					produceSVMLightTKExample(questionCas, commentCas, suffix, ts,
						qid, cid, cgold, cgold_yn, features);
				}
                if(PRODUCE_KELP_DATA){
                    produceKelpExample(questionCas, commentCas, suffix, ts,
                                       qid, cid, cgold, cgold_yn, features);
                }
			}
			
			if(specificQuestionSelection){
				System.exit(0);
			}
		}
		
		for(String commentId : commentIsDialogue.keySet()) {
			this.fm.writeLn(dataFile + ".dialogue.txt", commentId);
		}
//		Iterator<String> iterator = questionCategories.iterator();
//		while(iterator.hasNext()){
//			System.out.println("CATEGORY_" + iterator.next());
//		}
		
		this.fm.closeFiles();
		outKelp.close();
		out.close();
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
                                          String suffix, TreeSerializer ts, String qid, String cid,
                                          String cgold, String cgold_yn, List<Double> features) {
        /**
         * Produce output for Kelp
         */
        
        TokenTree questionTree = RichTree.getPosChunkTree(questionCas);
        String questionTreeString = ts.serializeTree(questionTree, RichNode.OUTPUT_PAR_SEMANTIC_KERNEL);
        
        TokenTree commentTree = RichTree.getPosChunkTree(commentCas);
        String commentTreeString = ts.serializeTree(commentTree, RichNode.OUTPUT_PAR_SEMANTIC_KERNEL);
        
            String output = cgold + " ";
            output += "|<||BT:tree| " + questionTreeString.replaceAll("[|]", "-") + " |ET||,||BT:tree| " + commentTreeString.replaceAll("[|]", "-") + " |ET| |>| ";
            
            String featureString = "|BV:features|";
            
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
            
            output += "|BS:info| #" + qid + "\t" + cid + "|ES|";
            
            fm.writeLn("semeval2015-3/svmlighttk/a/" + suffix + "/data"
                       + ".klp",
                       output.trim());
       
        
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
	
//	public JCas getPreliminarCas(Analyzer analyzer, JCas emptyCas,
//			String sentenceId, String sentence) {
//		this.preliminaryCas.reset();
//		
//		/**
//		 * Without this the annotator fails badly
//		 */
//		sentence = sentence.replaceAll("/", "");
//		sentence = sentence.replaceAll("~", "");
//
//		// Carry out preliminary analysis
//		Analyzable content = new SimpleContent(sentenceId, sentence, ArabicAnalyzer.ARABIC_LAN);
//		
//		analyzer.analyze(this.preliminaryCas, content);
//		
//		// Copy data to a new CAS and use normalized text as DocumentText
//		emptyCas.reset();	
//		emptyCas.setDocumentLanguage(ArabicAnalyzer.ARABIC_LAN);
//	
//		CasCopier.copyCas(this.preliminaryCas.getCas(), emptyCas.getCas(), false);
//	
//		String normalizedText = JCasUtil.selectSingle(this.preliminaryCas, NormalizedText.class).getText();
//		emptyCas.setDocumentText(normalizedText);
//
//		return emptyCas;
//	}
}
