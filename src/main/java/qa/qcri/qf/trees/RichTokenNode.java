package qa.qcri.qf.trees;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class RichTokenNode extends BaseRichNode {
	
	private Token token;
	
	public RichTokenNode(Token token) {
		super();
		this.token = token;
		this.metadata.put(RichNode.TYPE_KEY, RichNode.TYPE_TOKEN_NODE);
	}
	
	public Token getToken() {
		return this.token;
	}
	
	@Override
	public String getValue() {
		return this.token.getCoveredText();
	}
	
	@Override
	public String getRepresentation(String parameterList) {
		String output = this.getValue();
		
		if(parameterList.isEmpty() || !parameterList.contains(",")) {
			return output;
		}
		
		boolean lowercase = false;
		
		String[] fields = parameterList.split(",");
		for(String field : fields) {
			switch(field) {
			case RichNode.OUTPUT_PAR_TOKEN:
				output = this.token.getCoveredText();
				break;
			case RichNode.OUTPUT_PAR_LEMMA:
				output = this.token.getLemma().getValue();
				break;
			case RichNode.OUTPUT_PAR_TOKEN_LOWERCASE:
				lowercase = true;
				break;
			}
		}
		
		
		if(lowercase) {
			output = output.toLowerCase();
		}
		
		return output;
	}
}
