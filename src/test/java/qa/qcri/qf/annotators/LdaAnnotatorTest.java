package qa.qcri.qf.annotators;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.File;
import java.util.Collection;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.junit.BeforeClass;
import org.junit.Test;

import qa.qcri.qf.lda.LdaModelResource;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.type.LdaTopic;
import qa.qcri.qf.type.LdaTopicDistribution;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class LdaAnnotatorTest {
	
	private static JCas cas;
	
	@BeforeClass
	public static void setUp() throws UIMAException {
		Analyzer analyzer = new Analyzer();
		
		ExternalResourceDescription extDesc = createExternalResourceDescription(
				LdaModelResource.class, new File("data/lda/answerbag/model/train.topics100.model"));
		
		AnalysisEngine br = AnalysisEngineFactory.createEngine(
				createEngineDescription(BreakIteratorSegmenter.class));
		AnalysisEngine lda = AnalysisEngineFactory.createEngine(
				createEngineDescription(LdaAnnotator.class, LdaAnnotator.MODEL_KEY, extDesc));
		
		Analyzable content = new SimpleContent("0", " McCartney as the \"most successful composer and recording artist of all time\", with 60 gold discs and sales of over 100 million albums and 100 million singles");
		analyzer.addAE(br);
		analyzer.addAE(lda);
		
		cas = JCasFactory.createJCas();
		analyzer.analyze(cas, content);
	}

	@Test
	public void testLdaTopicDistribution() {
		LdaTopicDistribution topicDist = JCasUtil.selectSingle(cas, LdaTopicDistribution.class);
		assertNotNull(topicDist);		
	}
	
	@Test
	public void testLdaTopic() {
		Collection<LdaTopic> topics = JCasUtil.select(cas, LdaTopic.class);
		assertFalse(topics.isEmpty());
	}

}
