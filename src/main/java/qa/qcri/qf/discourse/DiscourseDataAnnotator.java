package qa.qcri.qf.discourse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.uimafit.util.JCasUtil;

import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.DiscourseTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.TreeUtil;
import qa.qcri.qf.trees.nodes.BaseRichNode;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import qa.qcri.qf.type.Discourse;
import util.Pair;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class DiscourseDataAnnotator {
	
	public static final String TREC_DATA = "data/trec-en/terrier.BM25b0.75_0";
	
	public static final String DISCOURSE_DATA = "data/discourse/QA_output_2nd_format.txt";
	
	public static final Pattern ENTITY_PATTERN = Pattern.compile("& ([A-Z]+) ;");
	
	private Map<String, String> idToDiscourse;
	
	public DiscourseDataAnnotator() {
		this.idToDiscourse = new HashMap<>();
		ReadFile discourseIn = new ReadFile(DISCOURSE_DATA);
		while(discourseIn.hasNextLine()) {
			List<String> discourseLine = Lists.newArrayList(Splitter.on(" ").limit(3)
					.split(discourseIn.nextLine().trim()));
			String id = discourseLine.get(1);
			String text = discourseLine.get(2);
			this.idToDiscourse.put(id, text);
		}
		discourseIn.close();
	}
	
	public void addDiscourseAnnotationToCas(JCas cas, String passageId) {
		assert this.idToDiscourse.containsKey(passageId);
		
		new UIMAFilePersistence("CASes/trec-en/").deserialize(cas, passageId);
		
		List<Token> tokens = Lists.newArrayList(JCasUtil.select(cas, Token.class));
		
		String discourseText = this.idToDiscourse.get(passageId);	
		TokenTree discourseTree = this.constructDiscourseTree(discourseText);

		List<RichNode> discourseLeaves =  TreeUtil.getLeaves(discourseTree);
		
		List<Integer> growingSizes = new ArrayList<>();
		int cumulativeSize = 0;
		for(int i = 0; i < tokens.size(); i++) {
			cumulativeSize += tokens.get(i).getCoveredText().length();
			growingSizes.add(cumulativeSize);
		}
		
		List<Integer> discourseSizes = new ArrayList<>();
		int discourseSize = 0;
		for(RichNode discourseLeaf : discourseLeaves) {
			discourseSize += discourseLeaf.getValue().length();
			discourseSizes.add(discourseSize);
		}

		List<Pair<Integer, Integer>> tokenIntervals = new ArrayList<>();
		int beginPos = 0;
		for(int leafSize : discourseSizes) {
			int endPos = growingSizes.indexOf(leafSize);		
			tokenIntervals.add(new Pair<>(beginPos, endPos));			
			beginPos = endPos + 1;			
		}
		
		for(int i = 0; i < tokenIntervals.size(); i++) {
			Pair<Integer, Integer> interval = tokenIntervals.get(i);
			for(int j = interval.getA(); j <= interval.getB(); j++) {
				RichNode discourseLeaf = discourseLeaves.get(i);
				RichNode parentDiscourseLeaf = discourseLeaf.getParent();
				parentDiscourseLeaf.getChildren().remove(discourseLeaf);
				Token token = tokens.get(j);
				RichTokenNode tokenNode = new RichTokenNode(token);
				tokenNode.setValue(token.getCoveredText());
				parentDiscourseLeaf.addChild(tokenNode);
				
				discourseTree.addToken(tokenNode);
			}
		}
		
		recursivelyExtractAnnotation(cas, discourseTree);
		
		List<Discourse> discourses = Lists.newArrayList(JCasUtil.select(cas, Discourse.class));
		
		Map<Discourse, List<Discourse>> nodeToChildren = new HashMap<>();
		Map<Integer, Discourse> beginIndexToNode = new HashMap<>();
		
		Discourse parentNode = null;
		int currentBegin = 0;
		for(Discourse node : discourses) {
			nodeToChildren.put(node, new ArrayList<Discourse>());
			if(parentNode == null) {
				parentNode = node;
			} else {
				if(node.getBegin() == currentBegin) {
					nodeToChildren.get(parentNode).add(node);
					currentBegin = node.getEnd() + 1;
					beginIndexToNode.put(node.getBegin(), node);
				} else {
					parentNode = beginIndexToNode.get(node.getBegin());
					nodeToChildren.get(parentNode).add(node);
					currentBegin = node.getEnd() + 1;
					beginIndexToNode.put(node.getBegin(), node);
				}
			}
		}
		
		for(Discourse node : nodeToChildren.keySet()) {
			List<Discourse> children = nodeToChildren.get(node);
			
			FSArray childrenFSArray = new FSArray(cas, children.size());
			for(int i = 0; i < children.size(); i++) {
				Discourse child = children.get(i);
				child.setParent((Annotation) node);
				childrenFSArray.set(i, (Annotation) child);
			}
			
			node.setChildren(childrenFSArray);
		}

	}
	
	public Pair<Integer, Integer> recursivelyExtractAnnotation(JCas cas, RichNode discourseNode) {
		if(discourseNode.isLeaf()) {
			Token tokenNode = ((RichTokenNode) discourseNode).getToken();
			return new Pair<Integer, Integer>(tokenNode.getBegin(), tokenNode.getEnd());
		} else {
			Integer begin = Integer.MAX_VALUE;
			Integer end = Integer.MIN_VALUE;
			
			for(RichNode child : discourseNode.getChildren()) {
				Pair<Integer, Integer> span = recursivelyExtractAnnotation(cas, child);
				
				if(span.getA() < begin) {
					begin = span.getA();
				}
				
				if(span.getB() > end) {
					end = span.getB();
				}
			}
			
			Discourse annotation = new Discourse(cas);
			annotation.setBegin(begin);
			annotation.setEnd(end);
			annotation.setValue(discourseNode.getValue());
			annotation.addToIndexes(cas);
			
			return new Pair<Integer, Integer>(begin, end);
		}
	
	}
	
	public static void main(String[] args) throws UIMAException {
		
		JCas cas = JCasFactory.createJCas();
		
		DiscourseDataAnnotator ddAnnotator = new DiscourseDataAnnotator();
		ddAnnotator.addDiscourseAnnotationToCas(cas, "NYT19990607.0273-24");
		
		for(Discourse discourse : JCasUtil.select(cas, Discourse.class)) {
			System.out.println(discourse.getValue() + " "
					+ discourse.getBegin() + " "
					+ discourse.getEnd());
			FSArray children = discourse.getChildren();
			if(children.size() == 0) {
				System.out.println("\tNo children");
			} else {
				for(int i = 0; i < children.size(); i++) {
					Discourse child = (Discourse) children.get(i);
					System.out.println("\t" + child.getValue() + " "
							+ child.getBegin() + " "
							+ child.getEnd());
				}
			}
		}
		
		TreeSerializer ts = new TreeSerializer().useSquareBrackets();

		TokenTree discourseTree = DiscourseTree.getDiscourseTreeWithTokensAndChunks(cas);
		
		System.out.println(ts.serializeTree(discourseTree));
	}
		
	public TokenTree constructDiscourseTree(String discourseTree) {
		return parseDiscourseTree(discourseTree);
	}
	
	private static TokenTree parseDiscourseTree(String discourseTree) {
		TokenTree root = null;
		RichNode currentNode = null;

		for(char ch : discourseTree.toCharArray()) {
			if(ch == '[') {
				if(currentNode == null) {
					root = new TokenTree();
					root.setValue("");
					currentNode = root;
				} else {
					RichNode child = new BaseRichNode();
					child.setValue("");
					child.setParent(currentNode);
					currentNode.addChild(child);
					currentNode = child;
				}
			} else if(ch == ']') {
				currentNode = currentNode.getParent();
			} else {
				String newValue = currentNode.getValue() + ch;
				currentNode.setValue(newValue);
			}
		}
		
		for(RichNode node : TreeUtil.getNonLeaves(root)) {
			node.setValue(node.getValue().trim());
		}
		
		for(RichNode node : TreeUtil.getLeaves(root)) {
			String value = node.getValue();
			node.setValue(value.substring(0, value.indexOf(" ")));
			
			String leafValue = value.substring(value.indexOf(" ")).replaceAll(" ", "");
			RichNode newLeaf = new BaseRichNode();
			newLeaf.setValue(leafValue);
			newLeaf.setParent(node);
			node.addChild(newLeaf);
		}
		
		return root;
	}
}
