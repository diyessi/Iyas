package it.unitn.limosine.converters;

import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Lemma;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;

public class LimosineConverter {
	
	private Analyzer analyzer;
	
	private JCas limoCas;
	
	public LimosineConverter(String casesPath) throws UIMAException {
		this.analyzer = new Analyzer(new UIMAFilePersistence(casesPath));
		this.limoCas = JCasFactory.createJCas();
	}
	
	public JCas getLimosineCas() {
		return this.limoCas;
	}
	
	public LimosineConverter readLimosineAnnotations(String limosineXml) {
		this.limoCas.reset();
		
		Analyzable limosineContent = new SimpleContent(limosineXml, "dummy unused content");
		this.analyzer.analyze(this.limoCas, limosineContent);
		
		return this;
	}
	
	public void convertSegmentation(JCas targetCas) {
		
		for(Sentence limoSentence : JCasUtil.select(targetCas, Sentence.class)) {			
			Map<Token, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token>
				limoToDKProTokens = new HashMap<>();
			
			de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence dkProSentence = 
					new de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence(targetCas);
			
			dkProSentence.setBegin(limoSentence.getBegin());
			dkProSentence.setEnd(limoSentence.getEnd());
			dkProSentence.addToIndexes(targetCas);
			
			for(Token limoToken : JCasUtil.selectCovered(targetCas, Token.class, limoSentence)) {
				de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token dkProToken =
					new de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token(targetCas);
				
				dkProToken.setBegin(limoToken.getBegin());
				dkProToken.setEnd(limoToken.getEnd());
				
				limoToDKProTokens.put(limoToken, dkProToken);
			}
			
			for(Lemma limoLemma : JCasUtil.selectCovered(targetCas, Lemma.class, limoSentence)) {
				Token limoToken = limoLemma.getToken();
			
				de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma dkProLemma
					= new de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma(targetCas);
				
				dkProLemma.setBegin(limoLemma.getBegin());
				dkProLemma.setEnd(limoLemma.getEnd());
				dkProLemma.setValue(limoLemma.getLemma());
				dkProLemma.addToIndexes();
				
				limoToDKProTokens.get(limoToken)
					.setLemma(dkProLemma);
			}
			
			for(Pos limoPos : JCasUtil.selectCovered(targetCas, Pos.class, limoSentence)) {
				Token limoToken = limoPos.getToken();
		
				de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS dkProPos
					= new de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS(targetCas);
				
				dkProPos.setBegin(limoPos.getBegin());
				dkProPos.setEnd(limoPos.getEnd());
				dkProPos.setPosValue(limoPos.getPostag());
				dkProPos.addToIndexes();
				
				limoToDKProTokens.get(limoToken)
					.setPos(dkProPos);
			}
			
			for(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token tokenDkPro
					: limoToDKProTokens.values()) {
				tokenDkPro.addToIndexes(targetCas);
			}
		}
	}
	
	public void convertNamedEntities(JCas cas) {
		for(NER limoNe : JCasUtil.select(cas, NER.class)) {
			String limoEntityTag = limoNe.getNetag();
			switch(limoEntityTag) {
			case "LOCATION":
				de.tudarmstadt.ukp.dkpro.core.api.ner.type.Location dkProLocation =
					new de.tudarmstadt.ukp.dkpro.core.api.ner.type.Location(cas);
				dkProLocation.setBegin(limoNe.getBegin());
				dkProLocation.setEnd(limoNe.getEnd());
				dkProLocation.setValue("LOCATION");
				break;
			case "PERSON":
				de.tudarmstadt.ukp.dkpro.core.api.ner.type.Person dkProPerson =
					new de.tudarmstadt.ukp.dkpro.core.api.ner.type.Person(cas);
				dkProPerson.setBegin(limoNe.getBegin());
				dkProPerson.setEnd(limoNe.getEnd());
				dkProPerson.setValue("PERSON");
				break;
			case "ORGANIZATION":
				de.tudarmstadt.ukp.dkpro.core.api.ner.type.Organization dkProOrganization =
					new de.tudarmstadt.ukp.dkpro.core.api.ner.type.Organization(cas);
				dkProOrganization.setBegin(limoNe.getBegin());
				dkProOrganization.setEnd(limoNe.getEnd());
				dkProOrganization.setValue("ORGANIZATION");
				break;
			case "MONEY":
				de.tudarmstadt.ukp.dkpro.core.api.ner.type.Money dkProMoney =
					new de.tudarmstadt.ukp.dkpro.core.api.ner.type.Money(cas);
				dkProMoney.setBegin(limoNe.getBegin());
				dkProMoney.setEnd(limoNe.getEnd());
				dkProMoney.setValue("MONEY");
				break;
			case "DATE":
				de.tudarmstadt.ukp.dkpro.core.api.ner.type.Date dkProDate =
					new de.tudarmstadt.ukp.dkpro.core.api.ner.type.Date(cas);
				dkProDate.setBegin(limoNe.getBegin());
				dkProDate.setEnd(limoNe.getEnd());
				dkProDate.setValue("DATE");
				break;
			case "TIME":
				de.tudarmstadt.ukp.dkpro.core.api.ner.type.Time dkProTime =
					new de.tudarmstadt.ukp.dkpro.core.api.ner.type.Time(cas);
				dkProTime.setBegin(limoNe.getBegin());
				dkProTime.setEnd(limoNe.getEnd());
				dkProTime.setValue("TIME");
				break;
			case "PERCENT":
				de.tudarmstadt.ukp.dkpro.core.api.ner.type.Percent dkProPercent =
					new de.tudarmstadt.ukp.dkpro.core.api.ner.type.Percent(cas);
				dkProPercent.setBegin(limoNe.getBegin());
				dkProPercent.setEnd(limoNe.getEnd());
				dkProPercent.setValue("PERCENT");
				break;
			default:
				de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity dkProNamedEntity =
					new de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity(cas);
				dkProNamedEntity.setBegin(limoNe.getBegin());
				dkProNamedEntity.setEnd(limoNe.getEnd());
				dkProNamedEntity.setValue("MISC");
			}
		}
	}
}
