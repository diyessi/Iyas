package qa.qcri.qf.tools.questionclassifier;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

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

public class Commons {
	
	public static final String QF_DIRECTORY = "data/question-classifier/";
	
	public static Analyzer instantiateAnalyzer(UIMAPersistence persistence)
			throws UIMAException {
		Analyzer ae = new Analyzer(persistence);

		ae.addAEDesc(createEngineDescription(StanfordSegmenter.class))
				.addAEDesc(createEngineDescription(StanfordPosTagger.class))
				.addAEDesc(createEngineDescription(StanfordParser.class));

		return ae;
	}
	
	public static Set<String> analyzeAndCollectCategories(String questionsPath, Analyzer ae) throws UIMAException {
		Set<String> categories = new HashSet<>();
		JCas cas = JCasFactory.createJCas();
		Iterator<CategoryContent> questions = new QuestionReader(questionsPath).iterator();
		while(questions.hasNext()) {
			CategoryContent question = questions.next();
			categories.add(question.getCategory());
			
			ae.analyze(cas, question);
		}
		return categories;
	}
	
	public static String getParameterList() {
		String parameterList = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA,
						RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
		return parameterList;
	}
	
}
