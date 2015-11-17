package qa.qcri.qf.features.cooking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import qa.qcri.qf.type.Acknowledgment;

public class AcknowledgmentAnnotator extends JCasAnnotator_ImplBase {

	private Pattern ackPatterns[];
	
	//TODO
	//load the acknowledgments-related vocabulary
	//search for the required patterns
	//have them ready in the JCas for reading 
	
	/* (non-Javadoc)
	 * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(org.apache.uima.UimaContext)
	 */
	public void initialize(UimaContext aContext) 
					throws ResourceInitializationException {
		super.initialize(aContext);
		// We load the acknowledgments (pseudo-)patterns which are defined in 
		// the AcknowledgmentAnnotatorDescriptor.xml file, under the name 
		// AckPatterns. Having that list there prevents from modifying this code. 
		String[] ackStrings = 
				  (String[]) aContext.getConfigParameterValue("AckPatterns");
		
		// We compile these strings into patterns
		// Note (ABC): I know we don't actually need this regex to 
		ackPatterns = new Pattern[ackStrings.length];
		for (int i = 0 ; i<ackStrings.length ; i++) {
			ackPatterns[i] = Pattern.compile(ackStrings[i]);
		}
		
	}
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		Matcher matcher;
		int pos;
		// get document text
		String docText = aJCas.getDocumentText();
		
		// Note (ABC): I know this is not the most efficient way. We might not
		// be even interested in finding the exact location of the word. Still,
		// by now I code it this way as an example for further instances.
		for (Pattern pt : ackPatterns) {
			matcher = pt.matcher(docText);
			pos = 0;
			while (matcher.find(pos)){
				// found an ack - create annotation
				Acknowledgment annotation = new Acknowledgment(aJCas);
				annotation.setBegin(matcher.start());
				annotation.setEnd(matcher.end());
				annotation.addToIndexes();
				pos = matcher.end();
			}
			
		}
		

	}

}
