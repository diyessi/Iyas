package it.unitn.limosine.emd;

import it.unitn.limosine.types.emd.EMD;
import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.SharedModel;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

public class NERasEMD extends JCasAnnotator_ImplBase{

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
	
		 FSIndex<Annotation> NERIndex = cas.getAnnotationIndex(NER.type);
		 
		 
		 Iterator<Annotation> NERIterator = NERIndex.iterator(); 


		 while (NERIterator.hasNext()) {
			 NER ner = (NER) NERIterator.next();

		       	EMD mcas=new EMD(cas);
	        	String acetype="NAM";
	        	mcas.setACEtype(acetype);
	        	mcas.setBegin(ner.getBegin());
	        	mcas.setEnd(ner.getEnd());
	        	mcas.setStartToken(ner.getStartToken());
	        	mcas.setStartTokenMIN(ner.getStartToken());
	        	mcas.setEndToken(ner.getEndToken());
	        	mcas.setEndTokenMIN(ner.getEndToken());
	        	mcas.setHeadToken(ner.getEndToken()); // this is very rough, but..
	        	mcas.setHead(ner.getEndToken().getCoveredText()); //B: add entity type
	        	mcas.setEntityType(ner.getNetag());
	        	mcas.setSemanticType(ner.getNetag());
	        	mcas.setAnnotatorID(getClass().getCanonicalName());
	        	mcas.addToIndexes();
	        	
		 }
	}	
}
