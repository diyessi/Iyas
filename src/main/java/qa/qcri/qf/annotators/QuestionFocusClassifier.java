package qa.qcri.qf.annotators;

import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import qa.qcri.qf.classifiers.Classifier;
import qa.qcri.qf.classifiers.SVMLightTKClassifierFactory;
import qa.qcri.qf.tools.questionfocus.FocusClassifier;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import qa.qcri.qf.type.QuestionFocus;
import util.Pair;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;

@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent" }, outputs = { "qa.qcri.qf.type.QuestionFocus" })
public class QuestionFocusClassifier extends JCasAnnotator_ImplBase {

	public static final String MODEL_PATH = "data/question-focus/svm.model";

	private TreeSerializer ts;

	private Classifier classifier;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		this.ts = new TreeSerializer();
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {

		// Return early if no Constituent is present
		if (!JCasUtil.exists(cas, Constituent.class))
			return;

		// Lazy loading
		if (this.classifier == null) {
			init();
		}
		
		TokenTree tree = RichTree.getConstituencyTree(cas);
		
		List<Pair<String, RichTokenNode>> examples 
			= FocusClassifier.generateExamples(tree, this.ts);
		
		RichTokenNode focusNode = null;
		
		Double maxPrediction = Double.NEGATIVE_INFINITY;
		
		for(Pair<String, RichTokenNode> example : examples) {
			Double prediction = this.classifier.classify(example.getA());
			
			if(prediction > maxPrediction) {
				maxPrediction = prediction;
				focusNode = example.getB();
			}
		}
		
		if(focusNode != null) {
			Token focusToken = focusNode.getToken();
			QuestionFocus annotation = new QuestionFocus(cas);
			annotation.setBegin(focusToken.getBegin());
			annotation.setEnd(focusToken.getEnd());
			annotation.setFocus(focusToken);
			annotation.addToIndexes(cas);
		}
	}

	private void init() {
		this.classifier = new SVMLightTKClassifierFactory()
				.createClassifier(MODEL_PATH);
	}
}
