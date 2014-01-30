package qa.qcri.qf.features.dkpro;

import org.apache.uima.jcas.JCas;

import qa.qcri.qf.features.PairFeatures;

public class PairFeatureFactory {
	
	public PairFeatures getPairFeatures(JCas aCas, JCas bCas) {
		PairFeatures pf = new PairFeatures(aCas, bCas);
		pf.addFeature(TokensCosineSimilarity.NAME);
		pf.addFeature(LemmasCosineSimilarity.NAME);
		pf.addFeature(LowerCaseTokensCosineSimilarity.NAME);
		return pf;
	}
}
