package qa.qcri.qf.tools.questionclassifier;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import it.unitn.limosine.italian.syntax.constituency.BerkeleyWrapperFix;
import it.unitn.limosine.italian.textpro.TextProWrapperFix;

import java.io.IOError;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import com.google.common.base.Joiner;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.nodes.RichNode;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import edu.berkeley.nlp.util.Logger;

public class Commons {

	public static final String QF_DIRECTORY = "data/question-classifier/";

	public static final String[] CATEGORIES = { "ABBR", "DESC", "ENTY", "HUM",
			"LOC", "NUM" };

	public static final String MODELS_DIRECTORY = Commons.QF_DIRECTORY
			+ "models/";

	/*
	public static Analyzer instantiateAnalyzer(UIMAPersistence persistence)
			throws UIMAException {
		Analyzer ae = new Analyzer(persistence);

		ae.addAEDesc(createEngineDescription(StanfordSegmenter.class))
				.addAEDesc(createEngineDescription(StanfordPosTagger.class))
				.addAEDesc(createEngineDescription(StanfordParser.class));

		return ae;
	}
	*/
	
	public static Analyzer instantiateQuestionClassifierAnalyzer(String lang) throws UIMAException { 
		if (lang == null) 
			throw new NullPointerException("lang is null");
		
		Analyzer analyzer = null;
		if (lang.equals("en")) {
			analyzer = instantiateEnglishQuestionClassifierAnalyzer();
		} else if (lang.equals("it")) {
			try {
				analyzer = instantiateItalianQuestionClassifierAnalyzer();
			} catch (IOException e) { 
				throw new UIMAException(e);
			}
		} else {
			Logger.warn("No QuestionClassifier analyzer found for lang: " + lang + ". Returned default QuestionClassifier analyzer for english language.");
			analyzer = instantiateEnglishQuestionClassifierAnalyzer();
		}
		
		return analyzer;		
	}
	
	private static Analyzer instantiateEnglishQuestionClassifierAnalyzer() throws UIMAException {
		Analyzer analyzer = new Analyzer();

		analyzer.addAEDesc(createEngineDescription(StanfordSegmenter.class))
				.addAEDesc(createEngineDescription(StanfordPosTagger.class))
				.addAEDesc(createEngineDescription(StanfordParser.class));

		return analyzer;
	}
	
	private static Analyzer instantiateItalianQuestionClassifierAnalyzer() throws UIMAException, IOException {
		Analyzer analyzer = new Analyzer();
		
		analyzer.addAEDesc(createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor"))
				.addAEDesc(createEngineDescription("desc/Limosine/BerkeleyITDescriptor"));
		/*
		analyzer.addAEDesc(createEngineDescription(TextProWrapperFix.class))
		  		.addAEDesc(createEngineDescription(BerkeleyWrapper.class));
		*/
		  	
		return analyzer;
	}

	/**
	 * Analyzes a file of questions and collects the associated category labels
	 * 
	 * @param questionsPath
	 *            the path of the question file
	 * @param ae
	 *            the analyzer to run
	 * @return the encountered categories
	 * @throws UIMAException
	 */
	public static Set<String> analyzeAndCollectCategories(String questionsPath,
			Analyzer ae) throws UIMAException {
		Set<String> categories = new HashSet<>();
		JCas cas = JCasFactory.createJCas();
		
		//System.out.println("questionsPath: " + questionsPath);
		Iterator<CategoryContent> questions = new QuestionReader(questionsPath)
				.iterator();
		
		int questionsNum = 0;
		while (questions.hasNext()) {
			//System.out.println("Processing questionNum: " + questionsNum++ + "...");
			CategoryContent question = questions.next();
			System.out.format("doctxt(%d): %s\n", questionsNum++, question.getContent());
			categories.add(question.getCategory());
			ae.analyze(cas, question);
		}
		return categories;
	}

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

}
