package qa.qcri.qf.semeval2015_3;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import qa.qcri.qf.annotators.WhitespaceTokenizer;
import qa.qcri.qf.annotators.arabic.ArabicAnalyzer;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.fileutil.WriteFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import qa.qcri.qf.trees.pruning.PosChunkLeavesPruner;
import qa.qcri.qf.trees.pruning.strategies.PruneIfParentIsWithoutMetadata;
import qa.qcri.qf.type.NormalizedText;
import util.Stopwords;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpChunker;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;


public class BaselineB {
	
	public static final String LANG_ENGLISH = "ENGLISH";
	
	public static final String LANG_ARABIC = "ARABIC";
	
	/**
	 * Set this option to true if you want to produce also data for
	 * SVMLightTK in order to train ad structural model with trees
	 * and feature vectors.
	 */
	public static final boolean PRODUCE_SVMLIGHTTK_DATA = true;
	
	public static final boolean USE_QCRI_ALT_TOOLS = false;
	
	/* Iman */
	public static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-train.xml";
	public static final String CQA_QL_TRAIN_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-train_dev.xml";
	public static final String CQA_QL_TEST_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/test_task3_English.xml";
	/***/
	
	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-devel.xml";
	
	public static final String CQA_QL_TRAIN_AR = "semeval2015-3/data/"
			+ "SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-train.xml";
	
	public static final String CQA_QL_DEV_AR = "semeval2015-3/data/"
			+ "SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-dev.xml";
	
	private Set<String> a_labels = new HashSet<>();
	
	private Set<String> b_labels = new HashSet<>();
	
	private String language = "English";
	
	private FileManager fm;
	
	private AnalysisEngine[] analysisEngineList;
	
	private JCas preliminaryCas; // Used by the QCRI Arabic pipeline
	
	private PairFeatureFactoryArabic pfArabic;
	
	private PairFeatureFactoryEnglish pfEnglish;
	
	private Alphabet alphabet;
	
	private Stopwords stopwords;
	
	private Analyzer analyzer;
	
	/* Iman */
	private HashMap<String, String[]> userCommentsStats;
	private HashMap<String, String[]> sentimentLexicon;
	private HashMap<String, Integer> IDF;
	private int iTotalDocs;
	/**/
	
	public static void main(String[] args) throws IOException, UIMAException, SimilarityException {
		/**
		 * Setup logger
		 */
		org.apache.log4j.BasicConfigurator.configure();
		
		/**
		 * Run the code for the Arabic task
		 */
		//new Baseline().runForArabic();
		
		/**
		 * Run the code for the English tasks
		 */
		new Baseline().runForEnglish();
	}
	
	public BaselineB() {
		/**
		 * Default language
		 */
		this.language = LANG_ENGLISH;
		
		this.fm = new FileManager();
		
		this.alphabet = new Alphabet();
	}
	
