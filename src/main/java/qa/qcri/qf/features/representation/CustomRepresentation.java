package qa.qcri.qf.features.representation;

import org.apache.uima.jcas.JCas;

import util.Pair;

public class CustomRepresentation implements Representation {

	private Pair<String, String> representation;
	
	public CustomRepresentation(String a, String b) {
		this.representation = new Pair<>(a, b);
	}
	
	@Override
	public Pair<String, String> getRepresentation(JCas aCas, JCas bCas) {
		return this.representation;
	}

}
