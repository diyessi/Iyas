package qa.qcri.qf.semeval2015_3;

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

import qa.qcri.qf.annotators.arabic.ArabicAnalyzer;
import qa.qcri.qf.features.PairFeatureFactory;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.fileutil.WriteFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.type.NormalizedText;
import util.Stopwords;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpChunker;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;


public class Baseline {
	
	public static final String LANG_ENGLISH = "ENGLISH";
	
	public static final String LANG_ARABIC = "ARABIC";
	
	public static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-train.xml";
	
	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-devel.xml";
	
	public static final String CQA_QL_TRAIN_AR = "semeval2015-3/data/"
			+ "SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-train.xml";
	
	public static final String CQA_QL_DEV_AR = "semeval2015-3/data/"
			+ "SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-dev.xml";
	
	public static final String STOPWORDS_EN_PATH = "resources/stoplist-en.txt";
	
	private Set<String> a_labels = new HashSet<>();
	
	private Set<String> b_labels = new HashSet<>();
	
	private String language = "English";
	
	private FileManager fm;
	
	private AnalysisEngine[] analysisEngineList;
	
	private JCas preliminaryCas; // Used by the Arabic pipeline
	
	private PairFeatureFactory pf;
	
	private Alphabet alphabet;
	
	private Stopwords stopwords;
	
	public static void main(String[] args) throws IOException, UIMAException, SimilarityException {
		/**
		 * Setup logger
		 */
		org.apache.log4j.BasicConfigurator.configure();
		
		new Baseline().runForEnglish();
	}
	
	public Baseline() {
		/**
		 * Default language
		 */
		this.language = LANG_ENGLISH;
		
		this.fm = new FileManager();
		
		this.alphabet = new Alphabet();

		this.pf = new PairFeatureFactory(this.alphabet);
		this.pf.setupMeasures(RichNode.OUTPUT_PAR_LEMMA);
	}
	
