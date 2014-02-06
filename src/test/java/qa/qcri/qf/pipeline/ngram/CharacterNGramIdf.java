/**
 * CharacterNGramMeasure.class requires a file in 
 * 	which are stored the idf values of the char n-grams appearing in a string.  
 * 
 */
package qa.qcri.qf.pipeline.ngram;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.maltparser.core.helper.HashMap;

import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.CharacterNGramMeasure;
import qa.qcri.qf.pipeline.SampleFileReader;
import qa.qcri.qf.pipeline.retrieval.Analyzable;

/**
 * Generate a idf model file for all character n-grams in the source file. 
 *
 */
public class CharacterNGramIdf {
	
	public final static String SAMPLE_CONTENT_PATH = "data/sample.txt";
	
	public final static String IDF_MODEL_PATH = "data/idf.model";
	
	/**
	 * Build the character n-grams idf model.
	 * 
	 * @param minN The size of the smallest n-grams to track
	 * @param maxN The size of the largest n-grams to track
	 * @param analyzableIterables A list of analyzables iterables
	 * @return The n-gram IdfModel object 
	 */
	public static IdfModel buildModel(int minN, int maxN, Iterable<Analyzable>... analyzableIterables) {
		if (analyzableIterables == null) {
			throw new NullPointerException("analyzables is null");
		}		
		if (minN > maxN) {
			throw new IllegalArgumentException("minN > maxN");
		}
		
		int documentsLength = 0;
		// Store n-grams doc freq
		Map<String, Integer> ngram2dfMap = new HashMap<>();
		
		for (Iterable<Analyzable> analyzableIterable : analyzableIterables) {			
			Iterator<Analyzable> analyzableIt = analyzableIterable.iterator();			
			
			for (Iterator<Analyzable> it = analyzableIterable.iterator(); it.hasNext(); documentsLength++) {
				Analyzable analyzable = analyzableIt.next();
				String text = analyzable.getContent();
				
				for (int n = minN; n <= maxN; n++) {
					CharacterNGramGenerator generator = new CharacterNGramGenerator(n);
					// Generate char n-grams
					for (String ngram : generator.getNGrams(text)) {
						Integer df = ngram2dfMap.get(ngram);
						if (df == null) {
							df = 0;
						}
						df += 1;
						ngram2dfMap.put(ngram, df);
					}
				}
			}
		}
			
		// store n-grams idf values
		Map<String, Double> ngram2idfMap = createNGram2IdfMap(ngram2dfMap, documentsLength);
		return new IdfStore(ngram2idfMap);
	}
	
	/** Records n-grams idf values */
	private static Map<String, Double> createNGram2IdfMap(Map<String, Integer> ngram2df, int documentsLength) {
		assert ngram2df != null;
		
		Map<String, Double> ngram2idf = new HashMap<>();
		for (String ngram : ngram2df.keySet()) {
			int df = ngram2df.get(ngram);
			double idf = computeIdf(df, documentsLength);
			System.out.printf("n-gram: %s, n: %d, df: %d, idf: %.2f\n", ngram, documentsLength, df, idf);
			ngram2idf.put(ngram, idf);
		}
		return ngram2idf;
	}
	
	/**
	 * Build the character n-grams idf model.
	 * 
	 * @param contentFilepath A string holding the source file path
	 * @param minN The size of the smallest n-grams to track
	 * @param maxN The size of the largset n-grams to track
	 * @return The n-grams IdfModel object
	 * @throws UIMAException
	 */
	public static IdfModel buildModel(int minN, int maxN, String... contentFilepaths) throws UIMAException {
		if (contentFilepaths == null) {
			throw new NullPointerException("contentFilepaths is null");
		}
		if (minN > maxN) {
			throw new IllegalArgumentException("minN > maxN");
		}
		
		List<Iterable<Analyzable>> analyzableIterables = new ArrayList<>();
		for (String filepath : contentFilepaths) {
			analyzableIterables.add(new SampleFileReader(filepath));
		}
		return buildModel(minN, maxN,
				analyzableIterables.toArray(new Iterable[analyzableIterables.size()]));
	}
	
	/**
	 * Save the idf model.
	 * 
	 * @param idfModel The idf model to serialize
	 * @param modelFile A string holding the serialized model file path
	 */
	public static void saveModel(IdfModel idfModel, String modelFile) {
		if (idfModel == null) {
			throw new NullPointerException("idfModel is null");
		}
		if (modelFile == null) {
			throw new NullPointerException("modelFile is null");
		}
		((IdfStore)idfModel).saveModel(modelFile);
	}
	
	
	private static double computeIdf(int df, int n) {
		return (double) n / df;
	}
	
	public static void test1() { 
		
		IdfModel idfModel = null;
		try {
			idfModel = CharacterNGramIdf.buildModel(1, 3, SAMPLE_CONTENT_PATH);			
		} catch (UIMAException e) { 
			System.out.println("Error while writing building n-gram idf model: " + e.getMessage());
			System.exit(-1);
		}	
		CharacterNGramIdf.saveModel(idfModel, IDF_MODEL_PATH);
	}
	
	public static void test2() {
		Iterable<Analyzable> iterable = new SampleFileReader(SAMPLE_CONTENT_PATH);
		
		IdfModel idfModel = CharacterNGramIdf.buildModel(1, 3, iterable);
		CharacterNGramIdf.saveModel(idfModel, IDF_MODEL_PATH);
	}
	
	public static void test3() throws Exception {
		CharacterNGramMeasure measure = new CharacterNGramMeasure(2, IDF_MODEL_PATH);
		String str1 = "frfa";
		String str2 = "frza";
		System.out.printf("similarity(\"%s\", \"%s\"): %.2f", str1, str2, measure.getSimilarity(str1, str2));
	}
	
	public static void main(String[] args) throws Exception { 
		test1();
		test2();
		//test3();
	}
}
