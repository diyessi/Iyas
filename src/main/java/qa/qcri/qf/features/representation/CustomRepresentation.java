package qa.qcri.qf.features.representation;

import util.Pair;

public class CustomRepresentation implements Representation {

	private Pair<String, String> representation;
	
	public CustomRepresentation(String a, String b) {
		this.representation = new Pair<>(a, b);
	}
	
	@Override
	public Pair<String, String> getRepresentation() {
		return this.representation;
	}

}
