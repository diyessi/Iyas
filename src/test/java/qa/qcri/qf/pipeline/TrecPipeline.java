package qa.qcri.qf.pipeline;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.datagen.DataObject;
import qa.qcri.qf.datagen.DataPair;
import qa.qcri.qf.datagen.Labelled;
import qa.qcri.qf.datagen.Pairer;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.trees.RichNode;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import util.ChunkReader;
import util.Pair;
import util.functions.InStringOutString;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerChunkerTT4J;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;

public class TrecPipeline {

	public static final String CASES_DIRECTORY = "CASes-trec/";
	public static final String QUESTIONS_PATH = "data/trec/questions.txt";
	public static final String CANDIDATES_PATH = "data/trec/candidates.txt";
	
	public static final String QUESTION_ID_KEY = "QUESTION_ID_KEY";
	
	public static final String TRAIN_SVM = "train/svm.train";

	private Analyzer ae;

	private Map<String, DataObject> idToQuestion = new HashMap<>();
	
	private FileManager fm;

	public TrecPipeline() throws UIMAException {
		this.ae = this.instantiateAnalyzer();
		this.idToQuestion = new HashMap<>();
		this.fm = new FileManager();
	}

	public void performAnalysis() throws UIMAException {
		this.processAnalyzables(getTrecQuestionsIterator(QUESTIONS_PATH));
		this.processAnalyzables(getTrecCandidatesIterator(CANDIDATES_PATH));
		this.populateIdToQuestionMap();
	}

	private Iterator<Analyzable> getTrecQuestionsIterator(String questionsPath) {
		return new TrecQuestionsReader(questionsPath).iterator();
	}

	private Iterator<Analyzable> getTrecCandidatesIterator(String candidatesPath) {
		return new TrecCandidatesReader(candidatesPath).iterator();
	}

	private void populateIdToQuestionMap() {
		Iterator<Analyzable> questions = getTrecQuestionsIterator(QUESTIONS_PATH);
		while (questions.hasNext()) {
			Analyzable question = questions.next();
			DataObject questionObject = new DataObject(Labelled.NEGATIVE_LABEL, question.getId(),
					new HashMap<String, Double>(), new HashMap<String, String>());
			this.idToQuestion.put(question.getId(), questionObject);
		}
	}

	private void processAnalyzables(Iterator<Analyzable> analyzables)
			throws UIMAException {
		JCas cas = JCasFactory.createJCas();
		while (analyzables.hasNext()) {
			this.ae.analyze(cas, analyzables.next());
		}
	}

