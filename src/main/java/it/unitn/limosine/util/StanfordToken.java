package it.unitn.limosine.util;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.LabelFactory;
import it.unitn.limosine.types.segmentation.Token;

/***
 * Helper class that implements Stanford's HasWord 
 * 
 * Used e.g. in StanfordParser and StanfordNER annotator's
 * 
 * @author bplank
 *
 */
public class StanfordToken implements HasWord, Label {

	private static final long serialVersionUID = 1L;
	
	private String tokenStr;
	private Token token;
	
	public StanfordToken(Token token) {
//		this.tokenStr = token.getCoveredText();
		this.tokenStr = token.getNormalizedText();
		this.token = token;
	}
	
	@Override
	public void setWord(String arg) {
		tokenStr = arg;
	}

	@Override
	public String word() {
		return tokenStr;
	}
	
	public Token getToken() {
		return token;
	}

	@Override
	public String value() {
		return tokenStr;
	}

	@Override
	public void setValue(String value) {
		setWord(value);
	}

	@Override
	public void setFromString(String labelStr) {
		setWord(labelStr);
	}

	@Override
	public LabelFactory labelFactory() {
		return null;
	}
	
}

