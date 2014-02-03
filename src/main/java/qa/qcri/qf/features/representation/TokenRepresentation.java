package qa.qcri.qf.features.representation;

import java.util.List;

import org.apache.uima.jcas.JCas;

import qa.qcri.qf.pipeline.UimaUtil;
import qa.qcri.qf.trees.RichTokenNode;
import util.Pair;

import com.google.common.base.Joiner;

public class TokenRepresentation implements Representation {
	
	private Pair<String, String> representation;
	
	public TokenRepresentation(JCas aCas, JCas bCas, String parameterList) {
		this.representation = new Pair<>(
				getRepresentation(aCas, parameterList),
				getRepresentation(bCas, parameterList));
	}

	@Override
	public Pair<String, String> getRepresentation() {
		return representation;
	}
	
	private String getRepresentation(JCas cas, String parameterList) {
		List<RichTokenNode> richTokens = UimaUtil.getRichTokens(cas);
		List<String> tokens = UimaUtil.getRichTokensRepresentation(richTokens, parameterList);
		return Joiner.on(" ").join(tokens);
	}

}
