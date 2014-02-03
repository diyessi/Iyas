package qa.qcri.qf.features.representation;

import org.apache.uima.jcas.JCas;

import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TreeSerializer;
import util.Pair;

public class PosChunkTreeRepresentation implements Representation {

	private Pair<String, String> representation;

	public PosChunkTreeRepresentation(JCas aCas, JCas bCas, String parameterList) {
		this.representation = new Pair<>(
				getRepresentation(aCas, parameterList), getRepresentation(bCas,
						parameterList));
	}

	@Override
	public Pair<String, String> getRepresentation() {
		return representation;
	}

	private String getRepresentation(JCas cas, String parameterList) {
		return new TreeSerializer().enableRelationalTags().serializeTree(
				RichTree.getPosChunkTree(cas), parameterList);
	}
}
