package qa.qcri.qf.features.heuristics;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.type.Acknowledgment;


public class AcknowledgmentAnnotator extends JCasAnnotator_ImplBase{

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		//we load the text from the document
		String txt = aJCas.getDocumentText();
		
		//Looking at acks. according to AlbertoSimoneFeatureExtractor 
		//containsAcknowledge
		//TODO this should be a regular expression and a loop on the whole
		//text should exist for identifying every occurrence of the desired
		//strings
		Boolean boo = false;		
		if(txt.contains("thank") || txt.contains("appreciat")){
			boo = true;
		}
		
		//This is the invocation of the tipe
		Acknowledgment acknowledgmentAnnotation = new Acknowledgment(aJCas);
		acknowledgmentAnnotation.setAck(boo);
		//to be added to the CAS indexes (it wont be found later, otherwise)
		acknowledgmentAnnotation.addToIndexes();
	}

}