	public void performDataGeneration(String parameterList)
			throws UIMAException {
		
		TreeSerializer ts = new TreeSerializer().enableRelationalTags();
		
		this.fm.create(TRAIN_SVM);

		JCas questionCas = JCasFactory.createJCas();
		JCas leftCandidateCas = JCasFactory.createJCas();
		JCas rightCandidateCas = JCasFactory.createJCas();

		Iterator<List<String>> chunks = this.getChunkReader(CANDIDATES_PATH).iterator();
		
		while (chunks.hasNext()) {
			List<String> chunk = chunks.next();
			
			if (chunk.isEmpty())
				continue;

			List<DataObject> candidateObjects = this.buildCandidatesObject(chunk);
			
			String questionId = candidateObjects.get(0).getMetadata().get(QUESTION_ID_KEY);
			
			System.out.println("Processing question: " + questionId);

			DataObject questionObject = this.idToQuestion.get(questionId);

			List<DataPair> pairs = pairQuestionWithCandidates(questionObject, candidateObjects);

			this.ae.analyze(questionCas, new SimpleContent(questionId, ""));
			
			List<Pair<DataPair, DataPair>> trainingPairs = Pairer.pair(pairs);

			for (Pair<DataPair, DataPair> trainingPair : trainingPairs) {
				DataPair leftPair = trainingPair.getA();
				DataPair rightPair = trainingPair.getB();

				DataObject lCandidate = leftPair.getB();
				DataObject rCandidate = rightPair.getB();

				TokenTree leftQuestionTree = RichTree.getPosChunkTree(questionCas);
				TokenTree rightQuestionTree = RichTree.getPosChunkTree(questionCas);

				this.ae.analyze(leftCandidateCas, new SimpleContent(lCandidate.getId(), ""));
				this.ae.analyze(rightCandidateCas, new SimpleContent(rCandidate.getId(), ""));

				TokenTree leftCandidateTree = RichTree.getPosChunkTree(leftCandidateCas);
				TokenTree rightCandidateTree = RichTree.getPosChunkTree(rightCandidateCas);
				
				MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(
						new MarkTwoAncestors());
				
				marker.markTrees(leftQuestionTree, leftCandidateTree, parameterList);
				marker.markTrees(rightQuestionTree, rightCandidateTree, parameterList);
				
				StringBuffer sb = new StringBuffer(1024 * 4);
				String label = leftPair.isPositive() ? "+1" : "-1";
				sb.append(label);
				sb.append(" |BT| ");
				sb.append(ts.serializeTree(leftQuestionTree, parameterList));
				sb.append(" |BT| ");
				sb.append(ts.serializeTree(leftCandidateTree, parameterList));
				sb.append(" |BT| ");
				sb.append(ts.serializeTree(rightQuestionTree, parameterList));
				sb.append(" |BT| ");
				sb.append(ts.serializeTree(rightCandidateTree, parameterList));
				sb.append(" |ET| ");

				this.fm.writeLn(TRAIN_SVM, sb.toString());
			}
		}
		
		this.fm.close(TRAIN_SVM);
	}
	
	private List<DataObject> buildCandidatesObject(List<String> chunk) {
		List<DataObject> candidateObjects = new ArrayList<>();

		for (String line : chunk) {
			List<String> fields = Lists.newArrayList(Splitter.on(" ").limit(6).split(line));
			String questionId = fields.get(0);
			String candidateId = fields.get(1);
			boolean relevant = fields.get(4).equals("true") ? true : false;

			Map<String, Double> features = new HashMap<>();
			
			Map<String, String> metadata = new HashMap<>();
			metadata.put(QUESTION_ID_KEY, questionId);
			
			DataObject candidateObject = new DataObject(
					relevant == true ? Labelled.POSITIVE_LABEL : Labelled.NEGATIVE_LABEL,
					candidateId, features, metadata);

			candidateObjects.add(candidateObject);
		}
		
		return candidateObjects;
	}
	
	private List<DataPair> pairQuestionWithCandidates(DataObject question,
			List<DataObject> candidates) {
		List<DataPair> pairs = new ArrayList<>();

		for (DataObject candidate : candidates) {
			pairs.add(new DataPair(candidate.getLabel(),
					question.getId() + "-" + candidate.getId(),
					new HashMap<String, Double>(),  new HashMap<String, String>(),
					question, candidate));
		}
		
		return pairs;
	}

	public ChunkReader getChunkReader(String candidatePath) {
		ChunkReader cr = new ChunkReader(candidatePath,
				new InStringOutString() {
					@Override
					public String apply(String str) {
						return str.substring(0, str.indexOf(" "));
					}
				});

		return cr;
	}

	private Analyzer instantiateAnalyzer() throws UIMAException {
		Analyzer ae = new Analyzer(new UIMAFilePersistence(CASES_DIRECTORY));

		ae.addAEDesc(createEngineDescription(BreakIteratorSegmenter.class))
				.addAEDesc(createEngineDescription(TreeTaggerPosLemmaTT4J.class))
				.addAEDesc(createEngineDescription(TreeTaggerChunkerTT4J.class));

		return ae;
	}

	public static void main(String[] args) throws UIMAException {

		String parameterList = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA,
						RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });

		TrecPipeline pipeline = new TrecPipeline();
		pipeline.performAnalysis();
		pipeline.performDataGeneration(parameterList);

	}
}
