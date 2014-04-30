package qa.qcri.qf.tools.questionfocus;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public class Focus {
	
	private final static Logger logger = Logger.getLogger(Focus.class);
	
	// Part-of-speech of words appearing in question focus for italian sentences. 
	private final static String[] italianAllowedTags = { "SN", "SP", "SPN", "SS"  };
	
	// Part-of-speech of words appearing in question focus for english sentences.
	private final static String[] englishAllowedTags = { //"NNPS", "VBN", "JJ", "JJS", // These postags are seen just once as focus
		"NNS", "NNP",  "NN" // These are high frequency postags
	};
	
	private final static String[] emptyTagset = { };
	

	/**
	 * Returns the set of legal tags (i.e. Part-of-speech) which can be tagged
	 *  with the *-FOCUS marker.
	 * 
	 * @param lang A string holding the text language
	 * @return The legal set of tags (POS) 
	 */
	static Set<String> getAllowedTagsByLanguage(String lang) { 
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

}
