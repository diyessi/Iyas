package qa.qcri.qf.discourse;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.annotators.WhitespaceTokenizer;
import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.fileutil.WriteFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.treemarker.MarkIfThisNodeHasRelChildren;
import qa.qcri.qf.treemarker.MarkParent;
import qa.qcri.qf.treemarker.MarkThisNode;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.treemarker.Marker;
import qa.qcri.qf.treemarker.MarkingStrategy;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.TreeUtil;
import qa.qcri.qf.trees.nodes.BaseRichNode;
import qa.qcri.qf.trees.nodes.RichChunkNode;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;

public class EMNLP {
	
	public final static String TRAIN_1 = "data/discomt/WMT_2011/train_massimo_clean.svm";
	public final static String TRAIN_2 = "data/discomt/WMT_2012/train_massimo.svm";
	public final static String TRAIN_3 = "data/discomt/WMT_2013/train_massimo.svm";
	
	private UIMAPersistence noPersistence = new UIMANoPersistence();
	
	private Analyzer analyzer;
	
	private JCas cas1, cas2, cas3;
	
	private MarkingStrategy markThisNode = new MarkThisNode();
	
	private MarkingStrategy markTwoAncestors = new MarkTwoAncestors();
	
	private MarkingStrategy markParent = new MarkParent();
	
	private MarkingStrategy markIfThisNodeHas075RelChildren = new MarkIfThisNodeHasRelChildren(0.75);
	
	private MarkingStrategy markIfThisNodeHasAllRelChildren = new MarkIfThisNodeHasRelChildren(1.00);
	
	public static void main(String[] args) throws UIMAException, IOException {
		
		UIMAFramework.getDefaultPerformanceTuningProperties()
			.setProperty(UIMAFramework.JCAS_CACHE_ENABLED, "false");
		
		EMNLP app = new EMNLP();
		
		//app.produceExamplesWithBVecMatching(TRAIN_1, TRAIN_1 + ".12.noprop.out");
		//app.produceExamplesWithBVecMatching(TRAIN_2, TRAIN_2 + ".12.noprop.out");
		app.produceExamples(TRAIN_1, TRAIN_1 + ".baseline-no-stopwords.out");
		app.produceExamples(TRAIN_2, TRAIN_2 + ".baseline-no-stopwords.out");
	}
	
	public EMNLP() throws UIMAException {
		this.noPersistence = new UIMANoPersistence();
		this.analyzer = setupAnalysisPipeline();
		this.cas1 = JCasFactory.createJCas();
		this.cas2 = JCasFactory.createJCas();
		this.cas3 = JCasFactory.createJCas();
	}
	
