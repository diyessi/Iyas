package qa.qcri.qf.pipeline.ngram;

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

}