	public void runForArabic() throws UIMAException {	
		this.stopwords = new Stopwords(Stopwords.STOPWORD_AR);
		this.stopwords = new Stopwords("semeval2015-3/arabic-corpus-specific-stopwords.txt");
		
		this.pfArabic = new PairFeatureFactoryArabic(this.alphabet);
		this.pfArabic.setupMeasures(RichNode.OUTPUT_PAR_TOKEN_LOWERCASE, this.stopwords);
		
		this.language = LANG_ARABIC;
		
		this.preliminaryCas = JCasFactory.createJCas();
		
		/**
		 * Specify the task label
		 * For Arabic there is just one task
		 */
		this.a_labels.add("direct");
		this.a_labels.add("related");
		this.a_labels.add("irrelevant");
		
		/**
		 * Instantiate the QCRI Analyzer, but for now we are
		 * using the analysis engines instantiated later on
		 */
		if(USE_QCRI_ALT_TOOLS) {
			this.analyzer = new Analyzer(new UIMANoPersistence());
			analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription(ArabicAnalyzer.class)));
		} else {	
			/**
			 * Whitespace tokenizer. The Stanford Segmenter for Arabic
			 * has a very bad bug and the tokenization is completely wrong.
			 */
			AnalysisEngine segmenter = createEngine(createEngineDescription(
				WhitespaceTokenizer.class));	
			/**
			 * Stanford POS-Tagger
			 */
			AnalysisEngine postagger = createEngine(createEngineDescription(
				StanfordPosTagger.class, StanfordPosTagger.PARAM_LANGUAGE, "ar",
				StanfordPosTagger.PARAM_VARIANT, "accurate"));
			/**
			 * Putting together the UIMA DKPro annotators
			 */
			this.analysisEngineList = new AnalysisEngine[2];
			this.analysisEngineList[0] = segmenter;
			this.analysisEngineList[1] = postagger;
		}
		
		try {
			processArabicFile(analyzer, CQA_QL_TRAIN_AR, "train");
			processArabicFile(analyzer, CQA_QL_DEV_AR, "dev");
		} catch (SimilarityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void processArabicFile(Analyzer analyzer, String dataFile, String suffix) throws SimilarityException, UIMAException, IOException {	
		/**
		 * We do not have a lemmatizer so we work with tokens
		 */
		String parameterList = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
		
		/**
		 * Instantiate CASes
		 */
		JCas questionCas = JCasFactory.createJCas();
		JCas commentCas = JCasFactory.createJCas();
		
		WriteFile out = new WriteFile(dataFile + ".csv");
		
		Document doc = Jsoup.parse(new File(dataFile), "UTF-8");
		
		boolean firstRow = true;
		
		/**
		 * Consume data
		 */
		Elements questions = doc.getElementsByTag("Question");
		
		int numberOfQuestions = questions.size();
		int questionNumber = 1;
		
		for(Element question : questions) {
			System.out.println("[INFO]: Processing " + questionNumber++ + " out of " + numberOfQuestions);
			/**
			 * Parse question node
			 */
			String qid = question.attr("QID");
			String qcategory = question.attr("QCATEGORY");
			String qdate = question.attr("QDATE");		
			String qsubject = question.getElementsByTag("QSubject").get(0).text()
					.replaceAll("/", "")
					.replaceAll("~", "");
			String qbody = question.getElementsByTag("QBody").get(0).text()
					.replaceAll("/", "")
					.replaceAll("~", "");
			
			/**
			 * Get analyzed text for question
			 */
			if(USE_QCRI_ALT_TOOLS) {
				questionCas = this.getPreliminarCas(analyzer, questionCas, qid, qsubject + ". " + qbody);
			} else {
				questionCas.reset();
				questionCas.setDocumentLanguage("ar");
				questionCas.setDocumentText(qsubject + ". " + qbody);				
				SimplePipeline.runPipeline(questionCas, this.analysisEngineList);
			}
			
			/**
			 * Parse answer nodes
			 */
			Elements comments = question.getElementsByTag("Answer");
			for(Element comment : comments) {
				String cid = comment.attr("CID");
				String cgold = comment.attr("CGOLD");
				String cbody = comment.text()
						.replaceAll("/", "")
						.replaceAll("~", "");;
				
				/**
				 * Get analyzed text for comment
				 */
				if(USE_QCRI_ALT_TOOLS) {
					commentCas = this.getPreliminarCas(analyzer, commentCas, cid, cbody);
				} else {
					commentCas.reset();
					commentCas.setDocumentLanguage("ar");
					commentCas.setDocumentText(cbody);
					
					SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
				}
				
				/**
				 * Compute features between question and comment
				 */
				
				FeatureVector fv = pfArabic.getPairFeatures(questionCas, commentCas, parameterList);
				
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
						
				/***************************************
				 * * * * THANKS! * * * *
				 ***************************************/
				
				/**
				 * Produce output line
				 */
				
				if(firstRow) {
					out.write("cid,cgold");
					for(int i = 0; i < fv.numLocations(); i++) {
						int featureIndex = i + 1;
						out.write(",f" + featureIndex);
					}
					out.write("\n");
					
					firstRow = false;
				}
				
				List<Double> features = this.serializeFv(fv);
	
				/**
				 * Produce output line
				 */
				
				out.writeLn(qid + "-" + cid + "," + cgold + "," + Joiner.on(",").join(features));
			}
		}
		
		this.fm.closeFiles();
		out.close();
	}
	
	public void runForEnglish() throws UIMAException, IOException {
		
		this.stopwords = new Stopwords(Stopwords.STOPWORD_EN);
		
		this.pfEnglish = new PairFeatureFactoryEnglish(this.alphabet);
		this.pfEnglish.setupMeasures(RichNode.OUTPUT_PAR_LEMMA, this.stopwords);
		
		this.language = LANG_ENGLISH;
		
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
		
		/*Iman*/
		loadSentimentLexicon();
		loadIDF();
		/**/
		
		this.analyzer = new Analyzer(new UIMAFilePersistence("CASes/semeval"));
		for(AnalysisEngine ae : this.analysisEngineList) {
			analyzer.addAE(ae);
		}
		
		try {
			/* Iman */
			loadUserMetaData("train");
			this.processEnglishFile(CQA_QL_TRAIN_EN, "train");
			this.processEnglishFile(CQA_QL_DEV_EN, "dev");
			
			loadUserMetaData("train_dev");
			this.processEnglishFile(CQA_QL_TRAIN_DEV_EN, "traindev");
			this.processEnglishFile(CQA_QL_TEST_EN, "test");
		
		} catch (UIMAException | IOException
				| SimilarityException e) {
			e.printStackTrace();
		}
	}

	/* Iman */
	private void loadUserMetaData(String train) throws IOException{
		String[] saTokens;
		BufferedReader br = new BufferedReader(new FileReader(new File("semeval2015-3/data/"
		+ "SemEval2015-Task3-English-data/datasets/CQA-QL-"+ train +".xml_user_metadata")));
		this.userCommentsStats = new HashMap<String, String[]>();
		String sLine="";
		
		while((sLine=br.readLine())!=null)
		{
			saTokens = sLine.split("\t");
			this.userCommentsStats.put(saTokens[0], saTokens);
		}
		br.close();		
	}
	
	private void loadIDF() throws IOException{
		String[] saTokens;
		BufferedReader br = new BufferedReader(new FileReader(new File("/media/iman/Local Disk/Documents/CQA/IDF2.txt")));
		this.IDF = new HashMap<String, Integer>();
		String sLine="";
		iTotalDocs = Integer.parseInt(br.readLine());
		
		while((sLine=br.readLine())!=null)
		{
			saTokens = sLine.split("\t");
			this.IDF.put(saTokens[0].toLowerCase(), Integer.parseInt(saTokens[1]));
		}
		br.close();		
	}
	
	private void loadSentimentLexicon() throws IOException{
		String[] saTokens;
		BufferedReader br = new BufferedReader(new FileReader(new File("/media/iman/Local Disk/semEval/" +
				"NRC-Hashtag-Sentiment-Lexicon-v0.1/NRC-Hashtag-Sentiment-Lexicon-v0.1/unigrams-pmilexicon.txt")));
		this.sentimentLexicon = new HashMap<String, String[]>();
		String sLine="";
		
		while((sLine=br.readLine())!=null)
		{
			saTokens = sLine.replace("#", "").replace("@", "").split("\t");
			this.sentimentLexicon.put(saTokens[0], saTokens);
		}
		
		br.close();
	}
	
	/***/
	
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
		
		Document doc = Jsoup.parse(new File(dataFile), "UTF-8");
		
		doc.select("QURAN").remove();
		doc.select("HADEETH").remove();
		
		boolean firstRow = true;
		String[] saTokens;
		
		/**
		 * Consume data
		 */
		Elements questions = doc.getElementsByTag("Question");		
		int numberOfQuestions = questions.size();
		int questionNumber = 1;
		
		/* Iman */
		
		int commentNumber = 0;
		Double numOfUserComments = 0.0;
		Double numOfGUserCommentsInCat = 0.0;
		Double numOfBUserCommentsInCat = 0.0;
		Double numOfPUserCommentsInCat = 0.0;
		
		Double sentiScore = 0.0;
		
		Double tmp = 0.0;
		Double tfidf = 0.0;
		Double qNorm = 0.0;
		Double cNorm = 0.0;
		Double clength = 0.0;
		Double qlength = 0.0;
		
		HashMap<String,Integer> hmCWords = new HashMap<String,Integer>();
		HashMap<String,Integer> hmQWords = new HashMap<String,Integer>();
		
		String[] qWords;
		String[] cWords;
		
		ArrayList<Double> qTFIDF = new ArrayList<Double>();
		ArrayList<Double> cTFIDF = new ArrayList<Double>();
		
		TokenTree commentTree;
		List<RichTokenNode> commentTokens;
		TokenTree questionTree;
		List<RichTokenNode> questionTokens;
		/**/
		Map<String, Boolean> commentIsDialogue = new HashMap<>();
		
		for(Element question : questions) {
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
			
			commentNumber = 0;
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
			questionTree = RichTree.getPosChunkTree(questionCas);
			questionTokens = questionTree.getTokens();
			 
			//this.analyzer.analyze(questionCas, new SimpleContent("q-" + qid, qsubject + ". " + qbody));
			
			/**
			 * Parse comment nodes
			 */
			Elements comments = question.getElementsByTag("Comment");
			for(Element comment : comments) {
				String cid = comment.attr("CID");
				String cuserid = comment.attr("CUSERID");
				String cgold = comment.attr("CGOLD");
				String cgold_yn = comment.attr("CGOLD_YN");
				String csubject = comment.getElementsByTag("CSubject").get(0).text();
				String cbody = comment.getElementsByTag("CBody").get(0).text();
				commentNumber++;
				/**
				 * Setup comment CAS
				 */
				commentCas.reset();
				commentCas.setDocumentLanguage("en");
				
				/* Iman */
				/*if(csubject.trim().startsWith("RE:")) {
                    commentCas.setDocumentText(cbody);
                } else {*/
                    commentCas.setDocumentText(csubject + ". " + cbody);                    
                //}
				/**/
		
                /**
				 * Run the UIMA pipeline
				 */
				SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
				
				commentTree = RichTree.getPosChunkTree(commentCas);
				commentTokens = commentTree.getTokens();
				
				//this.analyzer.analyze(commentCas, new SimpleContent("c-" + cid, csubject + ". " + cbody));
								
				FeatureVector fv = pfEnglish.getPairFeatures(questionCas, commentCas, parameterList);
				
				/***************************************
				 * * * * PLUG YOUR FEATURES HERE * * * *
				 ***************************************/
				/*Iman*/
				
				//------------ Sentiment Info ------------
				sentiScore = 0.0;
				
				for(RichTokenNode token:commentTokens)
						//.replaceAll("http(s?)://[^\\s]+?", "")
						//.replaceAll("&[^\\s]+?;", "")
						//.split("[^a-zA-Z0-9]"))
				{
					if(!token.getValue().startsWith("http")
							&& !token.getValue().matches("&[^\\s]+?;")
							&& !this.stopwords.contains(token.getValue())
							&& token.getValue().matches("[a-zA-Z]+"))
					{
						saTokens = sentimentLexicon.get(token.getValue());
						// word, sentiment score, number of co occurrences with +ve marker, 
						// number of co occurrences with -ve marker
						if(saTokens != null)
						{
							tmp = Double.parseDouble(saTokens[1]);
							//System.out.println(token + " # " + tmp);
							if(tmp>1.0 || tmp<-1.0)	//ABC: words close to neutral are neglected
								sentiScore += tmp; 	//ABC: Should we take an average at the end?; A normalization for all the sentence in range(-1,1)?
													//ABC: what happened to the coming two features?
							//positiveCoOccurrence += Double.parseDouble(saTokens[2]);
							//negativeCoOccurrence += Double.parseDouble(saTokens[3]);
						}
					}
				} 
				((AugmentableFeatureVector) fv).add("senti_score", sentiScore);
				
				//------------ TF-IDF -------------
				hmCWords.clear();
				hmQWords.clear();
				qTFIDF.clear();
				cTFIDF.clear();
				tfidf = 0.0;
				qNorm = 0.0;
				cNorm = 0.0;
				clength = 0.0;
				qlength = 0.0;
				
				//cWords = cbody.toLowerCase().split("[^a-zA-Z]+",0);	//ABC: why not using the segmenter output?
				for(RichTokenNode token:commentTokens)
				{
					if(!stopwords.contains(token.getValue())
							&& token.getValue().matches("[a-zA-Z]+"))
					{
						clength++;
						if(hmCWords.containsKey(token.getValue()))
							hmCWords.put(token.getValue(), hmCWords.get(token.getValue())+1);
						else
							hmCWords.put(token.getValue(), 1);
					}
				}
				
				//qWords = qbody.toLowerCase().split("[^a-zA-Z]+",0);
				for(RichTokenNode token:questionTokens)
				{
					if(!stopwords.contains(token.getValue())
							&& token.getValue().matches("[a-zA-Z]+"))
					{
						qlength++;
						if(hmQWords.containsKey(token.getValue()))
							hmQWords.put(token.getValue(), hmQWords.get(token.getValue())+1);
						else
							hmQWords.put(token.getValue(), 1);
					}
				}
				
				for(RichTokenNode token:questionTokens)
				{
					if(hmCWords.containsKey(token.getValue()))
					{
						if(IDF.containsKey(token.getValue())){
							//ABC: I think this should be 
							cTFIDF.add((hmCWords.get(token.getValue())/clength)*((Math.log((double)iTotalDocs/(1+IDF.get(token.getValue()))))));
							qTFIDF.add((hmQWords.get(token.getValue())/qlength)*((Math.log((double)iTotalDocs/(1+IDF.get(token.getValue()))))));
						}
						else{
							//ABC: check the use of this delta. The code shouldn't enter this else
							cTFIDF.add((hmCWords.get(token.getValue())/clength)*((Math.log((double)iTotalDocs))));
							qTFIDF.add((hmQWords.get(token.getValue())/qlength)*((Math.log((double)iTotalDocs))));
						}
					}
				}
				
				if(cTFIDF.size() != qTFIDF.size())
				{
					System.err.println("Error");
				}
				//ABC: cosine computation
				for(int i=0; i<cTFIDF.size() ; i++)
				{
					tfidf += cTFIDF.get(i)*qTFIDF.get(i);
					qNorm += Math.pow(qTFIDF.get(i), 2);
					cNorm += Math.pow(cTFIDF.get(i), 2);
				}
				qNorm = Math.sqrt(qNorm);
				cNorm = Math.sqrt(cNorm);
				
				if(qNorm!=0 && cNorm!=0)
					((AugmentableFeatureVector) fv).add("tfidf", tfidf/(qNorm*cNorm));
				else
					((AugmentableFeatureVector) fv).add("tfidf", 0.0);
				//((AugmentableFeatureVector) fv).add("overlap", cTFIDF.size());
				
				//------------ Lexical Info --------------
				//ABC: this could be "cannot" or "cannes", november, "nobody", "notice"!!!
				//THIS IS COMPLETELY MISTAKEN
				if(containsIgnoreCase(commentTokens, "yes") 
						|| containsIgnoreCase(commentTokens, "can")
						|| containsIgnoreCase(commentTokens, "sure") 
						|| containsIgnoreCase(commentTokens, "wish")
						|| containsIgnoreCase(commentTokens, "would"))
					((AugmentableFeatureVector) fv).add("yes", 1.0);
				else
					((AugmentableFeatureVector) fv).add("yes", 0.0);
				
				if(containsIgnoreCase(commentTokens, "no") 
						|| containsIgnoreCase(commentTokens, "not")
						|| containsIgnoreCase(commentTokens, "neither"))
					((AugmentableFeatureVector) fv).add("no", 1.0);
				else
					((AugmentableFeatureVector) fv).add("no", 0.0);
				
				if(containsIgnoreCase(commentTokens, "http"))
					((AugmentableFeatureVector) fv).add("url", 1.0);
				else
					((AugmentableFeatureVector) fv).add("url", 0.0);
				
				((AugmentableFeatureVector) fv).add("comment_len", cbody.split("\\s").length);
				((AugmentableFeatureVector) fv).add("comment_order", 1.0/(double)commentNumber);
				
				//------------- User Info --------------
				
				if(cuserid.compareTo(quserid)!=0)
					((AugmentableFeatureVector) fv).add("asker_not_answerer", 1.0);
				else
					((AugmentableFeatureVector) fv).add("asker_not_answerer", 0.0);
				
				
				if(userCommentsStats.containsKey(cuserid))
				{
					numOfUserComments = Double.parseDouble(userCommentsStats.get(cuserid)[1])
							+ Double.parseDouble(userCommentsStats.get(cuserid)[3])
							+ Double.parseDouble(userCommentsStats.get(cuserid)[5])
							+ Double.parseDouble(userCommentsStats.get(cuserid)[7]);
					
					((AugmentableFeatureVector) fv).add("ugcomment_num", Double.parseDouble(userCommentsStats.get(cuserid)[1]));
					((AugmentableFeatureVector) fv).add("ugcomment_avg_len", Double.parseDouble(userCommentsStats.get(cuserid)[2]));
					((AugmentableFeatureVector) fv).add("ubcomment_num", Double.parseDouble(userCommentsStats.get(cuserid)[3]));
					((AugmentableFeatureVector) fv).add("ubcomment_avg_len", Double.parseDouble(userCommentsStats.get(cuserid)[4]));
					((AugmentableFeatureVector) fv).add("upcomment_num", Double.parseDouble(userCommentsStats.get(cuserid)[5]));
					((AugmentableFeatureVector) fv).add("upcomment_avg_len", Double.parseDouble(userCommentsStats.get(cuserid)[6]));
					((AugmentableFeatureVector) fv).add("udcomment_num", Double.parseDouble(userCommentsStats.get(cuserid)[7]));
					((AugmentableFeatureVector) fv).add("udcomment_avg_len", Double.parseDouble(userCommentsStats.get(cuserid)[8]));
					
					((AugmentableFeatureVector) fv).add("ucomment_num", numOfUserComments);
					
					// numOfUserCommentsInCat=0.0;
					
					// userCommentsStats.get(cuserid)[9] good
					
					if(userCommentsStats.get(cuserid)[9].startsWith(qcategory)
							|| userCommentsStats.get(cuserid)[9].contains("#"+qcategory+"#"))
					{
						saTokens = userCommentsStats.get(cuserid)[9].split("#");
						for(int i=0; i<saTokens.length; i+=2)
						{	
							if(saTokens[i].compareTo(qcategory)==0)
							{
								numOfGUserCommentsInCat = Double.parseDouble(saTokens[i+1]);
								//numOfUserCommentsInCat+=Double.parseDouble(saTokens[i+1]);
								break;
							}
						}
					}
					else
						numOfGUserCommentsInCat = 0.0;
					
					// userCommentsStats.get(cuserid)[10] potential
					if(userCommentsStats.get(cuserid)[10].startsWith(qcategory)
							|| userCommentsStats.get(cuserid)[10].contains("#"+qcategory+"#"))
					{
						saTokens = userCommentsStats.get(cuserid)[10].split("#");
						for(int i=0; i<saTokens.length; i+=2)
						{	
							if(saTokens[i].compareTo(qcategory)==0)
							{
								numOfPUserCommentsInCat = Double.parseDouble(saTokens[i+1]);
								//numOfUserCommentsInCat+=Double.parseDouble(saTokens[i+1]);
								break;
							}
						}
					}
					else
						numOfPUserCommentsInCat = 0.0;
					
					// userCommentsStats.get(cuserid)[11] bad
					
					if(userCommentsStats.get(cuserid)[11].startsWith(qcategory)
							|| userCommentsStats.get(cuserid)[11].contains("#"+qcategory+"#"))
					{
						saTokens = userCommentsStats.get(cuserid)[11].split("#");
						for(int i=0; i<saTokens.length; i+=2)
						{	
							if(saTokens[i].compareTo(qcategory)==0)
							{
								numOfBUserCommentsInCat = Double.parseDouble(saTokens[i+1]);
								//numOfUserCommentsInCat+=Double.parseDouble(saTokens[i+1]);
								break;
							}
						}
					}
					else
						numOfBUserCommentsInCat = 0.0;
					
					
					((AugmentableFeatureVector) fv).add("cat_user_bad", numOfBUserCommentsInCat);
					((AugmentableFeatureVector) fv).add("cat_user_potential", numOfPUserCommentsInCat);
					((AugmentableFeatureVector) fv).add("cat_user_good", numOfGUserCommentsInCat);
					
					// userCommentsStats.get(cuserid)[12] dialog
				}
				else{
					((AugmentableFeatureVector) fv).add("ugcomment_num", 0.0);
					((AugmentableFeatureVector) fv).add("ugcomment_avg_len", 0.0);
					((AugmentableFeatureVector) fv).add("ubcomment_num", 0.0);
					((AugmentableFeatureVector) fv).add("ubcomment_avg_len", 0.0);
					((AugmentableFeatureVector) fv).add("upcomment_num", 0.0);
					((AugmentableFeatureVector) fv).add("upcomment_avg_len", 0.0);
					((AugmentableFeatureVector) fv).add("udcomment_num", 0.0);
					((AugmentableFeatureVector) fv).add("udcomment_avg_len", 0.0);
					
					((AugmentableFeatureVector) fv).add("ucomment_num", 0.0);
					
					((AugmentableFeatureVector) fv).add("cat_user_good", 0.0);
					((AugmentableFeatureVector) fv).add("cat_user_potential", 0.0);
					((AugmentableFeatureVector) fv).add("cat_user_bad", 0.0);
					//((AugmentableFeatureVector) fv).add("cat_user_dialog", 0.0);
				}
				
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
			}			
		}
		
		for(String commentId : commentIsDialogue.keySet()) {
			this.fm.writeLn(dataFile + ".dialogue.txt", commentId);
		}

		this.fm.closeFiles();
		out.close();
	}

	public boolean containsIgnoreCase(List <RichTokenNode> l, String s){
		Iterator <RichTokenNode> it = l.iterator();
		while(it.hasNext()){
			if(it.next().getValue().equalsIgnoreCase(s))
			return true;
		}
		return false;
	}
	
	private void produceSVMLightTKExample(JCas questionCas, JCas commentCas,
			String suffix, TreeSerializer ts, String qid, String cid,
			String cgold, String cgold_yn, List<Double> features) throws IOException {
		/**
		 * Produce output for SVMLightTK
		 */
		/* Iman */
		PosChunkLeavesPruner pruner;
		Function<List<RichNode>, List<Boolean>> pruningCriteria = new PruneIfParentIsWithoutMetadata(RichNode.REL_KEY);
		
		MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(
                new MarkTwoAncestors()).useStopwords(stopwords.STOPWORD_EN);
		/**/
		TokenTree questionTree = RichTree.getPosChunkTree(questionCas);
		TokenTree commentTree = RichTree.getPosChunkTree(commentCas);
		
		/*
		if(questionTree.getTokens().size() > 10)
			pruner = new PosChunkLeavesPruner(2);
		else
			pruner = new PosChunkLeavesPruner(3);
		
		marker.markTrees(questionTree, commentTree, RichNode.OUTPUT_PAR_LEMMA);
		*/
		
		//RichNode qnode = pruner.prune(questionTree, pruningCriteria);
		String questionTreeString = ts.serializeTree(questionTree);
		//String questionTreeString = ts.serializeTree(qnode,RichNode.OUTPUT_PAR_SEMANTIC_KERNEL);
		
		//RichNode cnode = pruner.prune(commentTree, pruningCriteria);
		String commentTreeString = ts.serializeTree(commentTree);
		//String commentTreeString = ts.serializeTree(cnode,RichNode.OUTPUT_PAR_SEMANTIC_KERNEL);
		
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
	
	public JCas getPreliminarCas(Analyzer analyzer, JCas emptyCas,
			String sentenceId, String sentence) {
		this.preliminaryCas.reset();
		
		/**
		 * Without this the annotator fails badly
		 */
		sentence = sentence.replaceAll("/", "");
		sentence = sentence.replaceAll("~", "");

		// Carry out preliminary analysis
		Analyzable content = new SimpleContent(sentenceId, sentence, ArabicAnalyzer.ARABIC_LAN);
		
		analyzer.analyze(this.preliminaryCas, content);
		
		// Copy data to a new CAS and use normalized text as DocumentText
		emptyCas.reset();	
		emptyCas.setDocumentLanguage(ArabicAnalyzer.ARABIC_LAN);
	
		CasCopier.copyCas(this.preliminaryCas.getCas(), emptyCas.getCas(), false);
	
		String normalizedText = JCasUtil.selectSingle(this.preliminaryCas, NormalizedText.class).getText();
		emptyCas.setDocumentText(normalizedText);

		return emptyCas;
	}
}
