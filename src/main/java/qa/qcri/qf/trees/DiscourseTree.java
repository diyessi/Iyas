package qa.qcri.qf.trees;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import qa.qcri.qf.trees.nodes.BaseRichNode;
import qa.qcri.qf.trees.nodes.RichChunkNode;
import qa.qcri.qf.trees.nodes.RichDiscourseNode;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import qa.qcri.qf.type.Discourse;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;

public class DiscourseTree {
	
	public static final String ROOT_LABEL = "ROOT";
	public static final String SENTENCE_LABEL = "S";
	
	public static TokenTree getDiscourseTree(JCas cas) {	
		TokenTree root = new TokenTree();
		root.setValue(ROOT_LABEL);
		
		List<Discourse> roots = new ArrayList<>();

		for (Discourse discourse : JCasUtil.select(cas, Discourse.class)) {
			if (discourse.getValue().endsWith("_ROOT")) { 
				roots.add(discourse);
			}
		}
		
		for(Discourse node : roots) {
			RichNode rootSubTree = getDiscourseSubTree(node, root);
			root.addChild(rootSubTree);
		}
		
		return root;
	}
	
	public static TokenTree getDiscourseTreeWithTokens(JCas cas) {
		TokenTree tree = getDiscourseTree(cas);
		
		List<RichNode> discourseLeaves = TreeUtil.getLeaves(tree);
		
		for(RichNode node : discourseLeaves) {
			RichDiscourseNode leaf = (RichDiscourseNode) node;
			Discourse discourse = leaf.getDiscourse();
			
			for(Token token : JCasUtil.selectCovered(Token.class, discourse)) {
				RichNode posNode = new BaseRichNode().setValue(token
						.getPos().getPosValue());

				RichTokenNode tokenNode = new RichTokenNode(token);

				node.addChild(posNode.addChild(tokenNode));

				tree.addToken(tokenNode);
			}
		}
		
		return tree;
	}
	
	public static TokenTree getDiscourseTreeWithTokensAndChunks(JCas cas) {
		TokenTree tree = getDiscourseTree(cas);
		
		List<RichNode> discourseLeaves = TreeUtil.getLeaves(tree);
		
		for(RichNode node : discourseLeaves) {
			RichDiscourseNode leaf = (RichDiscourseNode) node;
			Discourse discourse = leaf.getDiscourse();
			
			for(Chunk chunk : JCasUtil.selectCovered(Chunk.class, discourse)) {
				
				RichNode chunkNode = new RichChunkNode(chunk);
			
				for(Token token : JCasUtil.selectCovered(Token.class, chunk)) {
					RichNode posNode = new BaseRichNode().setValue(token
							.getPos().getPosValue());
	
					RichTokenNode tokenNode = new RichTokenNode(token);
	
					chunkNode.addChild(posNode.addChild(tokenNode));
	
					tree.addToken(tokenNode);
				}
				
				node.addChild(chunkNode);
			}
		}
		
		return tree;
	}

	private static RichNode getDiscourseSubTree(Discourse subTreeRoot, TokenTree root) {
		RichNode subTree = new RichDiscourseNode(subTreeRoot);
		
		Collection<Discourse> discourses = JCasUtil.select(
				subTreeRoot.getChildren(), Discourse.class);

		for (Discourse discourse : discourses) {
			subTree.addChild(getDiscourseSubTree(discourse, root));
		}
		
		return subTree;
	}
}
