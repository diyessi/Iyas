package qa.qcri.qf.annotators.arabic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.type.NormalizedText;
import arabicpostagger.wrapper.ArabicAnnotations;
import arabicpostagger.wrapper.ArabicToken;
import arabicpostagger.wrapper.ArabicWrapper;
import arabicpostagger.wrapper.ArabicWrapper.AnalysisMode;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Location;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Organization;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Person;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@TypeCapability(
		outputs = {
				"de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
				"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
				"de.tudarmstadt.ukp.dkpro.core.api.ner.type.Location",
				"de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity",
				"de.tudarmstadt.ukp.dkpro.core.api.ner.type.Organization",
				"de.tudarmstadt.ukp.dkpro.core.api.ner.type.Person"
		})
public class ArabicAnalyzer extends JCasAnnotator_ImplBase {

	public static final String ARABIC_LAN = "Arabic";
	
	private ArabicWrapper aw = null;
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		
		if(this.aw == null) {
			init();
		}
		
		List<String> bioTags = new ArrayList<>();
		
		String originalText = cas.getDocumentText();			
		try {
			ArabicAnnotations annotations = this.aw.annotateText(originalText);
			
			NormalizedText normalizedText = new NormalizedText(cas);
			normalizedText.setBegin(0);
			normalizedText.setEnd(annotations.getNormalizedText().length());
			normalizedText.setText(annotations.getNormalizedText());
			normalizedText.addToIndexes(cas);
			
			List<NamedEntity> nes = new ArrayList<>();
			NamedEntity ne = null;
				
			for(ArabicToken arabicToken : annotations.getArabicTokens()) {
				Token token = new Token(cas);
				token.setBegin(arabicToken.beginPos);
				token.setEnd(arabicToken.endPos);
				String posTag = arabicToken.getPosTag();
				if(!posTag.isEmpty()) {
					POS pos = new POS(cas);
					pos.setBegin(token.getBegin());
					pos.setEnd(token.getEnd());
					pos.setPosValue(posTag);
					pos.addToIndexes(cas);
					token.setPos(pos);
				}
				token.addToIndexes(cas);
				
				// Named entity extraction
				/*
				String bioTag = arabicToken.getBioTag();
				
				bioTags.add(bioTag);
				
				if(bioTag.startsWith("B-")) {	
					if(bioTag.endsWith("PERS")) {
						ne = new Person(cas);
						ne.setValue("PERSON");
					} else if(bioTag.endsWith("ORG")) {
						ne = new Organization(cas);
						ne.setValue("ORGANIZATION");
					} else if(bioTag.endsWith("LOC")) {
						ne = new Location(cas);
						ne.setValue("LOCATION");
					}
					
					if(ne == null) {
						System.out.println("NE null: " + bioTag);
					}
					
					ne.setBegin(token.getBegin());
					ne.setEnd(token.getEnd());
					
					nes.add(ne);
					
				} else if (bioTag.startsWith("I-")) {
					if(ne == null) {
						System.out.println("NE null: " + bioTag);
						for(String tag : bioTags) {
							System.out.println(tag);
							System.out.println(cas.getDocumentText());
						}
					}
					ne.setEnd(token.getEnd());
				} else {
					ne = null;
				}
				*/
			}
			
			for(NamedEntity namedEntity : nes) {
				namedEntity.addToIndexes(cas);
			}
			
		} catch (ClassNotFoundException | IOException | InterruptedException e) {
			e.printStackTrace();
		}	
	}

	private void init() {
		try {
			this.aw = new ArabicWrapper(AnalysisMode.NER, null, false);
		} catch (ClassNotFoundException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
