package qa.qcri.qf.semeval2015_3.wordspace;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import qa.qcri.qf.semeval2015_3.textnormalization.TextNormalizer;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class WordSpace {


	private HashMap <String, DenseMatrix64F> wordVectors;
	private int spaceDimensionality;
	
	/**
	 * @return the spaceDimensionality
	 */
	public int getSpaceDimensionality() {
		return spaceDimensionality;
	}

	private static final StanfordCoreNLP pipeline;
	static{
		Properties props = new Properties();

		// props.put("annotators",
		// "tokenize, ssplit, pos, lemma, ner, parse, dcoref");

		props.put("annotators", "tokenize, ssplit, pos, lemma");
		pipeline = new StanfordCoreNLP(props);
	}
	
	public WordSpace(String filename) throws FileNotFoundException, IOException{
		wordVectors = new HashMap<String, DenseMatrix64F>();
		
		BufferedReader br = null;
		GZIPInputStream gzis = null;
		if (filename.endsWith(".gz")) {
			gzis = new GZIPInputStream(new FileInputStream(filename));
			InputStreamReader reader = new InputStreamReader(gzis, "UTF8");
			br = new BufferedReader(reader);
		} else {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					filename), "UTF8"));
		}
		String line;
		ArrayList<String> split;

		String label;
		String[] vSplit;

		
		Pattern iPattern = Pattern.compile(",");
		float[] v = null;
		
		line = br.readLine();
		split = mySplit(line, ' ');
		this.spaceDimensionality = Integer.valueOf(split.get(1));
		
		
		while ((line = br.readLine()) != null) {
			if (!line.contains("\t"))
				continue;
			float norm2 = 0;
			split = mySplit(line, '\t');
			label = split.get(0);
			//idf = Float.parseFloat(split.get(1));
			vSplit = iPattern.split(split.get(3), 0);
			if (v == null)
				v = new float[vSplit.length];
			for (int i = 0; i < v.length; i++) {
				v[i] = Float.parseFloat(vSplit[i]);
				norm2 += v[i] * v[i];
			}
			float norm = (float) Math.sqrt(norm2);
			for (int i = 0; i < v.length; i++) {
				v[i] /= norm;
			}
			DenseMatrix64F featureVector = new DenseMatrix64F(1, this.spaceDimensionality);
			
			for (int i = 0; i < v.length; i++) {
				featureVector.set(0, i, (double) v[i]);
			}
			
			wordVectors.put(label, featureVector);
	
		}

		if (filename.endsWith(".gz")) {
			gzis.close();
		}
		br.close();
	}
	
	private ArrayList<String> mySplit(String s, char separator) {
		char[] c = (s).toCharArray();
		ArrayList<String> ll = new ArrayList<String>();
		int index = 0;
		for (int i = 0; i < c.length; i++) {
			if (c[i] == separator) {
				ll.add(s.substring(index, i));
				index = i + 1;
			}
		}
		ll.add(s.substring(index, s.length()));
		return ll;
	}
	
	public DenseMatrix64F getFeatureVector(String lemma, String POS){
		String key = lemma + "::" + POS.toLowerCase().charAt(0);
		return this.wordVectors.get(key);
	}
	
	public DenseMatrix64F sentence2vector(String sentence){
		DenseMatrix64F vector = new DenseMatrix64F(1, this.spaceDimensionality);
		String normalizedSentence = TextNormalizer.normalize(sentence);
		Annotation document = new Annotation(normalizedSentence);

		// run all Annotators on this text
		
	
		pipeline.annotate(document);

				

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for(CoreMap currentSentence: sentences) {
			//System.out.println("sentence: " + sentence);
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			List<CoreLabel> tokens = currentSentence.get(TokensAnnotation.class);
			
			for (CoreLabel token: tokens) {

				
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				// this is the NER label of the token

				// this is the LEMMA label of the token
				String lemma = token.getString(LemmaAnnotation.class);
				DenseMatrix64F wordVector = this.getFeatureVector(lemma, pos);
				if(wordVector!=null){
					CommonOps.addEquals(vector, wordVector);	
				}
				
			}
			
			
		}
		return vector;
	}
	
	public DenseMatrix64F sentence2vector(String sentence, Set<Character> posToBeConsidered){
		DenseMatrix64F vector = new DenseMatrix64F(1, this.spaceDimensionality);
		String normalizedSentence = TextNormalizer.normalize(sentence);
		Annotation document = new Annotation(normalizedSentence);

		// run all Annotators on this text
		
	
		pipeline.annotate(document);

				

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for(CoreMap currentSentence: sentences) {
			//System.out.println("sentence: " + sentence);
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			List<CoreLabel> tokens = currentSentence.get(TokensAnnotation.class);
			
			for (CoreLabel token: tokens) {

				
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				if(!posToBeConsidered.contains(pos.toLowerCase().charAt(0))){
					continue;
				}

				// this is the LEMMA label of the token
				String lemma = token.getString(LemmaAnnotation.class);
				DenseMatrix64F wordVector = this.getFeatureVector(lemma, pos);
				if(wordVector!=null){
					CommonOps.addEquals(vector, wordVector);
				}
					
			}
			
			
		}
		return vector;
	}
}
