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

public class EmNLP {
	
	public final static String TRAIN_1 = "data/discomt/WMT_2011/train_massimo_clean.svm";
	public final static String TRAIN_2 = "data/discomt/WMT_2012/train_massimo.svm";
	public final static String TRAIN_3 = "data/discomt/WMT_2013/train_massimo.svm";
	
	public final static String EXAMPLE = "+1 |BT| (SPAN (NUC (ROOT))(REL (BACKGROUND))(SPAN0 (NUC (SATELLITE))(NGRAM (Finger)(into)(the)(wound)(\")(brings)(us)(into)(straits)(,)))(SPAN (NUC (NUCLEUS))(REL (ELABORATION))(SPAN0 (NUC (NUCLEUS))(NGRAM (she)(said)(sweetly)(Argentine)(writer)(to)(the)(audience)(,)))(SPAN (NUC (SATELLITE))(REL (ENABLEMENT))(SPAN (NUC (NUCLEUS))(REL (ELABORATION))(SPAN0 (NUC (NUCLEUS))(NGRAM (composed)(mostly)(of)(young)(people)))(SPAN0 (NUC (SATELLITE))(NGRAM (who)(gathered)(at)(the)(International)(Book)(Fair)(in)(Guadalajara)(,))))(SPAN (NUC (SATELLITE))(REL (ELABORATION))(SPAN0 (NUC (NUCLEUS))(NGRAM (to)(listen)(to)(this)(roundtable)(discussion)(,)))(SPAN0 (NUC (SATELLITE))(NGRAM (moderated)(by)(Marisol)(Schultz)(and)(participation)(Valenzuelove)(,)(Sergio)(Ramirez)(,)(Mayra)(Montero)(and)(Luis)(Garcia)(Montero)(.))))))) |BT| (SPAN (NUC (ROOT))(REL (SAME-UNIT))(SPAN (NUC (NUCLEUS))(REL (ELABORATION))(SPAN0 (NUC (NUCLEUS))(NGRAM (A)(finger)(in)(the)(wounds)(')(brings)(us)(into)(narrow)(')(,)))(SPAN (NUC (SATELLITE))(REL (ENABLEMENT))(SPAN0 (NUC (NUCLEUS))(NGRAM (she)(libezne)(Argentine)(writer)))(SPAN0 (NUC (SATELLITE))(NGRAM (to)(the)(audience)(,)))))(SPAN (NUC (NUCLEUS))(REL (CAUSE))(SPAN (NUC (NUCLEUS))(REL (ELABORATION))(SPAN0 (NUC (NUCLEUS))(NGRAM (slozenemu)(mainly)(young)(people)(,)))(SPAN0 (NUC (SATELLITE))(NGRAM (which)(had)(gathered)(on)(the)(international)(kniznim)(exhibition)(in)(guadalajare))))(SPAN0 (NUC (SATELLITE))(NGRAM (to)(vyslechlo)(this)(debate)(for)(a)(round)(table)(moderovanou)(marisol)(schultzovou)(and)(with)(the)(participation)(of)(the)(valenzuelove)(,)(Sergio)(ramireze)(,)(mayry)(monterove)(and)(Luis)(Garcia)(montera)(.))))) |BT| (SPAN (NUC (ROOT))(REL (SAME-UNIT))(SPAN (NUC (NUCLEUS))(REL (ELABORATION))(SPAN0 (NUC (NUCLEUS))(NGRAM (The)(finger)(on)(it)(...)))(SPAN0 (NUC (SATELLITE))(NGRAM (\")(it)(puts)(us)(in)(a)(difficult)(position)(,))))(SPAN (NUC (NUCLEUS))(REL (CAUSE))(SPAN0 (NUC (NUCLEUS))(NGRAM (\")(told)(sweetly)(straight)(out)(the)(Argentine)(writer)(to)(the)(public)(,)))(SPAN (NUC (SATELLITE))(REL (ELABORATION))(SPAN (NUC (NUCLEUS))(REL (ELABORATION))(SPAN0 (NUC (NUCLEUS))(NGRAM (mainly)(of)(young)(people)(,)))(SPAN (NUC (SATELLITE))(REL (SUMMARY))(SPAN0 (NUC (NUCLEUS))(NGRAM (who)(met)(in)(the)(International)(Fair)(of)(the)(Book)(of)(Guadalajara)))(SPAN0 (NUC (SATELLITE))(NGRAM (-LHB-)(FIL)(-RHB-)))))(SPAN (NUC (SATELLITE))(REL (ELABORATION))(SPAN0 (NUC (NUCLEUS))(NGRAM (to)(listen)(to)(this)(round)(table)))(SPAN (NUC (SATELLITE))(REL (JOINT))(SPAN0 (NUC (NUCLEUS))(NGRAM (moderated)(by)(Marisol)(Shultz)))(SPAN0 (NUC (NUCLEUS))(NGRAM (and)(participated)(Valenzuela)(,)(Sergio)(Ramirez)(,)(Mayra)(Montero)(and)(Luis)(Garcia)(Montero)(all)(participated)(.)))))))) |ET|1:0.21516137 2:6.62295438 3:-0.52112676 4:-0.56726961 5:0.22613000 6:0.25788099 7:0.34070765 8:0.26534038 9:0.28364214 10:0.44047619 11:0.40697674 12:0.19148936 13:0.47731959 14:0.22797927 15:0.08108108 16:0.54901961 17:0.00000000 18:0.00000000 19:0.639101481945827 20:0.59825942443051 21:0.866868803827 22:0.378969525426489 23:0.640864045794162 |BV| 1:0.05886860 2:3.28563530 3:-0.73239437 4:-0.99130513 5:0.12445000 6:0.13003754 7:0.18032722 8:0.13521396 9:0.13706620 10:0.49090909 11:0.38983051 12:0.03000000 13:0.30217186 14:0.01630435 15:0.03225806 16:0.27210884 17:0.08066667 18:0.29991981 19:0.632950217703388 20:0.499136156668428 21:0.718440989096458 22:0.324145969459682 23:0.479549435196936 |EV|";
	
