package qa.qcri.qf.tools.questionfocus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.maltparser.core.helper.HashSet;

public class Focus {
	
	public static final Set<String> ITALIAN = new HashSet<>(Arrays.asList("SN", "SP", "SPN", "SS"));
	public static final Set<String> ENGLISH = new HashSet<>(
			//Arrays.asList("NNPS", "VBN", "JJ", "JJS", // These are high frequency postags
			Arrays.asList("NNS", "NNP", "NN")); // These are high frequency postags
			// "NNS", "NNP",  "NN" ));
			//"NNPS", "VBN", "JJ", "JJS", // These postags are seen just once as focus
	public static final Set<String> EMPTY = new HashSet<>(Arrays.asList(new String[]{}));
	
	private static Logger logger = Logger.getLogger(Focus.class);
	
	/*
	// Part-of-speech of words appearing in question focus for italian sentences. 
	private final static String[] italianAllowedTags = { "SN", "SP", "SPN", "SS"  };
	
	// Part-of-speech of words appearing in question focus for english sentences.
	private final static String[] englishAllowedTags = { //"NNPS", "VBN", "JJ", "JJS", // These postags are seen just once as focus
		"NNS", "NNP",  "NN" // These are high frequency postags
	};
		
	private final static String[] emptyTagset = { };
	*/
	
	private final int begin;
	private final String word;
	
	public Focus(String word, int begin) {
		if (word == null) { 
			throw new NullPointerException("word is null");
		}
		if (word.trim().equals("")) { 
			throw new IllegalArgumentException("word not specified");
		}
		
		this.begin = begin;
		this.word = word;		
	}
	
	public int getBegin() {
		return this.begin;		
	}
	
	public int getEnd() { 
		return this.begin + this.word.length();
	}
	
	public String getWord() {
		return this.word;
	}

	/**
	 * Returns the set of legal tags (i.e. Part-of-speech) which can be tagged
	 *  with the *-FOCUS marker.
	 * 
	 * @param lang A string holding the text language
	 * @return The legal set of tags (POS) 
	 *
	public static Set<String> getAllowedTagsByLanguage(String lang) { 
		if (lang == null)
			throw new NullPointerException("lang is null");
	
		String[] tagset = null;
		switch (lang) {
			case "en":
				tagset = englishAllowedTags;
				break;
			case "it":
				tagset = italianAllowedTags;
				break;
			default:
				logger.warn("No focus tagset found for lang \"" + lang + "\". Returning english tagset.");
				tagset = englishAllowedTags;
		}
		return new TreeSet<>(Arrays.asList(tagset));
	}
	*/

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + begin;
		result = prime * result + ((word == null) ? 0 : word.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Focus other = (Focus) obj;
		if (begin != other.begin)
			return false;
		if (word == null) {
			if (other.word != null)
				return false;
		} else if (!word.equals(other.word))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "Focus(\"" + word + ", " + begin + ", " + (begin + word.length()) + "\")";  
	}	
	
	public static Set<String> allowedTags(String lang) {
		if (lang == null) {
			throw new NullPointerException("lang is null");
		}
		if (lang.trim().equals("")) { 
			throw new IllegalArgumentException("lang not specified");
		}
		
		Set<String> poss;
		switch(lang) {
		case "it":
		case "italian":
			poss = ITALIAN;
			break;
		case "en":
		case "english":
			poss = ENGLISH;
			break;
		default:
			poss = EMPTY;
			logger.warn("No FocusClassifier found for lang: \"" + lang + "\"" + 
						"Returning FocusClassifier for the english lang.");
			break;
		}
		return poss;
	}

}
