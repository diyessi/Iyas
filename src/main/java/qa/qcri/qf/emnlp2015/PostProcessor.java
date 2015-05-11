package qa.qcri.qf.emnlp2015;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class PostProcessor {
	
	private Analyzer analyzer;
	
	private JCas cas;
	
	private TreeSerializer ts;
	
	public PostProcessor() throws UIMAException {
		this.analyzer = this.getAnalyzer();
		this.cas = JCasFactory.createJCas();
		this.ts = new TreeSerializer().useRoundBrackets();
	}
	
	public void produceTrees(String mode, String dataDir, String dataFile, String targetFile) {
		
		Map<String, Map<String, String>> idToTargets = getTargets(targetFile);
		
		FileManager fm = new FileManager();
			
		ReadFile in = new ReadFile(dataFile);
		in.nextLine(); // Skip header
		while(in.hasNextLine()) {
			String line = in.nextLine().trim();
			ArrayList<String> fields = Lists.newArrayList(Splitter.on(",").limit(3).split(line));
			String userId = fields.get(0);
			String date = fields.get(1);
			String status = fields.get(2).trim();
			
			while(status.endsWith("/")) {
				if(in.hasNextLine()) {
					String additionalLine = in.nextLine().trim();
					status = status + " " + additionalLine;
				}
			}
			
			userId = this.chompEscapes(userId);
			status = this.chompEscapes(status);
			status = this.replaceEscapeDelimiter(status);
			
			/**
			 * Analyze status
			 */
			
			analyzer.analyze(cas, new SimpleContent("dummy-id", status));
			
			TokenTree posChunkTree = RichTree.getPosChunkTree(cas);
			String serializedTree = ts.serializeTree(posChunkTree, RichNode.OUTPUT_PAR_LEMMA);
			
			/**
			 * Produce and write tree
			 */
			
			if(idToTargets.containsKey(userId)) {
				Map<String, String> targets = idToTargets.get(userId);
				for(String target : targets.keySet()) {
					fm.writeLn(dataDir + "/" + target + "." + mode, targets.get(target) + " |BT| " + serializedTree + " |ET|");
				}
			}	
		}
		
		fm.closeFiles();
	}
	
	public static Map<String, Map<String, String>> getTargets(String targetsPath) {
		Map<String, Map<String, String>> idToTargets = new HashMap<>();
		
		ReadFile in = new ReadFile(targetsPath);
		in.nextLine(); // Skip header
		while(in.hasNextLine()) {
			String line = in.nextLine().trim().replaceAll("\"", "");
			String[] fields = line.split(",");
			String userId = fields[0];
			
			if(!idToTargets.containsKey(userId)) {
				Map<String, String> labelToValue = new HashMap<>();
				labelToValue.put("ope", fields[1]);
				labelToValue.put("con", fields[2]);
				labelToValue.put("ext", fields[3]);
				labelToValue.put("agr", fields[4]);
				labelToValue.put("neu", fields[5]);
				idToTargets.put(userId, labelToValue);
			}
		}
		
		return idToTargets;
	}
	
	private Analyzer getAnalyzer() throws UIMAException {
		AnalysisEngine stanfordSegmenter = AnalysisEngineFactory
				.createEngine(createEngineDescription(StanfordSegmenter.class));

		AnalysisEngine stanfordPosTagger = AnalysisEngineFactory
				.createEngine(createEngineDescription(OpenNlpPosTagger.class,
						 StanfordPosTagger.PARAM_LANGUAGE, "en"));

		AnalysisEngine stanfordLemmatizer = AnalysisEngineFactory
				.createEngine(createEngineDescription(StanfordLemmatizer.class));

		AnalysisEngine illinoisChunker = AnalysisEngineFactory
				.createEngine(createEngineDescription(IllinoisChunker.class));
		
		Analyzer analyzer = new Analyzer(new UIMANoPersistence());
		
		analyzer.addAE(stanfordSegmenter)
			.addAE(stanfordPosTagger)
			.addAE(stanfordLemmatizer)
			.addAE(illinoisChunker);
		
		return analyzer;
	}
	
	private String chompEscapes(String s) {
		return s.substring(1, s.length() - 1);
	}
	
	private String replaceEscapeDelimiter(String s) {
		return s.replaceAll("/\"", "\"");
	}
	
	public static void main(String[] args) throws UIMAException {	
		PostProcessor pp = new PostProcessor();
		pp.produceTrees("train", LocalConfig.TRAIN_DIR,
				LocalConfig.TRAIN_SMALL, LocalConfig.TRAIN_SMALL_TARGETS);
		pp.produceTrees("dev", LocalConfig.DEV_DIR,
				LocalConfig.DEV_SMALL, LocalConfig.DEV_SMALL_TARGETS);
	}
}
