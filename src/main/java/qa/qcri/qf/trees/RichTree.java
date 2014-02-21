package qa.qcri.qf.trees;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.trees.nodes.BaseRichNode;
import qa.qcri.qf.trees.nodes.RichChunkNode;
import qa.qcri.qf.trees.nodes.RichConstituentNode;
import qa.qcri.qf.trees.nodes.RichDependencyNode;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * 
 * RichTree is a factory class for creating rich trees from annotated CASes
 */
public class RichTree {

	public static final String ROOT_LABEL = "ROOT";
	public static final String SENTENCE_LABEL = "S";

	/**
	 * Builds a POS+CHUNK tree from an annotated CAS. The CAS must contain
	 * sentence boundaries, tokens, POStags and chunks A POS+CHUNK tree is a
	 * tree with two salient layer. The bottom layer is made by tokens with
	 * their POStags. These nodes are grouped by chunk nodes.
	 * 
	 * @param cas
	 *            the UIMA JCas
	 * @return the POS+CHUNK tree, as a TokenTree
	 * 
	 * @see TokenTree
	 */
	public static TokenTree getPosChunkTree(JCas cas) {
		TokenTree root = new TokenTree();
		root.setValue(ROOT_LABEL);

		for (Sentence sentence : JCasUtil.select(cas, Sentence.class)) {
			RichNode sentenceNode = new BaseRichNode().setValue(SENTENCE_LABEL);

			for (Chunk chunk : JCasUtil.selectCovered(cas, Chunk.class, sentence)) {
				RichNode chunkNode = new RichChunkNode(chunk);
				for (Token token : JCasUtil.selectCovered(cas, Token.class, chunk)) {
					RichNode posNode = new BaseRichNode().setValue(
							token.getPos().getPosValue());

					RichTokenNode tokenNode = new RichTokenNode(token);

					chunkNode.addChild(
							posNode.addChild(tokenNode));

					root.addToken(tokenNode);
				}
				sentenceNode.addChild(chunkNode);
			}

			root.addChild(sentenceNode);
		}

		return root;
	}

	/**
	 * Builds a Constituency tree from an annotated CAS. The CAS must contain
	 * sentence boundaries, tokens, POStags and constituents
	 * 
	 * @param cas
	 *            the UIMA JCas
	 * @return the Constituency tree, as a TokenTree
	 * 
	 * @see TokenTree
	 */
	public static TokenTree getConstituencyTree(JCas cas) {
		TokenTree root = new TokenTree();
		root.setValue(ROOT_LABEL);

		List<Constituent> roots = new ArrayList<>();

		for (Constituent constituent : JCasUtil.select(cas, Constituent.class)) {
			if (constituent.getConstituentType().equals(ROOT_LABEL)) {
				roots.add(constituent);
			}
		}

		for (Constituent node : roots) {
			RichNode subTree = getConstituencySubTree(node, root);
			// Step required to skip each sentence root node
			for (RichNode sentenceNode : subTree.getChildren()) {
				root.addChild(sentenceNode);
			}
		}

		return root;
	}

	/**
	 * Recursive method for producing constituency trees out of a node
	 * 
	 * @param subTreeRoot
	 *            the root node of the subtree
	 * @param root
	 *            the root of the tree
	 * @return a serialized constituency subtree
	 */
	private static RichNode getConstituencySubTree(Constituent subTreeRoot,
			TokenTree root) {
		RichNode subTree = new RichConstituentNode(subTreeRoot);

		Collection<Constituent> constituents = JCasUtil.select(
				subTreeRoot.getChildren(), Constituent.class);

		for (Constituent constituent : constituents) {
			subTree.addChild(getConstituencySubTree(constituent, root));
		}

		/**
		 * If there are no constituents we are working with a token node
		 */
		if (constituents.size() == 0) {
			Collection<Token> tokens = JCasUtil.select(
					subTreeRoot.getChildren(), Token.class);

			for (Token token : tokens) {
				RichNode posNode = new BaseRichNode().setValue(
						token.getPos().getPosValue());

				RichTokenNode tokenNode = new RichTokenNode(token);

				subTree.addChild(
						posNode.addChild(tokenNode)
					);

				root.addToken(tokenNode);
			}
		}

		return subTree;
	}
	
	public static TokenTree getDependencyTree(JCas cas) {
		if (cas == null) {
			throw new NullPointerException("cas is null");
		}

		TokenTree root = new TokenTree();
		root.setValue(ROOT_LABEL);

		Map<Token, RichTokenNode> nodeMap = new HashMap<>();

		Collection<Dependency> deps = JCasUtil.select(cas, Dependency.class);
		for (Dependency dep : deps) {
			RichTokenNode govNode = getOrAddIfNew(dep.getGovernor(), nodeMap);
			RichTokenNode depNode = getOrAddIfNew(dep.getDependent(), nodeMap);
			RichDependencyNode relNode = new RichDependencyNode(dep);

			relNode.addChild(depNode);
			govNode.addChild(relNode);	
		}

		// Sort token nodes
		List<RichTokenNode> tokenNodes = new ArrayList<>(nodeMap.values());
		Collections.sort(tokenNodes, new Comparator<RichTokenNode>() {
			@Override
			public int compare(RichTokenNode o1, RichTokenNode o2) {
				return o1.getToken().getBegin() - o2.getToken().getBegin();
			}
		});

		// Add token nodes
		for (RichTokenNode tokenNode : tokenNodes) {
			root.addToken(tokenNode);

			if (!tokenNode.hasParent()) {	
				// Insert a fake dependency node between the root and the first token node
				RichDependencyNode depNode = newRichDependencyNode(cas, tokenNode, "root");
				root.addChild(depNode);
				depNode.addChild(tokenNode);

				//root.addChild(tokenNode);
			}
		}
		return root;
	}

	private static RichTokenNode getOrAddIfNew(Token token, Map<Token, RichTokenNode> nodeMap) {
		assert token != null;
		assert nodeMap != null;

		RichTokenNode node = nodeMap.get(token);
		if (node != null) {
			return node;
		} else {
			node = new RichTokenNode(token);
			nodeMap.put(token, node);
		}
		return node;
	}

	private static RichDependencyNode newRichDependencyNode(JCas cas, RichTokenNode depNode, String dependencyType) {
		assert cas != null;
		assert depNode != null;
		assert dependencyType != null;

		Token dep = depNode.getToken();
		Dependency dependency = new Dependency(cas, dep.getBegin(), dep.getEnd());
		dependency.setDependencyType(dependencyType);

		RichDependencyNode relNode = new RichDependencyNode(dependency);
		return relNode;	
	}
}
