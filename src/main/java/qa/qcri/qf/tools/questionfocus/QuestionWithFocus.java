package qa.qcri.qf.tools.questionfocus;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Objects;

import qa.qcri.qf.pipeline.retrieval.Analyzable;

/**
 * A simple class storing information about a 
 * 	question and its focus.
 *
 */
public class QuestionWithFocus implements Analyzable {
	
	private final static String DEFAULT_LANGUAGE = "en";
	
	private final Logger logger = Logger.getLogger(QuestionWithFocus.class);

	//private final static String FOCUS_SYMBOL = "#"; // symbol used to mark the focus
	//private final static String IMPLICIT_FOCUS_MARK = "IMPL"; // start label denoting implicit focus
	
	private final String id;
	private final String lang;
	private final String content;
	
	private final List<Focus> foci = new ArrayList<>();	
	public QuestionWithFocus(String id, String content) {
		this(id, content, DEFAULT_LANGUAGE);
	}
	
	public QuestionWithFocus(String id, String content, String lang) {
		if (id == null)
			throw new NullPointerException("lang is null");
		if (lang == null)
			throw new NullPointerException("content is null");
		if (content == null)
			throw new NullPointerException("lang is null");
			
		this.id = id;
		this.lang = lang;
		//int j = 0;
		
		String line = content.trim().replaceAll("\\s+", " ");
		String sent = "", 
			   focusVal = "";
		for (int i = 0; i < line.length(); i++) { 
			char ch = line.charAt(i);
			if (ch == '#') {
				//int begin = i;
				int begin = i - foci.size();
				i++;
				while (i < line.length() && line.charAt(i) != ' ') { 
					sent += line.charAt(i);
					focusVal += line.charAt(i);
					i++;
				}
				Focus focus = new Focus(focusVal, begin);
				foci.add(focus);
				focusVal = "";
			}
			if (i < line.length()) {
				sent += line.charAt(i);
			}
			
		}
		this.content = sent;
		logger.debug("foci: " + foci);
		logger.debug("sent: " + sent);
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getContent() {
		return this.content;
	}

	@Override
	public String getLanguage() {
		return this.lang;
	}
	
	public Analyzable getQuestion() { 
		return new Analyzable() {
			
			@Override
			public String getLanguage() {
				return lang;
			}
			
			@Override
			public String getId() {
				return id;
			}
			
			@Override
			public String getContent() {
				//return lineWithStrippedFocus;
				return content;
			}
		};
	}
	
	/*
	public String getLineWithStrippedFocus() {
		return this.lineWithStrippedFocus;
	}
	*/
	
	/**
	 * Says whether the focus is implicit.
	 * 
	 * @return True if the focus is implicit, false otherwise
	 *
	public boolean isImplicit() { 
		return this.line.startsWith(IMPLICIT_FOCUS_MARK);
	}
	*/
	
	/**
	 * Says whether the question contains focus.
	 * 
	 * @return True if the question contains focus, false otherwise
	 *
	public boolean containsFocus() {
		return this.line.contains(FOCUS_SYMBOL);
	}
	*/
	
	public boolean hasFocus() { 
		return !this.foci.isEmpty();
	}
	
	/**
	 * Returns the sentence without focus.
	 * 
	 * @return A string holding the sentence without focus mark
	 *
	public String stripFocus() {
		return line.replaceAll(FOCUS_SYMBOL, "");
	}
	*/
	
	/**
	 * Returns the focus word.
	 * 
	 * @return A string holding the focus word
	 *
	public String getFocus() {
		Pair<Integer, Integer> focusSpan = getFocusSpanFromMarkedSentence();
		
		return this.line.substring(focusSpan.getA() + 1, focusSpan.getB());
	}
	*/	
	
	/**
	 * Returns the position of the focus (including the focus marker #)
	 * 
	 * @return A pair of Integer holding the start and end position 
	 *      of the focus
	 *
	public Pair<Integer, Integer> getFocusSpanFromMarkedSentence() { 
		int beginPos = line.indexOf(FOCUS_SYMBOL);
		int endPos = line.indexOf(" ", beginPos);
		if (endPos < 0) {
			endPos = line.length();
		}
		return new Pair<Integer, Integer>(beginPos, endPos);
	}
	*/
	
	public List<Focus> getFoci() { 
		return this.foci;
	}
	
	/**
	 * Returns the position of the focus in the sentence 
	 *  (assuming the sentence does not contain the focus marker)
	 * 
	 * @return A pair of Integer holding the start and end position
	 *     of the focus
	 *
	public Pair<Integer, Integer> getFocusSpanFromUnmarkedSentence() {
		int beginPos = line.indexOf(FOCUS_SYMBOL);
		int endPos = line.indexOf(" ", beginPos);
		if (endPos < 0) {
			endPos = line.length();
		}
		return new Pair<Integer, Integer>(beginPos, endPos - 1);
	}
	*/
	
	/**
	 * Returns the number of focuses.
	 * 
	 * @return An Integer holding the number of focuses
	 */
	public int getFocusNumber() {
		//return StringUtils.countMatches(line, FOCUS_SYMBOL);
		return foci.size();
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("id", this.id)
				.add("lang", this.lang)
				.add("content", this.content)
				.toString();
	}
	
	/**
	 * Performs basic whitespace filtering on a string.
	 * 
	 * @param text A string holding the text
	 * @return the filtered text
	 *
	private String filterText(String text) {
		String filteredText = text.trim();
		filteredText = filteredText.replaceAll(" +", " ");
		return filteredText;
	}
	*/

}
