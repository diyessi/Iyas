package qa.qcri.qf.features.representation;

import java.util.List;

import org.apache.uima.jcas.JCas;

import qa.qcri.qf.pipeline.UimaUtil;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import util.Pair;

import com.google.common.base.Joiner;

public class TokenRepresentation implements Representation {

	private String parameterList;

	public TokenRepresentation(String parameterList) {
		this.parameterList = parameterList;
	}

	@Override
	public Pair<String, String> getRepresentation(JCas aCas, JCas bCas) {
		return new Pair<>(
				getRepresentation(aCas, this.parameterList),
				getRepresentation(bCas, this.parameterList)
			);
	}

	private String getRepresentation(JCas cas, String parameterList) {
		List<RichTokenNode> richTokens = UimaUtil.getRichTokens(cas);
		List<String> tokens = UimaUtil.getRichTokensRepresentation(
				richTokens, parameterList);
		return Joiner.on(" ").join(tokens);
	}

	@Override
	public String getName() {
		return "TokenRepresentation";
	}

}
