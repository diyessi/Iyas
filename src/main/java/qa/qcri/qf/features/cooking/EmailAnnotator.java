package qa.qcri.qf.features.cooking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

public class EmailAnnotator extends JCasAnnotator_ImplBase{

	public static final String PARAM_LANGUAGE = "language";
	
	private Pattern emailPattern;
	
	/* (non-Javadoc)
	 * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(org.apache.uima.UimaContext)
	 */
	public void initialize(UimaContext aContext) 
					throws ResourceInitializationException {
		super.initialize(aContext);
		// We load the email pattern, which is defined in the 
		// EmailAnnotatorDescriptor.xml file, under the name EmailPattern.  
		String mailPattern = 
				(String) aContext.getConfigParameterValue("EmailPattern"); 
		emailPattern = Pattern.compile(mailPattern);
	}
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		Matcher matcher;
		int pos;
		// get document text
		String docText = aJCas.getDocumentText();
		matcher = emailPattern.matcher(docText);
		pos = 0;
		System.out.println("before");
		while (matcher.find(pos)){
			System.out.println("inside");
			// found an email - create annotation
			Email annotation = new Email(aJCas);
			annotation.setBegin(matcher.start());
			annotation.setEnd(matcher.end());
			annotation.addToIndexes();
			pos = matcher.end();
		}	
	}
}
