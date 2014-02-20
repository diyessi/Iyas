package arabicpostagger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArabicWrapper {
	
	public enum AnalysisMode {
	    TOKENIZATION,
	    POSTAGGING,
	    NER
	}
	
	public static final String SAMPLE_SENTENCE = "قال بيان اصدره حزب الحريه والعداله، الذراع السياسي لجماعه الإخوان المسلمين، حصلت \"رصد\" علي نسخه منه، بشان الاحداث الدمويه بالمدينه الجامعيه بالازهر، ان الانقلابيين فقدوا صوابهم، ويحاولون صرف الانظار عن فشلهم في تحقيق الامن للمصريين، وحمايه جنودنا بسيناء في مشهد يؤكد تكراره تورط الانقلابيين في تدبيره.";
	
	private AnalysisMode analysisMode = AnalysisMode.TOKENIZATION;
	private String kenlmDirectory = null;
	private boolean denormalizeText = false;
	private String dataDirectory = null;
	private POSAnnotator tagger = null;
	private ArabicNERAnnotator ner = null;
	private DenormalizeText dnt = null;
	
	public ArabicWrapper(AnalysisMode analysisMode, String kenlmDirectory, boolean denormalizeText)
			throws FileNotFoundException, ClassNotFoundException, IOException, InterruptedException {
		this.analysisMode = analysisMode;
		this.kenlmDirectory = kenlmDirectory;
		this.denormalizeText = denormalizeText;
		
		this.dataDirectory = System.getProperty("java.library.path");
		if(!this.dataDirectory.endsWith("/")) {
			this.dataDirectory += "/";
		}
		
		this.tagger = new POSAnnotator(this.dataDirectory);
		
		if(this.denormalizeText) {
			this.dnt = new DenormalizeText(dataDirectory, this.kenlmDirectory);
		}
		
		if(this.analysisMode == AnalysisMode.NER) {
			this.ner = new ArabicNERAnnotator(this.dataDirectory, this.tagger);
		}
	}
	
	public ArabicAnnotations annotateText(String text)
			throws FileNotFoundException, ClassNotFoundException, IOException, InterruptedException {
		
		if(this.denormalizeText) {
			text = this.dnt.denormalize(text);
		}
		
        List<String> output = this.doTagging(text);
        
        output = this.removeInvalidOutput(output);
        
        return createArabicAnnotationsFromOutput(output);
	}
	
	public List<String> doTagging(String text)
			throws IOException, InterruptedException, ClassNotFoundException {
		switch(this.analysisMode) {
		case TOKENIZATION:
			return this.doTokenization(text);
		case POSTAGGING:
			return this.doPOSTagging(text);
		case NER:
			return this.doNER(text);
		default:
			return this.doTokenization(text);
		}
	}
	
	public List<String> doTokenization(String text) throws IOException, InterruptedException {
		return this.tagger.tag(text, true, false);
	}
	
	public List<String> doPOSTagging(String text) throws IOException, InterruptedException {
		return this.tagger.tag(text, false, false);
	}
	
	public List<String> doNER(String text) throws IOException, InterruptedException, ClassNotFoundException {
		return this.ner.tag(text, true);
	}
	
	public List<String> removeInvalidOutput(List<String> output) {
        for(Iterator<String> i = output.iterator(); i.hasNext(); ) {
        	String s = i.next();
        	if(s.equals(s.equals("-") || s.equals("_"))) {
        		i.remove();
        	}
        }
        return output;
	}
	
	public ArabicAnnotations createArabicAnnotationsFromOutput(List<String> output) {
        List<ArabicToken> arabicTokens = new ArrayList<>();
        String normalizedText = "";
        int currentPos = 0;
        for(String token : output) {
        	String[] tokenParts = token.split("/");
        	String tokenSurface = tokenParts[0];
        	
        	ArabicToken arabicToken = new ArabicToken(tokenSurface,
        			currentPos, currentPos + tokenSurface.length());
        	
        	arabicToken.setOriginalTaggedToken(token);
        	
        	currentPos += tokenSurface.length() + 1;
        	normalizedText += tokenSurface + " ";
        	
        	if(tokenParts.length > 1) {
        		String posTag = tokenParts[1];
        		arabicToken.setPosTag(posTag);
        	}
        	
        	if(tokenParts.length > 2) {
        		String bioTag = tokenParts[2];
        		arabicToken.setBioTag(bioTag);
        	}
        	
        	arabicTokens.add(arabicToken);
        }
        
        return new ArabicAnnotations(arabicTokens, normalizedText.trim());
	}
	
	public static void main(String[] args) {
		try {
			ArabicWrapper aw = new ArabicWrapper(AnalysisMode.NER, null, false);
			ArabicAnnotations annotations = aw.annotateText(SAMPLE_SENTENCE);
			
			String normalizedText = annotations.getNormalizedText();
			
			for(ArabicToken token : annotations.getArabicTokens()) {
				String tokenFromText = normalizedText.substring(token.beginPos, token.endPos);
				System.out.println(token.surfaceForm + " " + tokenFromText + " "
						+ token.getPosTag() + " " + token.getBioTag() + " | " + token.getOriginalTaggedToken());		
			}
			
		} catch (ClassNotFoundException | IOException | InterruptedException e) {
			System.out.println("Error instantiating the software.");
		}
	}
}
