package qa.qcri.qf.semeval2015_3.textnormalization;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;




public class TextNormalizer {

	private static final String WWW = "3W_SUBS";
	
	public static String normalize(String informalText){
		String normalizedText = informalText.replaceAll("www", WWW);//for preserving the www (otherwise they will be shrinked in a single 'w')

		normalizedText = getText(Jsoup.parse(informalText));
		
		normalizedText = normalizedText.replace('_', '-'); //substitutes _ with - in order not to mess with the stanford parser
		
		normalizedText = normalizedText.replaceAll("!+", "!");//eliminates multiple occurrences of !
		
		normalizedText = normalizedText.replaceAll("[?]+", "?");//eliminates multiple occurrences of ?
		
		normalizedText = normalizedText.replaceAll("([!]*[?]+[!]*)+", "?");//substitute ?!? into ?
		
		normalizedText = normalizedText.replaceAll("[.]+", ".");//eliminates multiple occurrences of .
		
		normalizedText = normalizedText.replaceAll("([a-zA-Z])(\\1{2,})", "$1");//substitutes triple (or more) occurrences of the same letter with a single occurrence
		
		normalizedText = normalizedText.replaceAll("\\[[^\\]\\[]*\\]", "");//eliminates text in squared brackets (they usually are not informative and they confuse the parser)
		
		//normalizedText.replaceAll("\[[^\[\]]*\]", "");
		
		
		
		if(normalizedText.equals(normalizedText.toUpperCase())){
			normalizedText = normalizedText.toLowerCase();
		}
		
		return normalizedText.replaceAll(WWW, "www");
		
	}
	
	/**
	 * @param cell element that contains whitespace formatting
	 * @return
	 */
	public static String getText(Element cell) {
	    String text = null;
	    List<Node> childNodes = cell.childNodes();
	    if (childNodes.size() > 0) {
	        Node childNode = childNodes.get(0);
	        if (childNode instanceof TextNode) {
	            text = ((TextNode)childNode).getWholeText();
	        }
	    }
	    if (text == null) {
	        text = cell.text();
	    }
	    return text;
	}
	
}
