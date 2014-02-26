package arabicpostagger.wrapper;

import java.util.List;

public class ArabicAnnotations {
	
	private String normalizedText;
	private List<ArabicToken> tokens;
	
	public ArabicAnnotations(List<ArabicToken> tokens, String normalizedText) {
		this.normalizedText = normalizedText;
		this.tokens = tokens;
	}
	
	public String getNormalizedText() {
		return this.normalizedText;
	}
	
	public List<ArabicToken> getArabicTokens() {
		return this.tokens;
	}	
}
