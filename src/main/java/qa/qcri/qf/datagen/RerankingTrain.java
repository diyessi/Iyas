package qa.qcri.qf.datagen;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import util.Pair;

public class RerankingTrain {
	
	private FileManager fm;
	
	private String outputPath;
	
	private Analyzer ae;
	
	private TreeSerializer ts;
	
	private JCas questionCas;
	private JCas leftCandidateCas;
	private JCas rightCandidateCas;
	
	private String parameterList;
	
	public RerankingTrain(FileManager fm, String outputPath, Analyzer ae) throws UIMAException {
		this.fm = fm;
		this.fm.create(outputPath);
		this.outputPath = outputPath;
		this.ae = ae;
		
		this.ts = new TreeSerializer().enableRelationalTags();
		
		this.questionCas = JCasFactory.createJCas();
		this.leftCandidateCas = JCasFactory.createJCas();
		this.rightCandidateCas = JCasFactory.createJCas();
		
		this.parameterList = "";
	}
	
	public void generateData(DataObject questionObject, List<DataObject> candidateObjects) {
		List<DataPair> pairs = pairQuestionWithCandidates(questionObject, candidateObjects);
		
		this.ae.analyze(this.questionCas, new SimpleContent(questionObject.getId(), ""));
		
		List<Pair<DataPair, DataPair>> trainingPairs = Pairer.pair(pairs);
		
		for (Pair<DataPair, DataPair> trainingPair : trainingPairs) {
			DataPair leftPair = trainingPair.getA();
			DataPair rightPair = trainingPair.getB();

			DataObject lCandidate = leftPair.getB();
			DataObject rCandidate = rightPair.getB();

			TokenTree leftQuestionTree = RichTree.getPosChunkTree(this.questionCas);
			TokenTree rightQuestionTree = RichTree.getPosChunkTree(this.questionCas);

			this.ae.analyze(this.leftCandidateCas, new SimpleContent(lCandidate.getId(), ""));
			this.ae.analyze(this.rightCandidateCas, new SimpleContent(rCandidate.getId(), ""));

			TokenTree leftCandidateTree = RichTree.getPosChunkTree(this.leftCandidateCas);
			TokenTree rightCandidateTree = RichTree.getPosChunkTree(this.rightCandidateCas);
			
			MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(
					new MarkTwoAncestors());
			
			marker.markTrees(leftQuestionTree, leftCandidateTree, this.parameterList);
			marker.markTrees(rightQuestionTree, rightCandidateTree, this.parameterList);
			
			StringBuffer sb = new StringBuffer(1024 * 4);
			String label = leftPair.isPositive() ? "+1" : "-1";
			sb.append(label);
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(leftQuestionTree, this.parameterList));
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(leftCandidateTree, this.parameterList));
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(rightQuestionTree, this.parameterList));
			sb.append(" |BT| ");
			sb.append(this.ts.serializeTree(rightCandidateTree, this.parameterList));
			sb.append(" |ET| ");

			this.fm.writeLn(this.outputPath, sb.toString());
		}
	}
	
	public void close() {
		this.fm.close(this.outputPath);
	}
	
	public RerankingTrain setParameterList(String parameterList) {
		this.parameterList = parameterList;
		return this;
	}
	
	private List<DataPair> pairQuestionWithCandidates(DataObject question,
			List<DataObject> candidates) {
		
		List<DataPair> pairs = new ArrayList<>();

		for (DataObject candidate : candidates) {
			pairs.add(new DataPair(candidate.getLabel(),
					question.getId() + "-" + candidate.getId(),
					DataObject.newFeaturesMap(), DataObject.newMetadataMap(),
					question, candidate));
		}
		
		return pairs;
	}
}
