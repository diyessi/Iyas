package qa.qcri.qf.features;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.WordNGramContainmentMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity;
import qa.qcri.qf.trees.RichNode;

public class PairFeatureFactory {
	
	public PairFeatureFactory() {
		
	}

	public PairFeatures getPairFeatures(JCas aCas, JCas bCas) {
		PairFeatures pf = new PairFeatures(aCas, bCas, RichNode.OUTPUT_PAR_TOKEN_LOWERCASE);
		
		pf.computeFeature(new CosineSimilarity());
		pf.computeFeature(new WordNGramContainmentMeasure(1));
		pf.computeFeature(new WordNGramContainmentMeasure(2));
		pf.computeFeature(new WordNGramContainmentMeasure(3));
		
		return pf;
	}
	
	
	
}