	public final static String EXAMPLE_2 = "-1 |BT| (SPAN0 (NUC (ROOT))(NGRAM (There)(is)(benefit)(mulopwe))) |BT|  |BT| (SPAN0 (NUC (ROOT))(NGRAM (A)(magician)(is)(looked)(for))) |ET|1:0.19505632 2:0.71212713 3:-0.80000000 4:-1.00000000 5:0.20205000 6:0.04761905 7:0.04761905 8:0.04761905 9:0.04761905 10:0.00000000 11:0.00000000 12:0.00000000 13:0.25333333 14:0.00000000 15:0.00000000 16:0.00000000 17:0.24675325 18:0.29729730 19:1 20:0.169030850945703 21:0.585369407004964 22:0.187251471568285 23:0.336336396998156 |BV| 1:0.06220117 2:0.00000000 3:-1.00000000 4:-1.00000000 5:0.00000000 6:0.00000000 7:0.04453436 8:0.00000000 9:0.00000000 10:0.00000000 11:0.00000000 12:0.00000000 13:0.12087912 14:0.00000000 15:0.00000000 16:0.66666667 17:0.00000000 18:0.20754717 19:1 20:0 21:0.480384461415261 22:0.0345650564910142 23:0.187867287325545 |EV|";
	
	private UIMAPersistence noPersistence = new UIMANoPersistence();
	
	private Analyzer analyzer;
	
	private JCas cas1, cas2, cas3;
	
	private MarkingStrategy markThisNode = new MarkThisNode();
	
	private MarkingStrategy markTwoAncestors = new MarkTwoAncestors();
	
	private MarkingStrategy markParent = new MarkParent();
	
	private MarkingStrategy markIfThisNodeHasRelChildren = new MarkIfThisNodeHasRelChildren(0.75);
	
	public static void main(String[] args) throws UIMAException, IOException {
		
		UIMAFramework.getDefaultPerformanceTuningProperties()
			.setProperty(UIMAFramework.JCAS_CACHE_ENABLED, "false");
		
		EmNLP app = new EmNLP();
		
		//app.produceExamplesWithBVecMatching(TRAIN_1, TRAIN_1 + ".12.noprop.out");
		//app.produceExamplesWithBVecMatching(TRAIN_2, TRAIN_2 + ".12.noprop.out");
		app.produceExamples(TRAIN_1, TRAIN_1 + ".baseline-no-stopwords.out");
		app.produceExamples(TRAIN_2, TRAIN_2 + ".baseline-no-stopwords.out");
	}
	
