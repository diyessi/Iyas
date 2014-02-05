package qa.qcri.qf.pipeline.ngram;

import java.util.Set;

import org.maltparser.core.helper.HashSet;

/**
 * Generate the set of character n-grams from a string.
 *
 */
public class CharacterNGramGenerator {
	
	private static final String alphabet = "abcdefjhijklmnopqrstuvwxyz0123456789";
	
	private int n;
	
	/**
	 * Builds a new CharacterNGramGenerator.
	 * 
	 * @param n An integer holding the n-gram length
	 */
	public CharacterNGramGenerator(int n) {
		this.n = n;
	}
	
	/**
	 * Generate the set of all n-grams of length n from this string.
	 * 
	 * @param text A string 
	 * @return The set of n-grams of length n
	 */
	public Set<String> getNGrams(String text) {
		Set<String> ngrams = new HashSet<>();
		
		text = encode(text);
		
		for (int i = 0; i < text.length() - n + 1; i++) {
			// Generate n-gram at index i
			String ngram = text.substring(i, i + n).toLowerCase();
			
			// Add ngram
			ngrams.add(ngram);
		}
		
		return ngrams;		
	}

	/**
	 * Strip  all the chars not included in the alphabet from string.
	 */
	private String encode(String text) {
		StringBuilder sb = new StringBuilder();
		
		text = text.toLowerCase();
		char[] chars = text.toCharArray();
		
		for (char c : chars) {
			if (alphabet.indexOf(c) > -1) 
				sb.append(c);
		}
		
		return sb.toString();
	}

}
