package qa.qcri.qf.features.dkpro;

import java.util.List;

import qa.qcri.qf.features.PairFeature;
import qa.qcri.qf.pipeline.UimaUtil;
import qa.qcri.qf.trees.RichNode;
import qa.qcri.qf.trees.RichTokenNode;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity;

public class LemmasCosineSimilarity implements PairFeature {
	
	public static final String NAME = "LemmasCosineSimilarity";

	private List<RichTokenNode> aTokens;

	private List<RichTokenNode> bTokens;

	private String parameterList;

	public LemmasCosineSimilarity(List<RichTokenNode> aTokens,
			List<RichTokenNode> bTokens) {
		this.aTokens = aTokens;
		this.bTokens = bTokens;
		this.parameterList = RichNode.OUTPUT_PAR_LEMMA;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public double getValue() throws SimilarityException {
		return new CosineSimilarity().getSimilarity(UimaUtil
				.getRichTokensRepresentation(this.aTokens, this.parameterList),
				UimaUtil.getRichTokensRepresentation(this.bTokens,
						this.parameterList));
	}

}
