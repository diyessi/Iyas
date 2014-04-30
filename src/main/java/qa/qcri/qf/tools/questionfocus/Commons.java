package qa.qcri.qf.tools.questionfocus;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

import com.google.common.base.Joiner;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.nodes.RichNode;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class Commons {
	
	public static final String QUESTION_FOCUS_DATA = "data/question-focus/";
	
	public static final String QUESTION_FOCUS_KEY = "FOCUS";

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
