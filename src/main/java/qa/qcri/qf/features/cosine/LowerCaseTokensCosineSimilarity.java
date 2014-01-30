package qa.qcri.qf.features.cosine;

import java.util.List;

import qa.qcri.qf.features.PairFeature;
import qa.qcri.qf.pipeline.UimaUtil;
import qa.qcri.qf.trees.RichNode;
import qa.qcri.qf.trees.RichTokenNode;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity;

public class LowerCaseTokensCosineSimilarity implements PairFeature {
	
	public static final String NAME = "LowerCaseTokensCosineSimilarity";

	private List<RichTokenNode> aTokens;

	private List<RichTokenNode> bTokens;

	private String parameterList;

	public LowerCaseTokensCosineSimilarity(List<RichTokenNode> aTokens,
			List<RichTokenNode> bTokens) {
		this.aTokens = aTokens;
		this.bTokens = bTokens;
		this.parameterList = Joiner.on(",").join(
				RichNode.OUTPUT_PAR_TOKEN, RichNode.OUTPUT_PAR_TOKEN_LOWERCASE);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public double getValue() throws SimilarityException {
		return new CosineSimilarity().getSimilarity(
				UimaUtil.getRichTokensRepresentation(this.aTokens, this.parameterList),
				UimaUtil.getRichTokensRepresentation(this.bTokens, this.parameterList));
	}

}
