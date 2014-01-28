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
import qa.qcri.qf.datagen.Labelled;
import qa.qcri.qf.datagen.RerankingTrain;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.RichNode;
import util.ChunkReader;
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
					DataObject.newFeaturesMap(), DataObject.newMetadataMap());
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
		
		RerankingTrain dataGenerator = new RerankingTrain(this.fm, TRAIN_SVM, this.ae)
			.setParameterList(parameterList);
		
		Iterator<List<String>> chunks = this.getChunkReader(CANDIDATES_PATH).iterator();
		
		while (chunks.hasNext()) {
			List<String> chunk = chunks.next();
			
			if (chunk.isEmpty())
				continue;

			List<DataObject> candidateObjects = this.buildCandidatesObject(chunk);
			
			DataObject questionObject = this.idToQuestion.get(
					this.getQuestionIdFromCandidateObjects(candidateObjects));
			
			dataGenerator.generateData(questionObject, candidateObjects);
		}
		
		dataGenerator.close();
	}

	private String getQuestionIdFromCandidateObjects(
			List<DataObject> candidateObjects) {
		return candidateObjects.get(0).getMetadata().get(QUESTION_ID_KEY);
	}
	
	private List<DataObject> buildCandidatesObject(List<String> chunk) {
		List<DataObject> candidateObjects = new ArrayList<>();

		for (String line : chunk) {
			List<String> fields = Lists.newArrayList(Splitter.on(" ").limit(6).split(line));
			String questionId = fields.get(0);
			String candidateId = fields.get(1);
			boolean relevant = fields.get(4).equals("true") ? true : false;

			Map<String, Double> features = DataObject.newFeaturesMap();
			
			Map<String, String> metadata = DataObject.newMetadataMap();
			metadata.put(QUESTION_ID_KEY, questionId);
			
			DataObject candidateObject = new DataObject(
					relevant == true ? Labelled.POSITIVE_LABEL : Labelled.NEGATIVE_LABEL,
					candidateId, features, metadata);

			candidateObjects.add(candidateObject);
		}
		
		return candidateObjects;
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
