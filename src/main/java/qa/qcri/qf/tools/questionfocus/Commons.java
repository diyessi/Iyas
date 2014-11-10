package qa.qcri.qf.tools.questionfocus;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.Iterator;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class Commons {
	
	private static final Logger logger = LoggerFactory.getLogger(Commons.class);
	
	public static final String QUESTION_FOCUS_DATA = "data/question-focus/";
	
	public static final String QUESTION_FOCUS_KEY = "FOCUS";
	
	private static final String QID_QUESTION_SEPARATOR = "\t";
		
	/**
	 * Builds a new QuestionFocus analyzer for the specified language.
	 * @param lang A string holding the analyzer language
	 * @returnTeh newwly built analyzer
	 * @throws UIMAException
	 */
	public static Analyzer instantiateQuestionFocusAnalyzer(String lang) throws UIMAException {
		if (lang == null)
			throw new NullPointerException("lang is null");
		
		Analyzer analyzer = null;
		if (lang.equals("en")) { 
			analyzer = instantiateEnglishQuestionFocusAnalyzer();
		} else {
			logger.warn("No QuestionFocus analyzer found for lang: " + lang + ". Returned default QuestionFocus analyzer for english language.");
			analyzer = instantiateEnglishQuestionFocusAnalyzer();
		}
		
		return analyzer;
	}
	
	/**
	 * Builds a new QuestionFocus analyzer for the english language.
	 * 
	 * @return The english QuestionFocus analyzer
	 * @throws UIMAException
	 */
	public static Analyzer instantiateEnglishQuestionFocusAnalyzer() throws UIMAException {
		Analyzer analyzer = new Analyzer()
			.addAEDesc(createEngineDescription(StanfordSegmenter.class))
			.addAEDesc(createEngineDescription(StanfordPosTagger.class))
			.addAEDesc(createEngineDescription(StanfordParser.class));
		
		return analyzer; 
	}

	/*
	public static Analyzer instantiateAnalyzer(UIMAPersistence persistence)
			throws UIMAException {
		Analyzer ae = new Analyzer(persistence);

		AnalysisEngine stanfordSegmenter = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordSegmenter.class));
		
		AnalysisEngine stanfordPosTagger = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordPosTagger.class));
		
		AnalysisEngine stanfordParser = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordParser.class));

		ae.addAE(stanfordSegmenter)
			.addAE(stanfordPosTagger)
			.addAE(stanfordParser);

		return ae;
	}
	*/
	

	/**
	 * 
	 * @return the parameters used for producing the classification trees
	 */
	public static String getParameterList() {
		String parameterList = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA,
						RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
		return parameterList;
	}

	public static void analyzeQuestionsWithId(
		String datafile, Analyzer analyzer) 
		throws UIMAException {
		if (datafile == null) 
			throw new NullPointerException("datafile is null");
		if (datafile.trim().equals("")) {
			throw new IllegalArgumentException("datafile is null");
		}
		if (analyzer == null)
			throw new NullPointerException("analyzer is null");
		
		JCas cas = JCasFactory.createJCas();
		
		Iterator<QuestionWithFocus> questions =
				new QuestionWithIdReader(datafile, QID_QUESTION_SEPARATOR)
					.iterator();
		while (questions.hasNext()) { 
			QuestionWithFocus questionWithFocus = questions.next();
			System.out.format("doxtxt(%s): %s\n", questionWithFocus.getId(), questionWithFocus.getContent());
			analyzer.analyze(cas, questionWithFocus);
		}		
	}

}
