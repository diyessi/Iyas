package qa.qcri.qf.tools.questionfocus;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import edu.berkeley.nlp.util.Logger;

public class Commons {
	
	public static final String QUESTION_FOCUS_DATA = "data/question-focus/";
	
	public static final String QUESTION_FOCUS_KEY = "FOCUS";
	
	/**
	 * Builds a new QuestionFocus analyzer for the specified language.
	 * @param lang the analyzer language
	 * @return the QuestionFocus analyzer
	 * @throws UIMAException
	 */
	public static Analyzer instantiateQuestionFocusAnalyzer(String lang) throws UIMAException {
		if (lang == null) {
			throw new NullPointerException("lang is null");
		}
		
		Analyzer analyzer = null;
		if (lang.equals("en")) { 
			analyzer = instantiateEnglishQuestionFocusAnalyzer();
		} else if (lang.equals("it")) { 
			analyzer = instantiateItalianQuestionFocusAnalyzer();
		} else {
			Logger.warn("No QuestionFocus analyzer found for lang: " + lang
					+ ". Returned default QuestionFocus analyzer for english language.");
			analyzer = instantiateEnglishQuestionFocusAnalyzer();
		}
		
		return analyzer;
	}
	
	/**
	 * Builds a new QuestionFocus analyzer for the English language.
	 * 
	 * @return the QuestionFocus analyzer for English
	 * @throws UIMAException
	 */
	public static Analyzer instantiateEnglishQuestionFocusAnalyzer() throws UIMAException {
		Analyzer analyzer = new Analyzer()
			.addAEDesc(createEngineDescription(StanfordSegmenter.class))
			.addAEDesc(createEngineDescription(StanfordPosTagger.class))
			.addAEDesc(createEngineDescription(StanfordParser.class));
		
		return analyzer; 
	}
	
	/**
	 * Builds a new QuestionFocus analyzer for the Italian language.
	 * 
	 * @return the QuestionFocus analyzer for Italian
	 * @throws UIMAException
	 */
	public static Analyzer instantiateItalianQuestionFocusAnalyzer() throws UIMAException {
		Analyzer analyzer = null;
		try {
			analyzer = new Analyzer()
				.addAEDesc(createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor"))
				.addAEDesc(createEngineDescription("desc/Limosine/BerkeleyITDescriptor"));
		} catch (IOException e) { 
			throw new UIMAException(e);
		}
			
		return analyzer;
	}

	/**
	 * 
	 * @return the parameters used for serializing the classification trees
	 */
	public static String getParameterList() {
		String parameterList = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA,
						RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
		return parameterList;
	}

}