	private void produceExamples(String inFile, String outFile) throws UIMAException, IOException {
		ReadFile in = new ReadFile(inFile);
		WriteFile out = new WriteFile(outFile);
		ReadFile bVecIn = new ReadFile(inFile + ".pairs.bvec");
		
		TreeSerializer ts = new TreeSerializer().enableRelationalTags();
		
		MarkTreesOnRepresentation relMarker = new MarkTreesOnRepresentation(
				this.markParent).useStopwords("resources/stoplist-en.txt");
		
		String outputParams = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });
		
		int lineCounter = 1;
		
		while(in.hasNextLine()) {
			System.out.println("Processing line: " + lineCounter++);
			
			String example = in.nextLine().trim();
			
			String[] fields = example.split("\\|[BE][TV]\\|");
			
			String label = fields[0].trim();
			String candidate1 = fields[1].trim();
			String candidate2 = fields[2].trim();
			String question = fields[3].trim();
			String candidate1Fv = fields[4].trim();
			String candidate2Fv = fields[5].trim();
			
			/**
			 * Read the data for marking using binary vectors
			 */
			List<List<Boolean>> bVecs1 = getBVec(bVecIn.nextLine().trim());
			List<List<Boolean>> bVecs2 = getBVec(bVecIn.nextLine().trim());
			
			/**
			 * Produce the discourse tree
			 */
			TokenTree candidate1Tree = this.produceDiscourseTree(cas1, candidate1, "id2");
			TokenTree candidate2Tree = this.produceDiscourseTree(cas2, candidate2, "id3");
			TokenTree questionTree = this.produceDiscourseTree(cas3, question, "id3");
			
			markTreesWithBVecs(bVecs1, candidate1Tree, questionTree, this.markTwoAncestors);
			relMarker.markTrees(candidate1Tree, questionTree, outputParams);
			
			propagateToNGram(candidate1Tree, this.markIfThisNodeHas075RelChildren);
			propagateToNGram(questionTree, this.markIfThisNodeHas075RelChildren);
			
			propagateRelTags(candidate1Tree);
			propagateRelTags(questionTree);
			
			String candidate1TreeStr = ts.serializeTree(candidate1Tree, outputParams);
			String question1TreeStr = ts.serializeTree(questionTree, outputParams);
			
			Marker.removeRelationalTagFromTree(questionTree);
			
			relMarker.markTrees(candidate2Tree, questionTree, outputParams);
			
			propagateToNGram(candidate2Tree, this.markIfThisNodeHas075RelChildren);
			propagateToNGram(questionTree, this.markIfThisNodeHas075RelChildren);
			
			//propagateRelTags(candidate2Tree);
			//propagateRelTags(questionTree);
			
			String candidate2TreeStr = ts.serializeTree(candidate2Tree, outputParams);
			String question2TreeStr = ts.serializeTree(questionTree, outputParams);
			
			String outputExample = new StringBuffer(1024*10)
				.append(label)
				.append(" |BT| ")
				.append(candidate1TreeStr)
				.append(" |BT| ")
				.append(question1TreeStr)
				.append(" |BT| ")
				.append(candidate2TreeStr)
				.append(" |BT| ")
				.append(question2TreeStr)
				.append(" |ET| ")
				.append(candidate1Fv)
				.append(" |BV| ")
				.append(candidate2Fv)
				.append(" |EV|")
				.toString();
			
			out.writeLn(outputExample);
		}
		
		in.close();
		out.close();
		bVecIn.close();
	}
	
	/**
	 * Propagates the RELATIONAL tag from leaves to root if all the children
	 * of a node have the RELATIONAL tag 
	 * @param tree the tree on which apply the propagation
	 */
	private void propagateRelTags(TokenTree tree) {
		List<RichNode> nodes = TreeUtil.getNodesBFS(tree);
		Collections.reverse(nodes);
		for(RichNode node : nodes) {
			RichNode parent = node.getParent();
			if(parent != null) {
				boolean propagate = true;
				for(RichNode child : parent.getChildren()) {
					if(!child.getMetadata().containsKey(RichNode.REL_KEY)) {
						propagate = false;
						break;
					}
				}
				if(propagate) {
					Marker.addRelationalTag(parent, this.markThisNode);
				}
			}
		}
	}
	
	/**
	 * Propagates the RELATIONAL label to NGRAMs according to the specified
	 * strategy
	 * @param tree the tree on which apply propagation
	 * @param strategy the propagation strategy
	 */
	private void propagateToNGram(TokenTree tree, MarkingStrategy strategy) {
		for(RichNode ngram : TreeUtil.getNodesWithLabel(tree, "NGRAM")) {
			Marker.addRelationalTag(ngram, strategy);
		}
	}
	
	/**
	 * Builds trees with discourse and pos nodes
	 * @param cas the CAS used to build the tree
	 * @param tree the tree to parse
	 * @param id the id of the tree, used only for serialization
	 * @return the parsed tree
	 * @throws UIMAException
	 */
	private TokenTree removeSyntax(TokenTree discourseTree) throws UIMAException {
		List<RichNode> ngrams = TreeUtil.getNodesWithLabel(discourseTree, "NGRAM");
		for(RichNode ngram : ngrams) {
			List<RichNode> chunks = ngram.getChildren();
			ngram.getChildren().clear();
			for(RichNode chunk : chunks) {
				for(RichNode pos : chunk.getChildren()) {
					ngram.addChild(pos);
				}
			}
 		}
		return discourseTree;
	}
	
	/**
	 * Produces a discourse tree with only chunks and POS tags.
	 * The tree is built removing nodes from a complete discourse tree.
	 * All the chunks from NGRAM nodes are attached to the root node.
	 * @param cas the CAS used to build the tree
	 * @param tree the tree to parse
	 * @param id the id of the tree, used only for serialization
	 * @return the parsed tree
	 * @throws UIMAException
	 */
	private TokenTree produceDiscourseTreeWithOnlySyntax(TokenTree discourseTree) throws UIMAException {	
		List<RichNode> ngrams = TreeUtil.getNodesWithLabel(discourseTree, "NGRAM");		
		discourseTree.getChildren().clear();	
		
		for(RichNode ngram : ngrams) {
			for(RichNode chunks : ngram.getChildren()) {
				discourseTree.addChild(chunks);
			}
		}
		
		discourseTree.setValue("ROOT");
		
		return discourseTree;
	}
	
	/**
	 * Produces a discourse tree with discourse, chunks and POS nodes
	 * @param cas the Cas used to retrieve the chunks and POS annotations
	 * @param tree the tree to parse and convert
	 * @param id the id of the tree, used only for caching the analysis
	 * @return the parsed TokenTree
	 * @throws UIMAException
	 */
	private TokenTree produceDiscourseTree(JCas cas, String tree, String id) throws UIMAException {		
		
		/**
		 * Parses the discourse tree
		 */
		RichNode richTree = parseDiscourseTree(tree);
		
		List<RichNode> treeTokens = performAnalysis(cas, id, richTree);
		
		/**
		 * Creates RichTokenNode from tokens
		 */
		List<RichTokenNode> tokenNodes = produceRichTokenNodes(cas);
		
		/**
		 * Builds a mapping between keys and RichChunkNodes
		 * A node is identified by its begin and end offsets
		 */
		Map<String, RichChunkNode> indexesToChunk = new HashMap<>();
		for(Chunk chunk : JCasUtil.select(cas, Chunk.class)) {
			indexesToChunk.put(chunkKey(chunk),	new RichChunkNode(chunk));
		}
		
		/**
		 * Creates the TokenTree node we will return to the caller.
		 * The value and children of the root node are copied to
		 * this node
		 */
		TokenTree tokenTree = new TokenTree();
		tokenTree.setValue(richTree.getValue());
		for(RichNode node : richTree.getChildren()) {
			tokenTree.addChild(node);
		}
		
		Set<RichChunkNode> insertedChunks = new HashSet<>();
		
		RichNode currentNgramRef = null;
		RichChunkNode currentChunkNodeRef = null;
		String currentChunkKey = null;
		
		for(int i = 0; i < tokenNodes.size(); i++) {
			/**
			 * Retrieves nodes from the tree
			 */
			RichNode posToken = treeTokens.get(i);
			RichNode ngramNode = posToken.getParent();
			
			/**
			 * Retrieves the correspondent RichTokenNode and the
			 * Token annotation stored in it
			 */
			RichTokenNode tokenNode = tokenNodes.get(i);
			Token token = tokenNode.getToken();
			
			/**
			 * Changes the token node of the original tree into a postag
			 * and appends to it the correspondent RichTokenNode
			 */
			posToken.setValue(tokenNode.getToken().getPos().getPosValue());
			posToken.addChild(tokenNode);
			
			/**
			 * Adds the RichTokenNode to the TokenTree, which have to keep
			 * track of its token leaf nodes
			 */
			tokenTree.addToken(tokenNode);
			
			/***
			 * Retrieves the chunk that covers the current token
			 * The UimaFit method returns lists. The list will contain
			 * 0 or 1 chunks.
			 */
			List<Chunk> coveringChunks = JCasUtil.selectCovering(cas, Chunk.class,
					token.getBegin(),
					token.getEnd());
			
			/**
			 * If the list does not contain a chunk we put a new dummy chunk
			 * in it and then we register a new RichChunkNode in the <indexes, chunk> map
			 */
			if(coveringChunks.isEmpty()) {
				Chunk emptyChunk = new Chunk(cas);
				emptyChunk.setBegin(token.getBegin());
				emptyChunk.setEnd(token.getEnd());
				emptyChunk.setChunkValue("o");
				coveringChunks.add(emptyChunk);			
				indexesToChunk.put(chunkKey(emptyChunk), new RichChunkNode(emptyChunk));
			}
			
			/**
			 * Gets the key of the covering chunk
			 */
			String coveringChunkKey = chunkKey(coveringChunks.get(0));
			
			if(currentChunkNodeRef == null) {
				currentChunkNodeRef = new RichChunkNode(coveringChunks.get(0));
				currentNgramRef = ngramNode;
				currentChunkKey = coveringChunkKey;
			} else {
				if(currentChunkKey.equals(coveringChunkKey)) {
					if(currentNgramRef != ngramNode) {
						currentChunkNodeRef = new RichChunkNode(coveringChunks.get(0));
						currentNgramRef = ngramNode;
						currentChunkKey = coveringChunkKey;
					}
				} else {
					currentChunkNodeRef = new RichChunkNode(coveringChunks.get(0));
					currentNgramRef = ngramNode;
					currentChunkKey = coveringChunkKey;
				}
			}
			
			/**
			 * Checks if the RichChunk node is already linked to the tree
			 */
			if(insertedChunks.contains(currentChunkNodeRef)) {
				/**
				 * If the node is already there, we unlink the pos node
				 * from the tree, and add it to the chunk node
				 */
				currentNgramRef.getChildren().remove(posToken);
				currentChunkNodeRef.addChild(posToken);
			} else {
				/**
				 * If the node is not there, we add the chunk node to the
				 * tree and to the inserted chunk list, in addition to
				 * the operations made in the previous case
				 */
				currentNgramRef.getChildren().remove(posToken);
				currentChunkNodeRef.addChild(posToken);
				
				currentNgramRef.addChild(currentChunkNodeRef);	
				
				insertedChunks.add(currentChunkNodeRef);
			}
		}
		
		return tokenTree;
	}

	private List<RichNode> performAnalysis(JCas cas, String id,
			RichNode richTree) {
		/**
		 * Collects the tokens from the tree and recovers the text
		 */
		List<RichNode> treeTokens = collectTokenNodes(richTree);
		
		String text = TreeUtil.getValues(treeTokens);
		
		/**
		 * Runs the analysis pipeline (token, postags, chunks)
		 */
		Analyzable analyzable = new SimpleContent(id, text);
		this.analyzer.analyze(cas, analyzable);
		return treeTokens;
	}
	
	/**
	 * Produces a list of RichTokenNodes from Token annotations retrieved
	 * from a cas
	 * @param cas the CAS containing Token annotations
	 * @return the list of RichTokenNodes
	 */
	private List<RichTokenNode> produceRichTokenNodes(JCas cas) {
		List<RichTokenNode> richTokenNodes = new ArrayList<>();
		for(Token token : JCasUtil.select(cas, Token.class)) {
			RichTokenNode richTokenNode = new RichTokenNode(token);
			richTokenNodes.add(richTokenNode);
		}
		return richTokenNodes;
	}
	
	/**
	 * Produces a key from the offsets of a chunk
	 * @param chunk the chunk
	 * @return the key 
	 */
	private String chunkKey(Chunk chunk) {
		return chunk.getBegin() + "-" + chunk.getEnd();
	}
	
	/**
	 * Collects the tokens from a parsed discourse tree. The tokens
	 * are nodes which have a parent node containing the value "NGRAM"
	 * @param tree the tree to traverse
	 * @return the tokens of the tree
	 */
	private List<RichNode> collectTokenNodes(RichNode tree) {
		return TreeUtil.getNodesWithFilter(tree, new Function<RichNode, Boolean>() {
			
			@Override
			public Boolean apply(RichNode input) {
				/**
				 * If the node does not have a parent return false
				 */
				if(input.getParent() == null) {
					return false;
				}
				
				/**
				 * If the node has a parent with NGRAM as value it is a token
				 */
				if(input.getParent().getValue().equals("NGRAM")) {
					return true;
				} else {
					return false;
				}
			}
		});
	}
	
	/**
	 * Parses a discourse tree and converts it into the Iyas tree structure
	 * @param discourseTree the discourse tree
	 * @return a tree made of RichNodes
	 */
	private RichNode parseDiscourseTree(String discourseTree) {
		RichNode root = null;
		RichNode currentNode = null;

		for(char ch : discourseTree.trim().toCharArray()) {
			if(ch == '(') {
				if(currentNode == null) {
					root = new BaseRichNode();
					root.setValue("");
					currentNode = root;
				} else {
					RichNode child = new BaseRichNode();
					child.setValue("");
					child.setParent(currentNode);
					currentNode.addChild(child);
					currentNode = child;
				}
			} else if(ch == ')') {
				currentNode = currentNode.getParent();
			} else {
				String newValue = currentNode.getValue() + ch;
				currentNode.setValue(newValue);
			}
		}
		
		for(RichNode node : TreeUtil.getNonLeaves(root)) {
			node.setValue(node.getValue().trim());
		}
		
		return root;
	}
	
	/**
	 * Produces tokenized sentence pairs for being aligned with Meteor
	 * @param inFile the input file
	 * @param outFile the output file
	 * @throws UIMAException
	 */
	private void produceTokenizedSentencePairs(String inFile, String outFile) throws UIMAException {
		ReadFile in = new ReadFile(inFile);
		WriteFile out = new WriteFile(outFile);
		
		String outputParams = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_TOKEN });
		
		int lineCounter = 1;
		
		while(in.hasNextLine()) {
			System.out.println("Processing line: " + lineCounter++);
			
			String example = in.nextLine().trim();
			
			String[] fields = example.split("\\|[BE][TV]\\|");

			String candidate1 = fields[1].trim();
			String candidate2 = fields[2].trim();
			String question = fields[3].trim();
			
			TokenTree candidate1Tree = this.produceDiscourseTree(cas1, candidate1, "id2");
			TokenTree candidate2Tree = this.produceDiscourseTree(cas2, candidate2, "id3");
			TokenTree questionTree = this.produceDiscourseTree(cas3, question, "id3");
			
			String candidate1TreeStr = TreeUtil.joinLeaves(candidate1Tree, outputParams);
			String candidate2TreeStr = TreeUtil.joinLeaves(candidate2Tree, outputParams);
			String questionTreeStr = TreeUtil.joinLeaves(questionTree, outputParams);
			
			StringBuffer sb = new StringBuffer(1024 * 10)
				.append(candidate1TreeStr)
				.append("\t")
				.append(questionTreeStr)
				.append("\n")
				.append(candidate2TreeStr)
				.append("\t")
				.append(questionTreeStr)
				.append("\n");
			
			out.write(sb.toString());
		}
		
		in.close();
		out.close();
	}
	
	/**
	 * Setups the analysis pipeline
	 * @return the analyzer
	 * @throws UIMAException
	 */
	private Analyzer setupAnalysisPipeline() throws UIMAException {
		Analyzer ae = new Analyzer(this.noPersistence);
		
		AnalysisEngine whitespaceTokenizer = AnalysisEngineFactory.createEngine(
				createEngineDescription(WhitespaceTokenizer.class));
		
		AnalysisEngine openNLPPosTagger = AnalysisEngineFactory.createEngine(
				createEngineDescription(OpenNlpPosTagger.class));
		
		AnalysisEngine stanfordLemmatizer = AnalysisEngineFactory.createEngine(
				createEngineDescription(StanfordLemmatizer.class));
		
		AnalysisEngine illinoisChunker = AnalysisEngineFactory.createEngine(
				createEngineDescription(IllinoisChunker.class));
		
		ae.addAE(whitespaceTokenizer)
			.addAE(openNLPPosTagger)
			.addAE(stanfordLemmatizer)
			.addAE(illinoisChunker);
		
		return ae;
	}
	
	/*
	 * 
	 * Marking of the nodes with binary vector alignments
	 * 
	 */

	private void markTreesWithBVecs(List<List<Boolean>> bVecs,
			TokenTree tree1, TokenTree tree2, MarkingStrategy markingStrategy) {
		
		List<Boolean> bVec1 = bVecs.get(0);
		List<Boolean> bVec2 = bVecs.get(1);
		
		markTreeWithBVec(bVec1, tree1, markingStrategy);
		markTreeWithBVec(bVec2, tree2, markingStrategy);
	}

	private void markTreeWithBVec(List<Boolean> bVec, TokenTree tree,
			MarkingStrategy markingStrategy) {
		List<RichNode> leaves = TreeUtil.getLeaves(tree);
		assert(bVec.size() == leaves.size());
		
		for(int i = 0; i < bVec.size(); i++) {
			if(bVec.get(i)) {
				Marker.addRelationalTag(leaves.get(i), markingStrategy);
			}
		}
	}

	private List<List<Boolean>> getBVec(String bVec) {
		String[] bVecs = bVec.split("\t");
		String[] bVec1 = bVecs[0].split(" ");
		String[] bVec2 = bVecs[1].split(" ");
		List<List<Boolean>> bVectors = new ArrayList<>();
		bVectors.add(new ArrayList<Boolean>());
		bVectors.add(new ArrayList<Boolean>());
		for(String value : bVec1) {
			bVectors.get(0).add(
					value.equals("1") || value.equals("2"));
		}
		for(String value : bVec2) {
			bVectors.get(1).add(
					value.equals("1") || value.equals("2"));
		}
		return bVectors;
	}
}
