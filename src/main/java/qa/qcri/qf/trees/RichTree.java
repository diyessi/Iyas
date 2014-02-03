package qa.qcri.qf.trees;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.trees.nodes.BaseRichNode;
import qa.qcri.qf.trees.nodes.RichChunkNode;
import qa.qcri.qf.trees.nodes.RichConstituentNode;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;

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
			RichNode sentenceNode = new BaseRichNode();
			sentenceNode.setValue(SENTENCE_LABEL);

			for (Chunk chunk : JCasUtil.selectCovered(cas, Chunk.class,
					sentence)) {
				RichNode chunkNode = new RichChunkNode(chunk);
				for (Token token : JCasUtil.selectCovered(cas, Token.class,
						chunk)) {
					RichNode posNode = new BaseRichNode();
					posNode.setValue(token.getPos().getPosValue());

					RichTokenNode tokenNode = new RichTokenNode(token);

					posNode.addChild(tokenNode);
					chunkNode.addChild(posNode);

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
			RichNode constituentNode = getConstituencySubTree(constituent, root);
			subTree.addChild(constituentNode);
		}

		/**
		 * If there are no constituents we are working with a token node
		 */
		if (constituents.size() == 0) {
			Collection<Token> tokens = JCasUtil.select(
					subTreeRoot.getChildren(), Token.class);

			for (Token token : tokens) {
				RichNode posNode = new BaseRichNode();
				posNode.setValue(token.getPos().getPosValue());

				RichTokenNode tokenNode = new RichTokenNode(token);

				posNode.addChild(tokenNode);
				subTree.addChild(posNode);

				root.addToken(tokenNode);
			}
		}

		return subTree;
	}
}
