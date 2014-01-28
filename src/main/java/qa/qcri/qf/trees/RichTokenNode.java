package qa.qcri.qf.trees;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * 
 * The RichTokenNode class wraps a Token object, a datatype from the DKPro
 * typesystem
 * 
 * The class overrides the getValue() method, which returns the text covered by
 * the token
 * 
 * Moreover, it understands a set of parameters for producing different token
 * representations
 */
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

	/**
	 * Return a string representation of the node which may be affected by the
	 * provided parameter list. A node can parse this list and react according
	 * to it. In the default case the implementation should return the same
	 * value of getValue()
	 * 
	 * Supported parameters
	 * 
	 * - RichNode.OUTPUT_PAR_TOKEN Return the text covered by the token, which
	 * is also the default behaviour
	 * 
	 * - RichNode.OUTPUT_PAR_LEMMA Return the lemma of the token
	 * 
	 * - RichNode.OUTPUT_PAR_TOKEN_LOWERCASE Return the lowercased current
	 * representation
	 * 
	 * Pay attention to the order of these parameters in the list. TOKEN and
	 * LEMMA override each other, so the parameter later in the list prevails.
	 * 
	 * @param parameterList
	 *            parameter list (strings separated by comma)
	 * @return the node representation
	 */
	@Override
	public String getRepresentation(String parameterList) {
		String output = this.getValue();

		if (parameterList.isEmpty() || !parameterList.contains(",")) {
			return output;
		}

		boolean lowercase = false;

		String[] fields = parameterList.split(",");
		for (String field : fields) {
			switch (field) {
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

		if (lowercase) {
			output = output.toLowerCase();
		}

		return output;
	}
}