	public EmNLP() throws UIMAException {
		this.noPersistence = new UIMANoPersistence();
		this.analyzer = setupAnalysisPipeline();
		this.cas1 = JCasFactory.createJCas();
		this.cas2 = JCasFactory.createJCas();
		this.cas3 = JCasFactory.createJCas();
	}
	
	private void testExample() throws UIMAException {
		
		String[] fields = EXAMPLE_2.split("\\|[BE][TV]\\|");
		
		TreeSerializer ts = new TreeSerializer().enableRelationalTags().useSquareBrackets();
		
		String candidate1 = fields[1].trim();
		String candidate2 = fields[2].trim();
		String question = fields[3].trim();

		TokenTree candidate1Tree = this.produceDiscourseTree(cas1, candidate1, "id2");
		TokenTree candidate2Tree = this.produceDiscourseTree(cas2, candidate2, "id3");
		TokenTree questionTree = this.produceDiscourseTree(cas3, question, "id3");
		
		MarkTreesOnRepresentation relMarker = new MarkTreesOnRepresentation(
				new MarkTwoAncestors());
		
		String lemma = Joiner.on(",").join(
				new String[] { RichNode.OUTPUT_PAR_LEMMA });
		
		relMarker.markTrees(candidate1Tree, questionTree, lemma);
		
		String candidate1TreeStr = ts.serializeTree(candidate1Tree);
		String question1TreeStr = ts.serializeTree(questionTree);
		
		Marker.removeRelationalTagFromTree(questionTree);
		
		relMarker.markTrees(candidate2Tree, questionTree, lemma);
		
		String candidate2TreeStr = ts.serializeTree(candidate2Tree);
		String question2TreeStr = ts.serializeTree(questionTree);
		
		System.out.println(ts.serializeTree(candidate1Tree));
		System.out.println(ts.serializeTree(candidate2Tree));
	}
	
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
			
			String candidate1TreeStr = joinLeaves(candidate1Tree, outputParams);
			String candidate2TreeStr = joinLeaves(candidate2Tree, outputParams);
			String questionTreeStr = joinLeaves(questionTree, outputParams);
			
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
	