	public void runForArabic() throws UIMAException {
		this.language = LANG_ARABIC;
		
		this.preliminaryCas = JCasFactory.createJCas();
		
		/**
		 * Specify the task label
		 * For Arabic there is just one task
		 */
		this.a_labels.add("direct");
		this.a_labels.add("related");
		this.a_labels.add("irrelevant");
		
		Analyzer analyzer = new Analyzer(new UIMANoPersistence());
		analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription(ArabicAnalyzer.class)));
		
		AnalysisEngine segmenter = createEngine(createEngineDescription(
				StanfordSegmenter.class, StanfordSegmenter.PARAM_LANGUAGE, "ar"));
		
		this.analysisEngineList = new AnalysisEngine[1];
		this.analysisEngineList[0] = segmenter;
		
		try {
			processArabicFile(analyzer, CQA_QL_TRAIN_AR, "train");
			//processArabicFile(analyzer, CQA_QL_DEV_AR, "dev");
		} catch (SimilarityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
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
	
	public void processArabicFile(Analyzer analyzer, String dataFile, String suffix) throws SimilarityException, UIMAException, IOException {
		/**
		 * QCRI Arabic Analyzer
		 */
		AnalysisEngine arabicAnalyzer = createEngine(createEngineDescription(
				ArabicAnalyzer.class));
		this.analysisEngineList[0] = arabicAnalyzer;
		
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
		for(Element question : questions) {
			/**
			 * Parse question node
			 */
			String qid = question.attr("QID");
			String qcategory = question.attr("QCATEGORY");
			String qdate = question.attr("QDATE");		
			String qsubject = question.getElementsByTag("QSubject").get(0).text();
			String qbody = question.getElementsByTag("QBody").get(0).text();
			
			/**
			 * Get analyzed text for question
			 */
			questionCas.reset();
			questionCas.setDocumentLanguage("ar");
			questionCas.setDocumentText(qbody);
			
			//SimplePipeline.runPipeline(questionCas, this.analysisEngineList);
			
			questionCas = this.getPreliminarCas(analyzer, questionCas, qid, qbody);
			
			/**
			 * Collect question tokens
			 */
			
			List<String> questionTokens = new ArrayList<>();
			for(Token token : JCasUtil.select(questionCas, Token.class)) {
				if(!this.stopwords.contains(token.getCoveredText())) {
					questionTokens.add(token.getCoveredText());
				}
			}
			
			/**
			 * Parse answer nodes
			 */
			Elements comments = question.getElementsByTag("Answer");
			for(Element comment : comments) {
				String cid = comment.attr("CID");
				String cgold = comment.attr("CGOLD");
				String cbody = comment.text();
				
				/**
				 * Get analyzed text for comment
				 */
				commentCas.reset();
				commentCas.setDocumentLanguage("ar");
				commentCas.setDocumentText(cbody);
				
				//SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
				
				commentCas = this.getPreliminarCas(analyzer, commentCas, cid, cbody);		
				
				/**
				 * Collect comment tokens
				 */
				List<String> commentTokens = new ArrayList<>();
				for(Token token : JCasUtil.select(commentCas, Token.class)) {
					if(!this.stopwords.contains(token.getCoveredText())) {
						commentTokens.add(token.getCoveredText());
					}
				}
				
				/**
				 * Compute features between question and comment
				 */
				
				//List<Double> features = this.getSyntacticFeatures(questionTokens, commentTokens);
				List<Double> features = new ArrayList<>();

				/**
				 * Produce output line
				 */
				
				if(firstRow) {
					out.write("cid,cgold");
					for(int i = 0; i < features.size(); i++) {
						int featureIndex = i + 1;
						out.write(",f" + featureIndex);
					}
					out.write("\n");
					
					firstRow = false;
				}
	
				System.out.println(qid + "-" + cid + "," + cgold + "," + Joiner.on(",").join(features));
				
				out.writeLn(qid + "-" + cid + "," + cgold + "," + Joiner.on(",").join(features));
			}
		}
		
		this.fm.closeFiles();
		out.close();
	}
	
	public void runForEnglish() throws ResourceInitializationException {
		
		this.language = LANG_ENGLISH;
		
		this.stopwords = new Stopwords(Stopwords.STOPWORD_EN);
		
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
		
		try {
			this.processEnglishFile(CQA_QL_TRAIN_EN, "train");
			this.processEnglishFile(CQA_QL_DEV_EN, "dev");
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
		marker.useStopwords(STOPWORDS_EN_PATH);
		
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
		
		boolean firstRow = true;
		
		/**
		 * Consume data
		 */
		Elements questions = doc.getElementsByTag("Question");		
		for(Element question : questions) {
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
			
			/**
			 * Collect question tokens
			 */
			List<String> questionLemmas = new ArrayList<>();
			for(Token token : JCasUtil.select(questionCas, Token.class)) {
				String lemma = token.getLemma().getValue();
				if(!this.stopwords.contains(lemma)) {
					questionLemmas.add(lemma);
				}
			}
			
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
				
				/**
				 * Produce question tree
				 */
				TokenTree questionTree = RichTree.getPosChunkTree(questionCas);
				String questionTreeString = ts.serializeTree(questionTree);
				
				/**
				 * Produce comment tree
				 */
				TokenTree commentTree = RichTree.getPosChunkTree(commentCas);
				String commentTreeString = ts.serializeTree(commentTree);
				
				/**
				 * Collect comment lemmas
				 */
				List<String> commentLemmas = new ArrayList<>();
				for(Token token : JCasUtil.select(commentCas, Token.class)) {
					String lemma = token.getLemma().getValue();
					if(!this.stopwords.contains(lemma)) {
						commentLemmas.add(lemma);
					}
				}
				
				FeatureVector fv = pf.getPairFeatures(questionCas, commentCas, parameterList);
				
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
				 * Produce output for SVMLightTK
				 */
				
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
		}

		this.fm.closeFiles();
		out.close();
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
	
	/**
	 * Load stopwords from a file containing one word per line 
	 * @param stopwordsPath the path of the file
	 * @return the stopwords set
	 */
	public static  Set<String> loadStopwords(String stopwordsPath) {
		Set<String> stopwords = new HashSet<>();
		
		ReadFile in = new ReadFile(stopwordsPath);
		while(in.hasNextLine()) {
			String word = in.nextLine().trim();
			stopwords.add(word);
		}
		in.close();
		
		return stopwords;
	}
}
