package qa.qcri.qf.pipeline;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.Test;

import qa.qcri.qf.features.PairFeatures;
import qa.qcri.qf.features.representation.CustomRepresentation;
import qa.qcri.qf.features.representation.Representation;
import qa.qcri.qf.features.similarity.PTKSimilarity;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.similarity.algorithms.api.TermSimilarityMeasure;

public class PairFeaturesTest {

	@Test
	public void serializeIndexedFeaturesTest() throws UIMAException {
		
		String a = "(ROOT (A (B (C (c)) D (E (e)))))";
		String b = "(ROOT (G (B (C (c)) D (F (f)))))";
		
		JCas aCas = JCasFactory.createJCas();
		JCas bCas = JCasFactory.createJCas();
		
		Analyzer ae = new Analyzer(new UIMANoPersistence());
        
        ae.addAEDesc(createEngineDescription(OpenNlpSegmenter.class))
        	.addAEDesc(createEngineDescription(OpenNlpPosTagger.class));
        
        ae.analyze(aCas, new SimpleContent("a", a));
        ae.analyze(bCas, new SimpleContent("b", b));
        
        TermSimilarityMeasure measure = new PTKSimilarity();
        
        PairFeatures pf = new PairFeatures();
        Representation trees = new CustomRepresentation(a, b);
        pf.computeFeature(measure,
        		trees.getRepresentation(aCas, bCas).getA(),
        		trees.getRepresentation(aCas, bCas).getB());
        
		Assert.assertEquals("1:" + "0.5219278751298042", pf.serializeIndexedFeatures(1));
	}
}
