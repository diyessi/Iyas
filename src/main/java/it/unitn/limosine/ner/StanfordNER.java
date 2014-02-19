package it.unitn.limosine.ner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.util.*;
import it.unitn.limosine.util.StanfordToken;


import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;

public class StanfordNER extends JCasAnnotator_ImplBase {

	private CRFClassifier<CoreLabel> classifier; 

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			
		//initialize NER tagger model, read from resource manager
				
		SharedModel sharedModel = (SharedModel) getContext().getResourceObject("NERModelName");
		String modelName = sharedModel.getPath();
		this.classifier = CRFClassifier.getClassifier(new File(modelName).getAbsolutePath());
		} catch (ClassCastException e) {
			e.printStackTrace();
		}  catch (ResourceAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		
		//needs input from tokenizer
		
		 FSIndex<?> sentencesIndex = cas.getAnnotationIndex(Sentence.type);
		 
		 //iterate over all sentences
		 
		 Iterator<?> sentenceIterator = sentencesIndex.iterator();
		 while (sentenceIterator.hasNext()) {
			 Sentence sentence = (Sentence) sentenceIterator.next();
			 List<AnnotationFS> myTokens = JCasUtility.selectCovered(cas, Token.class, sentence);
			 ArrayList<StanfordToken> tokensOfSentence = new ArrayList<StanfordToken>();
			 for (AnnotationFS tok : myTokens) {
				 Token token = (Token)tok;
				 tokensOfSentence.add(new StanfordToken(token)); 
			 }	
			 
			 
			 final List<CoreLabel> ner = classifier.classifySentence(tokensOfSentence);    
			 int i=-1;
			 
			 String oldtag="O";
			 NER ne=null;
			 for (CoreLabel label : ner)
		
			 { 
				 i++;				 
				String netag=label.get(AnswerAnnotation.class);
				//TODO: they should actually be IOB -- but no traces of those in stanford docs :(
				// quick hack: consider sequences with the same netag to span over a single NE
				
				if (netag.equals(oldtag)) continue;
				if (i>0 && ne !=null) {
					AnnotationFS token=myTokens.get(i-1);
					ne.setEnd(token.getEnd());	
					ne.setEndToken((Token)token);
					ne.setAnnotatorId(getClass().getCanonicalName());
					ne.addToIndexes();
				}
				oldtag=netag;
				if (netag.equals("O")) {
					ne=null;
					continue;
				}
				ne=new NER(cas);
				AnnotationFS token=myTokens.get(i);
				ne.setBegin(token.getBegin());
				ne.setStartToken((Token)token);
				ne.setNetag(netag);

				
			 }
		 }
		 

	}

}
