package qa.qcri.qf.emnlp2015;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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


/**
 * 
 * This class intends to substitute AlbertoSimoneBaseline 
 * @author alberto
 *
 */
public class CommentwiseSimilarity {	

	private static final boolean ONLY_BAD_AND_GOOD_CLASSES = true;
	private static final String GOOD = "GOOD";
	private static final String BAD = "BAD";
	
	/* With this flag and value we limit the number fo comments per question 
	 * to be considered (this intends to reduce the impact of long threads. 
	 * */
	public static final boolean LIMIT_COMMENTS_PER_Q = true;
	public static final int LIMIT_COMMENTS = 20;
	
	public static final String LANG_ENGLISH = "ENGLISH";
			
	public static final boolean USE_QCRI_ALT_TOOLS = false;
	
	private static final String CQA_QL = "semeval2015-3/data/"
		+ "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-train.xml";
	
	private static final String SUFFIX = ".pairsim.csv"; 
	
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
		 * Run the code for the English tasks
		 */
		new CommentwiseSimilarity().runForEnglish();
	}
	
	public CommentwiseSimilarity() {
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
			this.processEnglishFile(CQA_QL);
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
		//TreeSerializer ts = new TreeSerializer().enableRelationalTags().useRoundBrackets();
		
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
		
		//Map<String, Boolean> commentIsDialogue = new HashMap<>();
//		HashSet<String> questionCategories = new HashSet<String>();
		
		double[] matches = new double[11];
		int[] totals = new int[11];
		int bin;
		
		for(Element question : questions) {
			
			Question q = new Question();
			System.out.println("[INFO]: Processing " + questionNumber++ + 
					" out of " + numberOfQuestions);
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
			
			//this.analyzer.analyze(questionCas, new SimpleContent("q-" + qid, qsubject + ". " + qbody));
			
			/**
			 * Parse comment nodes
			 */
			Elements comments = question.getElementsByTag("Comment");
			
			if (LIMIT_COMMENTS_PER_Q && comments.size() >= LIMIT_COMMENTS){
				continue;
			}
			
			for(Element comment : comments) {
				String cid = comment.attr("CID");
				String cuserid = comment.attr("CUSERID");
				String cgold = comment.attr("CGOLD");
				String cgold_yn = comment.attr("CGOLD_YN");
				String csubject = comment.getElementsByTag("CSubject").get(0).text();
				String cbody = comment.getElementsByTag("CBody").get(0).text();
				q.addComment(cid, cuserid, cgold, cgold_yn, csubject, cbody);			
			}

			List<JCas> allCommentsCas = new ArrayList<JCas>();
			List<String> ids = new ArrayList<String>();
			List<String> labels = new ArrayList<String>();
			
			for(Element comment : comments) {
				allCommentsCas.add(computeCommentCas(comment));
				ids.add(comment.attr("CID"));
				labels.add(getgold(comment.attr("CGOLD")));
			}			
			
			
			
			for (int i=0; i< allCommentsCas.size() -1 ; i++){
				for (int j=i+1; j<= allCommentsCas.size() -1; j++){
					AugmentableFeatureVector fv;
					//COMPUTE THE SIMILARITY HERE
					//TODO where to assign this
					//Whether the CAS are exactly identical
					//how to store/display the output
					fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(
							allCommentsCas.get(i), allCommentsCas.get(j), parameterList);
					
//					System.out.println(
//							ids.get(i) + ","+
//							labels.get(i) + ","+
//							ids.get(j)+","+
//							labels.get(j) + ","+
//							Joiner.on(",").join(this.serializeFv(fv))
//					);
					
					bin= (int) Math.round(fv.getValues()[0]*10);
					if (labels.get(i).equals(labels.get(j)))
						matches[bin]++;
					totals[bin]++;
					
					/**
					 * Produce output line
					 */
					
					if(firstRow) {
						out.write("qid,cgold");
						for(int c = 0; c < fv.numLocations(); c++) {
							int featureIndex = c + 1;
							out.write(",f" + featureIndex);
						}
						out.write("\n");
						firstRow=false;
					}
					
					//System.out.println(bin);
					out.writeLn(
							ids.get(i) + "-"+
							ids.get(j) +","+
							labels.get(i) + "-"+							
							labels.get(j) + ","+
							Joiner.on(",").join(this.serializeFv(fv))

					);
				}
			}			
		}
		for (int i=0; i<11;i++)
			System.out.println("BIN: " + i + " pctge: " + matches[i]/totals[i]);
		

		
		this.fm.closeFiles();
		
		out.close();
	}
	
	private JCas computeCommentCas(Element comment) throws UIMAException{
		JCas cCas = JCasFactory.createJCas();
		String cid = comment.attr("CID");
		String cuserid = comment.attr("CUSERID");
		//String cgold = comment.attr("CGOLD");
		//String cgold = getgold(comment.attr("CGOLD"));
		
		//String cgold_yn = comment.attr("CGOLD_YN");
		String csubject = comment.getElementsByTag("CSubject").get(0).text();
		String cbody = comment.getElementsByTag("CBody").get(0).text();
		
		
		
		/**
		 * Setup comment CAS
		 */
		cCas.reset();
		cCas.setDocumentLanguage("en");
		cCas.setDocumentText(csubject + ". " + cbody);
		
		/**
		 * Run the UIMA pipeline
		 */
		SimplePipeline.runPipeline(cCas, this.analysisEngineList);
		
		//this.analyzer.analyze(commentCas, new SimpleContent("c-" + cid, csubject + ". " + cbody));
		return cCas;

	}
	
	public String getgold(String cgold){
		//Replacing the labels for the "macro" ones: Good vs Bad
		if(ONLY_BAD_AND_GOOD_CLASSES){
			if(cgold.equalsIgnoreCase("good")){
				cgold = GOOD;
			}else{
				cgold = BAD;
			}
		}
		return cgold;

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
