package qa.qcri.qf.tools.questionclassifier;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class Commons {

	public static final String QF_DIRECTORY = "data/question-classifier/";

	public static final String[] CATEGORIES = { "ABBR", "DESC", "ENTY", "HUM",
			"LOC", "NUM" };

	public static final String MODELS_DIRECTORY = Commons.QF_DIRECTORY
			+ "models/";

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
		Iterator<CategoryContent> questions = new QuestionReader(questionsPath)
				.iterator();
		while (questions.hasNext()) {
			CategoryContent question = questions.next();
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
