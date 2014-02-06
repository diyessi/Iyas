package qa.qcri.qf.pipeline.ngram;

import java.util.Set;

/**
 * Store idf values for different strings.
 *
 */
public interface IdfModel {
	
	/**
	 * @param str A string
	 * @return The idf value associated with this string
	 */
	double getIdf(String str);
	
	/**
	 * Returns the string keys stored in the idf model.
	 * @return The set of string keys 
	 */
	Set<String> keySet();

}
