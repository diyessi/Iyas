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
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.fileutil.WriteFile;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpChunker;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.WordNGramContainmentMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.WordNGramJaccardMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.GreedyStringTiling;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.JaroSecondStringComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubsequenceComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubsequenceNormComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubstringComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.MongeElkanSecondStringComparator;


public class Baseline {
	
	public static final String CQA_QL_TRAIN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-train.xml";
	
	public static final String CQA_QL_DEV = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-devel.xml";

	public static final Set<String> STOPWORDS = loadStopwords("resources/stoplist-en.txt");
	
	public static void main(String[] args) throws IOException, UIMAException, SimilarityException {
		
		/**
		 * Add some punctuation to the stopwords list
		 */
		STOPWORDS.add(".");
		STOPWORDS.add("...");
		STOPWORDS.add("\"");
		STOPWORDS.add(",");
		STOPWORDS.add("?");
		STOPWORDS.add("!");
		STOPWORDS.add("#");
		STOPWORDS.add("(");
		STOPWORDS.add(")");
		STOPWORDS.add("$");
		STOPWORDS.add("%");
		STOPWORDS.add("&");
		
		/**
		 * Setup logger
		 */
		org.apache.log4j.BasicConfigurator.configure();
		
		processFile(CQA_QL_TRAIN);
		processFile(CQA_QL_DEV);
	}

	/**
	 * Process the xml file and output a csv file with the results in the same directory
	 * @param dataFile the xml file to process
	 * 
	 * @throws ResourceInitializationException
	 * @throws UIMAException
	 * @throws IOException
	 * @throws AnalysisEngineProcessException
	 * @throws SimilarityException
	 */
	private static void processFile(String dataFile)
			throws ResourceInitializationException, UIMAException, IOException,
			AnalysisEngineProcessException, SimilarityException {
		
		/**
		 * Create analysis pipeline
		 */
		
		AnalysisEngine segmenter = createEngine(createEngineDescription(OpenNlpSegmenter.class));
		AnalysisEngine postagger = createEngine(createEngineDescription(OpenNlpPosTagger.class));
		AnalysisEngine chunker = createEngine(createEngineDescription(OpenNlpChunker.class));
		AnalysisEngine lemmatizer = createEngine(createEngineDescription(StanfordLemmatizer.class));
		
		
		/**
		 * Instantiate CASes
		 */
		JCas questionCas = JCasFactory.createJCas();
		JCas commentCas = JCasFactory.createJCas();
		
		WriteFile out = new WriteFile(dataFile + ".csv");
		
		Document doc = Jsoup.parse(new File(dataFile), "UTF-8");
		
		out.writeLn("qid,cgold,cgold_yn,f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13");
		
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
			questionCas.setDocumentText(qbody);
			
			/**
			 * Run the UIMA pipeline
			 */
			SimplePipeline.runPipeline(questionCas, segmenter, postagger,
					chunker, lemmatizer);
			
			/**
			 * Collect question tokens
			 */
			List<String> questionLemmas = new ArrayList<>();
			for(Token token : JCasUtil.select(questionCas, Token.class)) {
				String lemma = token.getLemma().getValue();
				if(!STOPWORDS.contains(lemma)) {
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
				commentCas.setDocumentText(cbody);
				
				/**
				 * Run the UIMA pipeline
				 */
				SimplePipeline.runPipeline(commentCas, segmenter, postagger,
						chunker, lemmatizer);
				
				/**
				 * Collect comment lemmas
				 */
				List<String> commentLemmas = new ArrayList<>();
				for(Token token : JCasUtil.select(commentCas, Token.class)) {
					String lemma = token.getLemma().getValue();
					if(!STOPWORDS.contains(lemma)) {
						commentLemmas.add(lemma);
					}
				}
				
				/**
				 * Compute features between question and comment
				 */
				
				List<Double> features = new ArrayList<>();			
				features.add(new WordNGramJaccardMeasure(1).getSimilarity(questionLemmas, commentLemmas));
				features.add(new WordNGramJaccardMeasure(2).getSimilarity(questionLemmas, commentLemmas));
				features.add(new WordNGramJaccardMeasure(3).getSimilarity(questionLemmas, commentLemmas));
				features.add(new WordNGramContainmentMeasure(1).getSimilarity(questionLemmas, commentLemmas));
				features.add(new WordNGramContainmentMeasure(2).getSimilarity(questionLemmas, commentLemmas));		
				features.add(new GreedyStringTiling(3).getSimilarity(questionLemmas, commentLemmas));
				features.add(new LongestCommonSubsequenceComparator().getSimilarity(questionLemmas, commentLemmas));
				features.add(new LongestCommonSubsequenceNormComparator().getSimilarity(questionLemmas, commentLemmas));
				features.add(new LongestCommonSubstringComparator().getSimilarity(questionLemmas, commentLemmas));			
				features.add(new JaroSecondStringComparator().getSimilarity(questionLemmas, commentLemmas));
				features.add(new MongeElkanSecondStringComparator().getSimilarity(questionLemmas, commentLemmas));
				features.add(new CosineSimilarity().getSimilarity(questionLemmas, commentLemmas));
				features.add(new JaroSecondStringComparator().getSimilarity(questionLemmas, commentLemmas));
				
				/**
				 * Produce output line
				 */
	
				out.writeLn(cid + "," + cgold + "," + cgold_yn + "," + Joiner.on(",").join(features));
			}
		}

		out.close();
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
