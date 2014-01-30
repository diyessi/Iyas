package qa.qcri.qf.datagen.rr;

import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.datagen.DataObject;
import qa.qcri.qf.features.PairFeatures;
import qa.qcri.qf.features.cosine.PairFeatureFactory;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;

/**
 * 
 * Generates the test data for reranking
 * 
 * TODO: the logic about retrieving trees, the marking and producing the token
 * representation should be factored out to a central "settings" class
 */
public class RerankingTest implements Reranking {

	public static final String DEFAULT_OUTPUT_TEST_FILE = "svm.test";

	private FileManager fm;

	private String outputDir;

	private String outputFile;

	private Analyzer ae;

	private TreeSerializer ts;

	private PairFeatureFactory pairFeatureFactory;

	private JCas questionCas;

	private JCas candidateCas;

	private String parameterList;

	public RerankingTest(FileManager fm, String outputDir, Analyzer ae,
			TreeSerializer ts, PairFeatureFactory pairFeatureFactory)
			throws UIMAException {
		this.fm = fm;
		this.outputDir = outputDir;
		this.outputFile = outputDir + DEFAULT_OUTPUT_TEST_FILE;
		this.ae = ae;

		this.ts = ts;

		this.pairFeatureFactory = pairFeatureFactory;

		this.questionCas = JCasFactory.createJCas();
		this.candidateCas = JCasFactory.createJCas();

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
		this.ae.analyze(this.questionCas,
				new SimpleContent(questionObject.getId(), ""));

		for (DataObject candidateObject : candidateObjects) {
			TokenTree questionTree = RichTree.getPosChunkTree(this.questionCas);

			this.ae.analyze(this.candidateCas, new SimpleContent(
					candidateObject.getId(), ""));

			TokenTree candidateTree = RichTree
					.getPosChunkTree(this.candidateCas);

			MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(
					new MarkTwoAncestors());

			marker.markTrees(questionTree, candidateTree, this.parameterList);

			PairFeatures pf = this.pairFeatureFactory.getPairFeatures(
					this.questionCas, this.candidateCas);

			StringBuffer sb = new StringBuffer(1024 * 4);
			String label = candidateObject.isPositive() ? "+1" : "-1";
			sb.append(label);
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(questionTree, this.parameterList));
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(candidateTree, this.parameterList));
			sb.append(" |BT| ");
			sb.append(" |BT| ");
			sb.append(" |ET| ");
			sb.append(pf.serializeIndexedFeatures(1));
			sb.append(" |BV| ");
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
	public RerankingTest setParameterList(String parameterList) {
		this.parameterList = parameterList;
		return this;
	}
}
