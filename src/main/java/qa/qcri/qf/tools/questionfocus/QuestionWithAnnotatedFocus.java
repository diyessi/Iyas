package qa.qcri.qf.tools.questionfocus;

import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Objects;

import qa.qcri.qf.pipeline.retrieval.Analyzable;
import util.Pair;

/**
 * A simple class storing information about a 
 * 	question and its focus.
 *
 */
public class QuestionWithAnnotatedFocus implements Analyzable {
	
	private final static String DEFAULT_LANGUAGE = "en";

	private final static String FOCUS_SYMBOL = "#"; // symbol used to mark the focus
	private final static String IMPLICIT_FOCUS_MARK = "IMPL"; // start label denoting implicit focus
	
	private final String id;
	private final String lang;
	private final String line;
	private final String content;
	private final String lineWithStrippedFocus;
	
	public QuestionWithAnnotatedFocus(String id, String content) {
		this(id, content, DEFAULT_LANGUAGE);
	}
	
	public QuestionWithAnnotatedFocus(String id, String content, String lang) {
		if (id == null)
			throw new NullPointerException("lang is null");
		if (lang == null)
			throw new NullPointerException("content is null");
		if (content == null)
			throw new NullPointerException("lang is null");
			
		this.id = id;
		this.lang = lang;
		this.content = content;
		this.line = this.filterText(content);
		this.lineWithStrippedFocus = line.replaceAll(FOCUS_SYMBOL, "");  
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
	
	public String getLineWithStrippedFocus() {
		return this.lineWithStrippedFocus;
	}
	
	/**
	 * Says whether the focus is implicit.
	 * 
	 * @return True if the focus is implicit, false otherwise
	 */
	public boolean isImplicit() { 
		return this.line.startsWith(IMPLICIT_FOCUS_MARK);
	}
	
	/**
	 * Says whether the question contains focus.
	 * 
	 * @return True if the question contains focus, false otherwise
	 */
	public boolean containsFocus() {
		return this.line.contains(FOCUS_SYMBOL);
	}
	
	/**
	 * Returns the sentence without focus.
	 * 
	 * @return A string holding the sentence without focus mark
	 */
	public String stripFocus() {
		return line.replaceAll(FOCUS_SYMBOL, "");
	}
	
	/**
	 * Returns the focus word.
	 * 
	 * @return A string holding the focus word
	 */
	public String getFocus() {
		Pair<Integer, Integer> focusSpan = getFocusSpanFromMarkedSentence();
		
		return this.line.substring(focusSpan.getA() + 1, focusSpan.getB());
	}	
	
	/**
	 * Returns the position of the focus (including the focus marker #)
	 * 
	 * @return A pair of Integer holding the start and end position 
	 *      of the focus
	 */
	public Pair<Integer, Integer> getFocusSpanFromMarkedSentence() { 
		int beginPos = line.indexOf(FOCUS_SYMBOL);
		int endPos = line.indexOf(" ", beginPos);
		if (endPos < 0) {
			endPos = line.length();
		}
		return new Pair<Integer, Integer>(beginPos, endPos);
	}
	
	/**
	 * Returns the position of the focus in the sentence 
	 *  (assuming the sentence does not contain the focus marker)
	 * 
	 * @return A pair of Integer holding the start and end position
	 *     of the focus
	 */
	public Pair<Integer, Integer> getFocusSpanFromUnmarkedSentence() {
		int beginPos = line.indexOf(FOCUS_SYMBOL);
		int endPos = line.indexOf(" ", beginPos);
		if (endPos < 0) {
			endPos = line.length();
		}
		return new Pair<Integer, Integer>(beginPos, endPos - 1);
	}
	
	/**
	 * Returns the number of focuses.
	 * 
	 * @return An Integer holding the number of focuses
	 */
	public int getFocusNumber() {
		return StringUtils.countMatches(line, FOCUS_SYMBOL);
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
	 */
	private String filterText(String text) {
		String filteredText = text.trim();
		filteredText = filteredText.replaceAll(" +", " ");
		return filteredText;
	}

}
