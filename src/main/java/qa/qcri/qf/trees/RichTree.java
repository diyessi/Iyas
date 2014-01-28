package qa.qcri.qf.trees;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;

/**
 *
 * RichTree is a factory class for creating rich trees
 * from annotated CASes
 */
public class RichTree {

	public static final String ROOT_LABEL = "ROOT";
	public static final String SENTENCE_LABEL = "S";

	/**
	 * Builds a POS+CHUNK tree from an annotated CAS
	 * 
	 * The CAS must contain sentence boundaries, tokens,
	 * POStags and chunks
	 * 
	 * A POS+CHUNK tree is a tree with two salient layer.
	 * The bottom layer is made by tokens with their POStags.
	 * These nodes are grouped by chunk nodes.
	 * 
	 * @param cas the UIMA JCas
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

			for (Chunk chunk : JCasUtil.selectCovered(cas, Chunk.class, sentence)) {
				RichNode chunkNode = new RichChunkNode(chunk);
				for (Token token : JCasUtil.selectCovered(cas, Token.class, chunk)) {
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

}
