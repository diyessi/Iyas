package qa.qcri.qf.pipeline.trec;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.datagen.DataObject;
import qa.qcri.qf.datagen.Labelled;
import qa.qcri.qf.datagen.rr.Reranking;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import util.ChunkReader;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class TrecPipeline {

	public static final String TRAIN_CASES_DIRECTORY = "CASes/trec/train/";
	public static final String TRAIN_QUESTIONS_PATH = "data/trec/questions-train.txt";
	public static final String TRAIN_CANDIDATES_PATH = "data/trec/candidates-train.txt";

	public static final String TEST_CASES_DIRECTORY = "CASes/trec/test/";
	public static final String TEST_QUESTIONS_PATH = "data/trec/questions-test.txt";
	public static final String TEST_CANDIDATES_PATH = "data/trec/candidates-test.txt";

	public static final String QUESTION_ID_KEY = "QUESTION_ID_KEY";
	public static final String SEARCH_ENGINE_POSITION_KEY = "SEARCH_ENGINE_POSITION_KEY";

	private Analyzer ae;

	private Map<String, DataObject> idToQuestion = new HashMap<>();

	private FileManager fm;

	private String questionPath;

	private String candidatesPath;
	
	private int candidatesToKeep = -1;

	public TrecPipeline(FileManager fm) throws UIMAException {
		this.fm = fm;
		this.idToQuestion = new HashMap<>();
	}

	public void performAnalysis(Analyzer ae, String questionPath,
			String candidatesPath) throws UIMAException {
		this.ae = ae;
		this.questionPath = questionPath;
		this.candidatesPath = candidatesPath;

		this.processAnalyzables(getTrecQuestionsIterator(questionPath));
		this.processAnalyzables(getTrecCandidatesIterator(candidatesPath));
		this.populateIdToQuestionMap();
	}

	public void performDataGeneration(Reranking dataGenerator)
			throws UIMAException {

		Iterator<List<String>> chunks = this
				.getChunkReader(this.candidatesPath).iterator();

		while (chunks.hasNext()) {
			List<String> chunk = chunks.next();

			if (chunk.isEmpty())
				continue;
				
			List<String> keptLines = new ArrayList<>();
			if(this.candidatesToKeep == -1 || this.candidatesToKeep > chunk.size()) {
				keptLines = chunk;
			} else {
				int upperBound = Math.min(chunk.size(), this.candidatesToKeep);
				for(int i = 0; i < upperBound; i++) {
					keptLines.add(chunk.get(i));
				}
			}

			List<DataObject> candidateObjects = this
					.buildCandidatesObject(keptLines);

			DataObject questionObject = this.idToQuestion.get(this
					.getQuestionIdFromCandidateObjects(candidateObjects));

			dataGenerator.generateData(questionObject, candidateObjects);
		}
	}

	public void closeFiles() {
		this.fm.closeFiles();
	}

	private ChunkReader getChunkReader(String candidatePath) {
		ChunkReader cr = new ChunkReader(candidatePath,
				new Function<String, String>() {
					@Override
					public String apply(String str) {
						return str.substring(0, str.indexOf(" "));
					}
				});

		return cr;
	}
	
	public void setCandidatesToKeep(int candidatesToKeep) {
		this.candidatesToKeep = candidatesToKeep;
	}

	public Analyzer instantiateAnalyzer(UIMAPersistence persistence)
			throws UIMAException {
		Analyzer ae = new Analyzer(persistence);

		ae.addAEDesc(createEngineDescription(StanfordSegmenter.class))
				.addAEDesc(createEngineDescription(StanfordPosTagger.class))
				.addAEDesc(createEngineDescription(StanfordLemmatizer.class))
				.addAEDesc(createEngineDescription(IllinoisChunker.class));

		return ae;
	}

	private Iterator<Analyzable> getTrecQuestionsIterator(String questionsPath) {
		return new TrecQuestionsReader(questionsPath).iterator();
	}

	private Iterator<Analyzable> getTrecCandidatesIterator(String candidatesPath) {
		return new TrecCandidatesReader(candidatesPath).iterator();
	}

	private void populateIdToQuestionMap() {
		Iterator<Analyzable> questions = getTrecQuestionsIterator(this.questionPath);
		while (questions.hasNext()) {
			Analyzable question = questions.next();
			DataObject questionObject = new DataObject(Labelled.NEGATIVE_LABEL,
					question.getId(), DataObject.newFeaturesMap(),
					DataObject.newMetadataMap());
			this.idToQuestion.put(question.getId(), questionObject);
		}
	}

	private void processAnalyzables(Iterator<Analyzable> analyzables)
			throws UIMAException {
		
		if(this.candidatesToKeep == -1) {	
			JCas cas = JCasFactory.createJCas();
			while (analyzables.hasNext()) {
				Analyzable analyzable = analyzables.next();
				this.ae.analyze(cas, analyzable);
			}
		} else {
			int counter = 0;
			JCas cas = JCasFactory.createJCas();
			while (analyzables.hasNext()) {
				Analyzable analyzable = analyzables.next();
				this.ae.analyze(cas, analyzable);
				counter++;
				
				if(counter >= this.candidatesToKeep) {
					break;
				}
			}
		}
	}

	private String getQuestionIdFromCandidateObjects(
			List<DataObject> candidateObjects) {
		return candidateObjects.get(0).getMetadata().get(QUESTION_ID_KEY);
	}

	private List<DataObject> buildCandidatesObject(List<String> chunk) {
		List<DataObject> candidateObjects = new ArrayList<>();

		for (String line : chunk) {
			List<String> fields = Lists.newArrayList(Splitter.on(" ").limit(6)
					.split(line));
			String questionId = fields.get(0);
			String candidateId = fields.get(1);
			String searchEnginePosition = fields.get(2);
			boolean relevant = fields.get(4).equals("true") ? true : false;

			Map<String, Double> features = DataObject.newFeaturesMap();

			Map<String, String> metadata = DataObject.newMetadataMap();
			metadata.put(QUESTION_ID_KEY, questionId);
			metadata.put(SEARCH_ENGINE_POSITION_KEY, searchEnginePosition);

			DataObject candidateObject = new DataObject(
					relevant == true ? Labelled.POSITIVE_LABEL
							: Labelled.NEGATIVE_LABEL, candidateId, features,
					metadata);

			candidateObjects.add(candidateObject);
		}

		return candidateObjects;
	}
}