	private void produceExamplesWithBVecMatching(String inFile, String outFile) throws UIMAException {
		ReadFile in = new ReadFile(inFile);
		WriteFile out = new WriteFile(outFile);	
		ReadFile bVecIn = new ReadFile(inFile + ".pairs.bvec");
		
		TreeSerializer ts = new TreeSerializer().enableRelationalTags();
		
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
			
			TokenTree candidate1Tree = this.produceDiscourseTree(cas1, candidate1, "id2");
			TokenTree candidate2Tree = this.produceDiscourseTree(cas2, candidate2, "id3");
			TokenTree questionTree = this.produceDiscourseTree(cas3, question, "id3");	
			
			/**
			 * Mark the nodes using binary vectors
			 */
			
			List<List<Boolean>> bVecs1 = getBVec(bVecIn.nextLine().trim());
			
			markTreesWithBVecs(bVecs1, candidate1Tree, questionTree);
			
			String candidate1TreeStr = ts.serializeTree(candidate1Tree, outputParams);
			String question1TreeStr = ts.serializeTree(questionTree, outputParams);
			
			Marker.removeRelationalTagFromTree(questionTree);
			
			List<List<Boolean>> bVecs2 = getBVec(bVecIn.nextLine().trim());
			
			markTreesWithBVecs(bVecs2, candidate2Tree, questionTree);
			
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
	
	private void markTreesWithBVecs(List<List<Boolean>> bVecs,
			TokenTree tree1, TokenTree tree2) {
		
		List<Boolean> bVec1 = bVecs.get(0);
		List<Boolean> bVec2 = bVecs.get(1);
		
		markTreeWithBVec(bVec1, tree1);
		markTreeWithBVec(bVec2, tree2);
	}

	private void markTreeWithBVec(List<Boolean> bVec, TokenTree tree) {
		List<RichNode> leaves = TreeUtil.getLeaves(tree);
		assert(bVec.size() == leaves.size());
		
		for(int i = 0; i < bVec.size(); i++) {
			if(bVec.get(i)) {
				//Marker.addRelationalTag(leaves.get(i), this.markThisNode);
				Marker.addRelationalTag(leaves.get(i), this.markTwoAncestors);
			}
		}
		
		//propagateRelTags(tree);
		//removeRelTagsFromTokens(tree);
	}

	private void removeRelTagsFromTokens(TokenTree tree) {
		for(RichTokenNode tokenNode : tree.getTokens()) {
			tokenNode.getMetadata().remove(RichNode.REL_KEY);
		}
	}

	private void propagateRelTags(TokenTree tree) {
		// Propagate relations
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
	
	private void produceExamples(String inFile, String outFile) throws UIMAException, IOException {
		ReadFile in = new ReadFile(inFile);
		WriteFile out = new WriteFile(outFile);
		
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
			
			TokenTree candidate1Tree = this.produceDiscourseTreeWithNoSyntax(cas1, candidate1, "id2");
			TokenTree candidate2Tree = this.produceDiscourseTreeWithNoSyntax(cas2, candidate2, "id3");
			TokenTree questionTree = this.produceDiscourseTreeWithNoSyntax(cas3, question, "id3");
			
			relMarker.markTrees(candidate1Tree, questionTree, outputParams);
			
			propagateToNGram(candidate1Tree, this.markIfThisNodeHasRelChildren);
			propagateToNGram(questionTree, this.markIfThisNodeHasRelChildren);
			
			//propagateRelTags(candidate1Tree);
			//propagateRelTags(questionTree);
			
			String candidate1TreeStr = ts.serializeTree(candidate1Tree, outputParams);
			String question1TreeStr = ts.serializeTree(questionTree, outputParams);
			
			Marker.removeRelationalTagFromTree(questionTree);
			
			relMarker.markTrees(candidate2Tree, questionTree, outputParams);
			
			propagateToNGram(candidate2Tree, this.markIfThisNodeHasRelChildren);
			propagateToNGram(questionTree, this.markIfThisNodeHasRelChildren);
			
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
	}
	
	private void propagateToNGram(TokenTree tree, MarkingStrategy strategy) {
		for(RichNode ngram : TreeUtil.getNodesWithLabel(tree, "NGRAM")) {
			Marker.addRelationalTag(ngram, strategy);
		}
	}
	
	private TokenTree produceDiscourseTreeWithNoSyntax(JCas cas, String tree, String id) throws UIMAException {
		
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
		 * Creates the TokenTree node we will return to the caller.
		 * The value and children of the root node are copied to
		 * this node
		 */
		TokenTree tokenTree = new TokenTree();
		tokenTree.setValue(richTree.getValue());
		for(RichNode node : richTree.getChildren()) {
			tokenTree.addChild(node);
		}
		
		for(int i = 0; i < tokenNodes.size(); i++) {
			/**
			 * Retrieves nodes from the tree
			 */
			RichNode dummyNode = treeTokens.get(i);
			
			/**
			 * Retrieves the correspondent RichTokenNode and the
			 * Token annotation stored in it
			 */
			RichTokenNode tokenNode = tokenNodes.get(i);
			
			/**
			 * Changes the token node of the original tree into a dummy node
			 * and adds as a child the correspondent RichTokenNode
			 */
			dummyNode.setValue(tokenNode.getToken().getPos().getPosValue()); //Let's put a postag
			dummyNode.addChild(tokenNode);
			
			/**
			 * Adds the RichTokenNode to the TokenTree, which have to keep
			 * track of its token leaf nodes
			 */
			tokenTree.addToken(tokenNode);	
		}
		
		return tokenTree;
	}
	
	private TokenTree produceDiscourseTreeWithOnlySyntax(JCas cas, String tree, String id) throws UIMAException {
		TokenTree tokenTree = produceDiscourseTree(cas, tree, id);
		
		List<RichNode> ngrams = TreeUtil.getNodesWithLabel(tokenTree, "NGRAM");
		
		tokenTree.getChildren().clear();
		
		for(RichNode ngram : ngrams) {
			for(RichNode chunks : ngram.getChildren()) {
				tokenTree.addChild(chunks);
			}
		}
		
		tokenTree.setValue("ROOT");
		
		return tokenTree;
	}
	
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
	
	private String joinLeaves(RichNode tree, String outputParams) {
		return TreeUtil.getText(TreeUtil.getLeaves(tree), outputParams);
	}
	
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
}
