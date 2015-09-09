package qa.qcri.qf.cQAdemo;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to extract URLs from questions in the QatarLiving forum.
 * So far, it is supposed to depend on Google's search engine.
 *
 */
public class QuestionRetriever {

	private final String url1 = "http://www.qatarliving.com/forum/qatar-living-lounge/posts/where-get-exit-permit-visit-visa";
	private final String url2 = "http://www.qatarliving.com/forum/qatar-living-lounge/posts/best-dental-clinic-doha-qatar";
	
	
	public QuestionRetriever() {
		//TODO Wei, please add the necessary code (including methods)
		//dependencies should be maven-like
	}

	/**
	 * Given a question, retrieve a set of links pointing to related questions in the QatarLiving website 
	 * @param userQuestion a question in plain text
	 * @return
	 */
	public List<String> getLinks(String userQuestion) {
		//TODO Wei, please add the necessary code
		return null;
	}
	
	/**
	 * Function for testing purposes
	 * @return a fixed set of links
	 */
	public List<String> getTestLinks() {
		List<String> urls = new ArrayList<String>();
		urls.add(url1);
		urls.add(url2);
		return urls;
	}
	
}
