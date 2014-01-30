package qa.qcri.qf.datagen.rr;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.datagen.DataObject;
import qa.qcri.qf.datagen.DataPair;
import qa.qcri.qf.datagen.Pairer;
import qa.qcri.qf.features.PairFeatures;
import qa.qcri.qf.features.PairFeatureFactory;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import util.Pair;

/**
 * 
 * Generates the training data for reranking
 * 
 * TODO: the logic about retrieving trees, the marking and producing the token
 * representation should be factored out to a central "settings" class
 */
public class RerankingTrain implements Reranking {

	public static final String DEFAULT_OUTPUT_TRAIN_FILE = "svm.train";

	private FileManager fm;

	private String outputDir;

	private String outputFile;

	private Analyzer ae;

	private TreeSerializer ts;

	private PairFeatureFactory pairFeatureFactory;

	private JCas questionCas;
	private JCas leftCandidateCas;
	private JCas rightCandidateCas;

	private String parameterList;

	public RerankingTrain(FileManager fm, String outputDir, Analyzer ae,
			TreeSerializer ts, PairFeatureFactory pairFeatureFactory)
			throws UIMAException {
		this.fm = fm;
		this.outputDir = outputDir;
		this.outputFile = outputDir + DEFAULT_OUTPUT_TRAIN_FILE;
		this.ae = ae;

		this.ts = ts;

		this.pairFeatureFactory = pairFeatureFactory;

		this.questionCas = JCasFactory.createJCas();
		this.leftCandidateCas = JCasFactory.createJCas();
		this.rightCandidateCas = JCasFactory.createJCas();

		this.parameterList = "";
	}

	/**
	 * Sets the outputFile. If the method is not called then the default output
	 * file name is used
	 * 
	 * @param outputFile
	 */
	public void setOutputFile(String outputFile) {
		this.outputFile = this.outputDir + outputFile;
	}

	@Override
	public void generateData(DataObject questionObject,
			List<DataObject> candidateObjects) {
		List<DataPair> pairs = pairQuestionWithCandidates(questionObject,
				candidateObjects);

		this.ae.analyze(this.questionCas,
				new SimpleContent(questionObject.getId(), ""));

		List<Pair<DataPair, DataPair>> trainingPairs = Pairer.pair(pairs);

		for (Pair<DataPair, DataPair> trainingPair : trainingPairs) {
			DataPair leftPair = trainingPair.getA();
			DataPair rightPair = trainingPair.getB();

			DataObject lCandidate = leftPair.getB();
			DataObject rCandidate = rightPair.getB();

			TokenTree leftQuestionTree = RichTree
					.getPosChunkTree(this.questionCas);
			TokenTree rightQuestionTree = RichTree
					.getPosChunkTree(this.questionCas);

			this.ae.analyze(this.leftCandidateCas,
					new SimpleContent(lCandidate.getId(), ""));
			this.ae.analyze(this.rightCandidateCas, new SimpleContent(
					rCandidate.getId(), ""));

			TokenTree leftCandidateTree = RichTree
					.getPosChunkTree(this.leftCandidateCas);
			TokenTree rightCandidateTree = RichTree
					.getPosChunkTree(this.rightCandidateCas);

			MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(
					new MarkTwoAncestors());

			marker.markTrees(leftQuestionTree, leftCandidateTree,
					this.parameterList);
			marker.markTrees(rightQuestionTree, rightCandidateTree,
					this.parameterList);

			PairFeatures pfLeft = this.pairFeatureFactory.getPairFeatures(
					questionCas, leftCandidateCas);
			PairFeatures pfRight = this.pairFeatureFactory.getPairFeatures(
					questionCas, rightCandidateCas);

			StringBuffer sb = new StringBuffer(1024 * 4);
			String label = leftPair.isPositive() ? "+1" : "-1";
			sb.append(label);
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(leftQuestionTree,
					this.parameterList));
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(leftCandidateTree,
					this.parameterList));
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(rightQuestionTree,
					this.parameterList));
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(rightCandidateTree,
					this.parameterList));
			sb.append(" |ET| ");
			sb.append(pfLeft.serializeIndexedFeatures(1));
			sb.append(" |BV| ");
			sb.append(pfRight.serializeIndexedFeatures(1));
			sb.append(" |EV| ");

			this.fm.writeLn(this.outputFile, sb.toString());
		}
	}

	/**
	 * Set the list of parameters used to retrieve the token representation
	 * 
	 * @param parameterList
	 * @return
	 */
	public RerankingTrain setParameterList(String parameterList) {
		this.parameterList = parameterList;
		return this;
	}

	/**
	 * Produces a list of DataPair. The right part of the pair is the question
	 * DataObject, the left part of the pair is the candidate DataObject
	 * 
	 * @param question
	 * @param candidates
	 * @return the list of DataPair
	 */
	private List<DataPair> pairQuestionWithCandidates(DataObject question,
			List<DataObject> candidates) {

		List<DataPair> pairs = new ArrayList<>();

		for (DataObject candidate : candidates) {
			pairs.add(new DataPair(candidate.getLabel(), question.getId() + "-"
					+ candidate.getId(), DataObject.newFeaturesMap(),
					DataObject.newMetadataMap(), question, candidate));
		}

		return pairs;
	}
}
