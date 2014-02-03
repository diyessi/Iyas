package qa.qcri.qf.tools.questionclassifier;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.providers.ConstituencyTreeProvider;
import qa.qcri.qf.trees.providers.TokenTreeProvider;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class QuestionClassifierTrain {
	
	public class QuestionReader implements Iterable<CategoryContent> {

		private ReadFile in;
		
		private int counter;
		
		public QuestionReader(String path) {
			this.in = new ReadFile(path);
			this.counter = 0;
		}
		
		@Override
		public Iterator<CategoryContent> iterator() {
			Iterator<CategoryContent> iterator = new Iterator<CategoryContent>() {

				@Override
				public boolean hasNext() {
					if (!in.hasNextLine()) {
						in.close();
						return false;
					}
					return in.hasNextLine();
				}

				@Override
				public CategoryContent next() {
					String line = in.nextLine().trim();
					
					List<String> fields = Lists.newArrayList(Splitter.on(" ").limit(2).split(line));
					
					String id = String.valueOf(counter);
					String category = fields.get(0).substring(0, fields.get(0).indexOf(":"));
					String content = fields.get(1);
					

					counter++;
					
					return new CategoryContent(id, content, category);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}			
			};
			
			return iterator;
		}
	}
	
	public static final String QF_DIRECTORY = "data/question-classifier/";
	
	public static final String CASES_DIRECTORY = QF_DIRECTORY + "CASes/";

	public static final String QUESTIONS_PATH = QF_DIRECTORY + "train_5500.label";
	
	public static final String TRAIN_DIRECTORY = QF_DIRECTORY + "train/";

	public static void main(String[] args) throws UIMAException {
		QuestionClassifierTrain qcTrain = new QuestionClassifierTrain();
		
		Analyzer ae = instantiateAnalyzer(new UIMAFilePersistence(
				CASES_DIRECTORY));
		
		String parameterList = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA,
						RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
		
		JCas cas = JCasFactory.createJCas();
		
		Set<String> categories = new HashSet<>();
		
		Iterator<CategoryContent> questions = qcTrain.new QuestionReader(QUESTIONS_PATH).iterator();
		while(questions.hasNext()) {
			CategoryContent question = questions.next();
			categories.add(question.getCategory());
			
			ae.analyze(cas, question);
		}
		
		FileManager fm = new FileManager();
		
		TokenTreeProvider treeProvider = new ConstituencyTreeProvider();
		
		TreeSerializer ts = new TreeSerializer();
		
		questions = qcTrain.new QuestionReader(QUESTIONS_PATH).iterator();
		while(questions.hasNext()) {
			CategoryContent question = questions.next();		
			ae.analyze(cas, question);			
			
			for(String category : categories) {
				String label = "-1";
				if(category.equals(question.getCategory())) {
					label = "+1";
				}
				
				String outputFile = TRAIN_DIRECTORY + category + ".train";
				String example = label + " |BT| "
						+ ts.serializeTree(treeProvider.getTree(cas), parameterList)
						+ " |ET|";
				
				fm.writeLn(outputFile, example);
			}
		}
		
		fm.closeFiles();
	}
	
	private static Analyzer instantiateAnalyzer(UIMAPersistence persistence)
			throws UIMAException {
		Analyzer ae = new Analyzer(persistence);

		ae.addAEDesc(createEngineDescription(StanfordSegmenter.class))
				.addAEDesc(createEngineDescription(StanfordPosTagger.class))
				.addAEDesc(createEngineDescription(StanfordParser.class));

		return ae;
	}
}
