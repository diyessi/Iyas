package qa.qcri.qf.pipeline.qademo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.classifiers.Classifier;
import qa.qcri.qf.features.FeaturesUtil;
import qa.qcri.qf.features.PairFeatureFactory;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.trec.AnalyzerFactory;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.treemarker.Marker;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.providers.PosChunkTreeProvider;
import qa.qcri.qf.trees.providers.TokenTreeProvider;
import qa.qcri.qf.type.QuestionClass;
import svmlighttk.SVMLightTK;
import util.Pair;
import util.PairCompareOnB;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;

import com.google.common.base.Joiner;

public class InteractivePipelineEn {
	
	public static final String INDEX = IndexWikiDump.INDEX_PATH;

	private static final int NUMBER_OF_CANDIDATES = 10;
	
	private static final String STOPWORDS_EN_PATH = "resources/stoplist-en.txt";

	/**
	 * Build your model using:
	 * - the TrecPipelineRunner program to generate training data from TREC
	 * - svm_learn -t 5 -F 3 -C + -W R -V R -S 0 -N 1 svm.train svm.model
	 *   to produce the rereanking model from TREC training data
	 */
	private static final String MODEL_FILE = "data/trec-en/train/svm.model";
	
	private StandardAnalyzer luceneAnalyzer;
	
	private Analyzer analyzer;
	
	private PairFeatureFactory pf;
	
	private Classifier reranker;
	
	public InteractivePipelineEn() throws UIMAException {
		this.luceneAnalyzer = new StandardAnalyzer(Version.LUCENE_47);
		this.analyzer = AnalyzerFactory.newTrecPipeline("en", new UIMANoPersistence());
		this.pf = new PairFeatureFactory(new Alphabet());
		this.reranker = new SVMLightTK(MODEL_FILE);
	}

	/**
	 * Send the question to the search engine and reranker and output the
	 * results
	 * 
	 * @param question the query question
	 * @throws UIMAException
	 */
	private void processQuestion(String question) throws UIMAException {
		try {
			List<String> resultSet = retrievePassages(question);	
			List<Pair<String, Double>> rerankedPassages = rerankPassages(question, resultSet);
			
			for(Pair<String, Double> scoredPassage : rerankedPassages) {
				System.out.println(scoredPassage.getB() + ")\t" + scoredPassage.getA() + "\n");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Analyze a question and a list of associated passages and rerank them
	 * using the semantic linking model
	 * 
	 * @param question
	 *            the question to answer
	 * @param resultSet
	 *            the associated passages retrieved from search engine
	 * @return a list of ordered passages along with their reranking score
	 * @throws UIMAException
	 * @throws IOException
	 */
	private List<Pair<String, Double>> rerankPassages(String question,
			List<String> resultSet) throws UIMAException, IOException {
		String parameterList = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA,
						RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });

		MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(
				new MarkTwoAncestors()).useStopwords(STOPWORDS_EN_PATH);

		TokenTreeProvider tokenTreeProvider = new PosChunkTreeProvider();

		TreeSerializer ts = new TreeSerializer();

		List<Double> scores = new ArrayList<>();
		
		JCas questionCas = JCasFactory.createJCas();
		
		this.analyzer.analyze(questionCas, new SimpleContent("question",
				question), AnalyzerFactory.QUESTION_ANALYSIS);
		
		JCas passageCas = JCasFactory.createJCas();

		for (String passage : resultSet) {
			this.analyzer.analyze(passageCas, new SimpleContent("passage",
					passage));

			TokenTree questionTree = tokenTreeProvider.getTree(questionCas);
			TokenTree passageTree = tokenTreeProvider.getTree(passageCas);

			QuestionClass questionClass = JCasUtil.selectSingle(questionCas,
					QuestionClass.class);

			marker.markTrees(questionTree, passageTree, parameterList);

			Marker.markFocus(questionCas, questionTree, questionClass);

			if (questionClass != null) {
				Marker.markNamedEntityRelatedToQuestionClass(passageCas,
						passageTree, questionClass);
			}

			FeatureVector fv = this.pf.getPairFeatures(questionCas, passageCas,
					parameterList);

			StringBuffer sb = new StringBuffer(1024 * 4);
			sb.append("|BT| ");
			sb.append(ts.serializeTree(questionTree, parameterList));
			sb.append(" |BT| ");
			sb.append(ts.serializeTree(passageTree, parameterList));
			sb.append(" |BT| ");
			sb.append(" |BT| ");
			sb.append(" |ET| ");
			sb.append(FeaturesUtil.serialize(fv));
			sb.append(" |BV| ");
			sb.append(" |EV| ");

			String example = sb.toString();

			double score = reranker.classify(example);

			scores.add(score);
		}

		List<Pair<String, Double>> scoredPassages = new ArrayList<>();
		for (int i = 0; i < resultSet.size(); i++) {
			scoredPassages.add(new Pair<String, Double>(resultSet.get(i),
					scores.get(i)));
		}

		Collections.sort(scoredPassages, new PairCompareOnB<String, Double>());

		return scoredPassages;
	}

	/**
	 * Retrieves passages from a search index using a question as search query
	 * 
	 * @param question
	 *            the search query question
	 * @return a list of passages
	 * @throws IOException
	 * @throws ParseException
	 */
	private List<String> retrievePassages(String question) throws IOException, ParseException {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(INDEX)));
		IndexSearcher searcher = new IndexSearcher(reader);
		QueryParser parser = new QueryParser(Version.LUCENE_47, "text", this.luceneAnalyzer);
		
		String escapedQuery = QueryParser.escape(question);
		Query query = parser.parse(escapedQuery);
		
		TopDocs results = searcher.search(query, NUMBER_OF_CANDIDATES);
		ScoreDoc[] hits = results.scoreDocs;
		int numTotalHits = hits.length;
		
		List<String> resultSet = new ArrayList<>();
		
		for(int i = 0; i < numTotalHits; i++) {
			Document passage = searcher.doc(hits[i].doc);
			
			//String id = passage.get("id");
			String text = passage.get("text");
			
			resultSet.add(text);
		}
		
		return resultSet;
	}

	/**
	 * Take questions from standard input and process it until
	 * the user types "quit".
	 */
	private void inputLoop() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			try {
				System.out.print("Enter your question (or \"quit\"): ");
				String question = br.readLine();
			
				if(question.toLowerCase().equals("quit"))
					break;
				
				processQuestion(question);
				
		    } catch (IOException e) {
		    	System.out.println("[ERROR]: Error reading from STDIN");
		    } catch (UIMAException e) {
		    	System.out.println("[ERROR]: UIMA Exception");
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {	
		try {
			new InteractivePipelineEn().inputLoop();
		} catch (UIMAException e) {
			e.printStackTrace();
		}
	}
}
