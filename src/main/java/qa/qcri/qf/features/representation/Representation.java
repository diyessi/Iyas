package qa.qcri.qf.features.representation;

import org.apache.uima.jcas.JCas;

import util.Pair;

public interface Representation {
	
	Pair<String, String> getRepresentation(JCas aCas, JCas bCas);
	
}
