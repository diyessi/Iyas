package qa.qcri.qf.emnlp2015;

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
import qa.qcri.qf.semeval2015_3.PairFeatureFactoryEnglish;
import qa.qcri.qf.semeval2015_3.Question;
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


public class ComentwiseFeatureExtractorEx {
	
	private static final boolean GENERATE_MASSIMO_FEATURES = true;
	private static final boolean GENERATE_ALBERTO_AND_SIMONE_FEATURES = true;
	private static final boolean CREATE_KELP_DATASETS = false;
	
	private static final boolean ONLY_BAD_AND_GOOD_CLASSES = true;
	private static final String GOOD = "GOOD";
	private static final String BAD = "BAD";
	
	private static final String LABEL_MATCH = "EQUAL";
	private static final String LABEL_NO_MATCH = "DIFF";
	
	public static final String LANG_ENGLISH = "ENGLISH";
	
	//True if the combination is a concat; false if it is a subtract
	public static final Boolean COMBINATION_CONCAT = false;
	
	public static final Boolean THREE_CLASSES = true;
	
	public static final boolean USE_QCRI_ALT_TOOLS = false;
	
	public static final boolean ALESSANDRO_COMBINATION = true;
	
	/* With this flag and value we limit the number fo comments per question 
	 * to be considered (this intends to reduce the impact of long threads. 
	 * */
	public static final boolean LIMIT_COMMENTS_PER_Q = true;
	public static final int LIMIT_COMMENTS = 20;
	
	public static final String CQA_QL_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-train.xml";
	
	public static final String SUFFIX = ".pairwise.shortened.csv";
	
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
		new ComentwiseFeatureExtractorEx().runForEnglish();
	}
	
	public ComentwiseFeatureExtractorEx() {
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
			this.processEnglishFile(CQA_QL_EN);			

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
	private void processEnglishFile(String dataFile)
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
		
		WriteFile out = new WriteFile(dataFile + SUFFIX);
				
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
			
			List<String> listCid = new ArrayList<String>();
			List<String> listCgold = new ArrayList<String>();
			List<String> listCgold_yn = new ArrayList<String>(); 
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
			
			
			
			if (LIMIT_COMMENTS_PER_Q && comments.size() >= LIMIT_COMMENTS){
				continue;
			}
			
			
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
				albertoSimoneFeatures = ComentwiseFeatureExtractorBack.extractFeatures(q);
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

				AugmentableFeatureVector fv;
				if(GENERATE_MASSIMO_FEATURES){
					fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(questionCas, commentCas, parameterList);
				}else{
					fv = new AugmentableFeatureVector(this.alphabet);
				}

				if(GENERATE_ALBERTO_AND_SIMONE_FEATURES){
				
					HashMap<String, Double> featureVector = albertoSimoneFeatures.get(commentIndex);
					List<Double> features = new ArrayList<Double>();
					
					for(String featureName : ComentwiseFeatureExtractorBack.getPastFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					
					features.clear();
					
					for(String featureName : ComentwiseFeatureExtractorBack.getLsaQuestionFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					
					features.clear();
					
					for(String featureName : ComentwiseFeatureExtractorBack.getLsaCommentFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					
					features.clear();
					
					
					////
					for(String featureName : ComentwiseFeatureExtractorBack.getHeuristicFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
					}
					
					features.clear();
					
					////
					
					for(String featureName : ComentwiseFeatureExtractorBack.getContextFeatureNames()){
						Double value = featureVector.get(featureName);
						double featureValue =0;
						if(value!=null){
							featureValue = value;
						}
						features.add(featureValue);
						fv.add(featureName, featureValue);
						
						//System.out.println(entry.getKey() + ":" + entry.getValue());
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
					out.write("qid,cgold");
					for(int i = 0; i < fv.numLocations(); i++) {
						int featureIndex = i + 1;
						out.write(",f" + featureIndex);
					}
					out.write("\n");
					
					firstRow = false;
				}
				
				List<Double> features = this.serializeFv(fv);
				
				listCid.add(cid);
				listCgold.add(cgold);
				listCgold_yn.add(cgold_yn); 
				listFeatures.add(features);
				
				
			}//END FOR COMMENTS
			
			
			
			if (listCid.size() > 1){//more than one comment; otherwise nothing to do

				//in this case we subtract the features. 
				//as a result, comparing c1,c2 is NOT the same as c2,c1

				StringBuffer sb = new StringBuffer();


				if (ALESSANDRO_COMBINATION) {
					for (int i=0; i < listCid.size() -1 ; i++){						
						for (int j = listCid.size()-1; j>=0; j--){
							if (i==j)
								continue;

							sb.append(listCid.get(i))
							.append("-")
							.append(listCid.get(j))
							.append(",");
							if (listCgold.get(i).equals(listCgold.get(j))){
								sb.append(LABEL_MATCH);
								if (THREE_CLASSES) {
									if (listCgold.get(i).toLowerCase().equals("good")){
										sb.append("_GOOD");
									} else {
										sb.append("_BAD");
									}
								}
							}else { 
								sb.append(LABEL_NO_MATCH);
							}
							
							sb.append(",");

							sb.append(Joiner.on(",").join(
									difference(listFeatures.get(i), listFeatures.get(j))));


							out.writeLn(sb.toString());
							sb.delete(0, sb.length());
						}
					}
				} else {						
					for (int i=0; i < listCid.size() -1 ; i++){						
						for (int j=i+1; j < listCid.size(); j++){
							if (i==j)
								continue;

							sb.append(listCid.get(i))
							.append("-")
							.append(listCid.get(j))
							.append(",");
							if (listCgold.get(i).equals(listCgold.get(j))){
								sb.append(LABEL_MATCH);
							} else { 
								sb.append(LABEL_NO_MATCH);
							}
							sb.append(",");

							if (COMBINATION_CONCAT){
								sb.append(Joiner.on(",").join(
										concatVectors(listFeatures.get(i), listFeatures.get(j))));
							} else {
								sb.append(Joiner.on(",").join(
										absoluteDifference(listFeatures.get(i), listFeatures.get(j))));
							}

							out.writeLn(sb.toString());
							sb.delete(0, sb.length());
						}
					}
					//sb.append("\n");

					//sb.delete(0, sb.length());
					//				out.writeLn(cid + "," + cgold + "," + cgold_yn + "," + Joiner.on(",").join(features));

				}
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
		out.close();
	}

	
	private List<Double> difference(List<Double> v1, List<Double> v2){
		if (v1.size() != v2.size()) {
			System.err.println("Vectors size mismatch");
			System.exit(1);
		}
		List<Double> diff = new ArrayList<Double>();
		for (int i=0; i < v1.size(); i++){
			diff.add(v1.get(i)-v2.get(i));
		}		
		return diff;		
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
