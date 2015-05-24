package qa.qcri.qf.semeval2015_3.textnormalization;


public class TextNormalizer {

	public static String normalize(String informalText){
		String normalizedText = informalText;
		
		normalizedText = normalizedText.replaceAll("!+", "!");
		
		normalizedText = normalizedText.replaceAll("[?]+", "?");
		
		normalizedText = normalizedText.replaceAll("[?,!]+", "?");
		
		normalizedText = normalizedText.replaceAll("[.]+", ".");
		
		normalizedText = normalizedText.replaceAll("([a-zA-Z])(\\1{2,})", "$1");
		
		if(normalizedText.equals(normalizedText.toUpperCase())){
			normalizedText = normalizedText.toLowerCase();
		}
		
		return normalizedText;
		
	}
	
}
