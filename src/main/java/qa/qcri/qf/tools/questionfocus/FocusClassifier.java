package qa.qcri.qf.tools.questionfocus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.collect.Lists;

import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.fileutil.WriteFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class FocusClassifier {
	
	public static final String DATA = "data/question-focus/data_2k.txt";
	
	public static final String CASES_DIRECTORY = "CASes/question-focus/";
	
	public static Set<String> allowedTags
		= new HashSet<>(Arrays.asList(
				//"NNPS", "VBN", "JJ", "JJS", // These postags are seen just once as focus
				"NNS", "NNP",  "NN" // These are high frequency postags
				));
	
	private Analyzer ae;
	
	private JCas cas;
	
	private TreeSerializer ts;	
	
	private Map<Integer, List<String>> examples;
	
	/*
	 * Variables for statistics
	 */
	
	private Map<String, Integer> posToFreq = new HashMap<>();
	
	public FocusClassifier() throws UIMAException {
		this.ae = Commons.instantiateAnalyzer(new UIMAFilePersistence(
				CASES_DIRECTORY));
		this.cas = JCasFactory.createJCas();
		this.ts = new TreeSerializer().enableAdditionalLabels();
		this.examples = new HashMap<>();
	}
	
	public FocusClassifier generateExamples(String dataPath) {
		ReadFile in = new ReadFile(dataPath);
		
		int qid = 0;
		
		while(in.hasNextLine()) {
			String line = this.filterText(in.nextLine());
			
			/**
			 * Input check
			 */
			if(line.startsWith("IMPL")) continue;		
			if(StringUtils.countMatches(line, "#") != 1) continue;
			assert line.contains("#");
			
			int beginPos = line.indexOf("#");
			int endPos = line.indexOf(" ", beginPos) - 1;
			if(endPos < 0) {
				endPos = line.length() - 1;
			}
			
			String text = line.replaceAll("#", "");
			
			this.ae.analyze(this.cas, new SimpleContent("q-" + qid, text));
			
			TokenTree tree = RichTree.getConstituencyTree(this.cas);
			
			this.addFocusMetadata(tree, beginPos, endPos);
			
			List<String> questionExamples = produceExamples(qid, tree);
			
			this.examples.put(qid, questionExamples);
			
			qid++;
		}
		
		in.close();
		
		return this;
	}
	
	public List<String> produceExamples(int qid, TokenTree tree) {
		List<String> examples = new ArrayList<>();
		
		for(RichTokenNode node : tree.getTokens()) {
			
			RichNode posTag = node.getParent();
			
			if(!allowedTags.contains(posTag.getValue())) continue;
			
			posTag.addAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
			
			boolean isFocus = node.getMetadata().containsKey(Commons.QUESTION_FOCUS_KEY);
			
			String label = isFocus ? "+1" : "-1";
			
			String taggedTree = ts.serializeTree(tree, Commons.getParameterList());
			
			String example = label
					+ "|BT| "
					+ taggedTree
					+ " |ET|";
			
			examples.add(example);
			
			posTag.removeAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
			
			if(isFocus) {
				String pos = node.getParent().getValue();
				
				Integer freq = this.posToFreq.get(pos);
				
				if(freq == null) {
					this.posToFreq.put(pos, 1);
				} else {
					this.posToFreq.put(pos, freq + 1);
				}
			}
		}
		
		return examples;
	}
	
	public void addFocusMetadata(TokenTree tree, int beginPos, int endPos) {
		for(RichTokenNode node : tree.getTokens()) {
			Token token = node.getToken();
			if(token.getBegin() == beginPos && token.getEnd() == endPos) {			
				node.getMetadata().put(Commons.QUESTION_FOCUS_KEY, Commons.QUESTION_FOCUS_KEY);
			}
		}
	}

	public String filterText(String text) {
		String filteredText = text.trim();
		filteredText = filteredText.replaceAll(" +", " ");
		return filteredText;
	}

	public FocusClassifier printStatistics() {
		System.out.println("F-POS\tFREQUENCY");
		for(String pos : this.posToFreq.keySet()) {
			System.out.println(pos + "\t" + this.posToFreq.get(pos));
		}
		
		int totalNumberOfExamples = 0;
		for(List<String> exampleList : this.examples.values()) {
			totalNumberOfExamples += exampleList.size();
		}

		System.out.println("\nTotal number of examples: " + totalNumberOfExamples);
		
		return this;
	}
	
	public FocusClassifier writeExamplesToDisk(Map<Integer, List<String>> examples,
			String outputPath) {
		
		WriteFile out = new WriteFile(outputPath);
		
		for(Integer qid : examples.keySet()) {
			List<String> examplesList = examples.get(qid);
			
			for(String example : examplesList) {
				out.writeLn(example);
			}
		}
		
		out.close();
		
		return this;
	}
	
	public FocusClassifier writeExamplesToDisk(String outputPath) {
		return this.writeExamplesToDisk(this.examples, outputPath);
	}
	
	public FocusClassifier writeExamplesInFolds(String outputDir, int folds) {
		
		List<Integer> questionIds = Lists.newArrayList(this.examples.keySet());
		
		List<List<Integer>> foldsOfIds = Lists.partition(questionIds, questionIds.size() / folds);
		
		System.out.println("Total number of questions: " + questionIds.size());
		System.out.println("Number of folds: " + foldsOfIds.size());
		for(List<Integer> foldOfIds : foldsOfIds) {
			System.out.println("Total questions in single fold: " + foldOfIds.size());
		}
		
		for(int i = 0; i < folds; i++) {
			List<Integer> trainIds = new ArrayList<>();
			List<Integer> testIds = foldsOfIds.get(i);
			
			for(int j = 0; j < folds; j++) {			
				if(i != j) {
					trainIds.addAll(foldsOfIds.get(j));
				}
			}
			
			this.writeExamples(outputDir + "/" + "fold-" + i + "/svm.train", trainIds);
			this.writeExamples(outputDir + "/" + "fold-" + i + "/svm.test", testIds);	
		}
		
		return this;
	}
	
	public void writeExamples(String outputFile, List<Integer> qIds) {
		WriteFile out = new WriteFile(outputFile);
		for(Integer qid : qIds) {
			for(String example : this.examples.get(qid))
				out.writeLn(example);
		}
		out.close();
	}
	
	public static void main(String[] args) throws UIMAException {
		new FocusClassifier().generateExamples(DATA)
			.printStatistics()
			.writeExamplesInFolds("data/question-focus/folds", 5);
	}
	
}
