package qa.qcri.qf.trees;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;

public class RichTree {
	
	public static TokenTree getPosChunkTree(JCas cas) {
		TokenTree root = new TokenTree();	
		root.setValue("ROOT");
		
		for (Sentence sentence : JCasUtil.select(cas, Sentence.class)) {
			RichNode sentenceNode = new BaseRichNode();
			sentenceNode.setValue("S");

			for(Chunk chunk : JCasUtil.selectCovered(cas, Chunk.class, sentence)) {
				RichNode chunkNode = new RichChunkNode(chunk);
				for(Token token : JCasUtil.selectCovered(cas, Token.class, chunk)) {
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
