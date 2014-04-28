package qa.qcri.qf.annotators;

import java.io.File;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
//import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import qa.qcri.qf.classifiers.OneVsAllClassifier;
import qa.qcri.qf.classifiers.SVMLightTKClassifierFactory;
import qa.qcri.qf.tools.questionclassifier.Commons;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.type.QuestionClass;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;

@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent" }, outputs = { "qa.qcri.qf.type.QuestionClass" })
public class QuestionClassifier extends JCasAnnotator_ImplBase {
	
	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name=PARAM_LANGUAGE, mandatory=true, description="The language of the document to be processed")
	private String language;
	
	public static final String PARAM_MODELS_DIRPATH = "modelsDirpath";
	@ConfigurationParameter(name=PARAM_MODELS_DIRPATH, mandatory=true, description="Directory to read the question-classifier models from")
	private String modelsDirpath;
	
	private OneVsAllClassifier classifier;

	private TreeSerializer ts;

	private String parameterList;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		this.ts = new TreeSerializer();
		this.parameterList = Commons.getParameterList();
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

		String tree = ts.serializeTree(RichTree.getConstituencyTree(cas),
				this.parameterList);

		String example = "|BT| " + tree + " |ET|";
		String questionClass = this.classifier.getMostConfidentModel(example);

		addQuestionClassAnnotation(cas, questionClass);
	}

	/**
	 * Adds a question class annotation to the CAS
	 * @param cas
	 * @param questionClass
	 */
	private void addQuestionClassAnnotation(JCas cas, String questionClass) {
		QuestionClass annotation = new QuestionClass(cas);
		annotation.setQuestionClass(questionClass);
		annotation.setBegin(0);
		annotation.setEnd(cas.getDocumentText().length());
		annotation.addToIndexes();
	}

	/**
	 * Instantiates the One-vs-All classifier loading the models
	 * produced by the question-classifier train/test components
	 */
	private void init() {
		this.classifier = new OneVsAllClassifier(
				new SVMLightTKClassifierFactory());
		
		//System.out.println("modelsDirpath: " + modelsDirpath);
		File file = new File(modelsDirpath);
		
		for (String modelName : file.list()) {
			int dotPos = modelName.indexOf(".");
			String category = modelName.substring(0, dotPos);
			String modelFilepath = new File(modelsDirpath, modelName).toString();
			this.classifier.addModel(category, modelFilepath);
		}
		
		/*
		for (String category : Commons.CATEGORIES) {
			this.classifier.addModel(category, Commons.MODELS_DIRECTORY
					+ category + ".model");
		}
		*/
	}

}
