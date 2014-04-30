package it.unitn.limosine.italian.syntax.constituency;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import edu.stanford.nlp.trees.Tree;

/**
 * A factory class for tree constituent objects. 
 * Use a stanford tree for getting constituents.
 *
 */
public class StanfordTreeConstituentsProvider {
	
	/**
	 * Builds constituents from a tree string representation.
	 * 
	 * @param jcas A jcas object
	 * @param treeStr A string holding the tree string representation
	 * @return the constituent for the specified tree string representation
	 */
	public static Constituent buildConstituents(JCas jcas, String treeStr, int tokenStart) {
		if (jcas == null)
			throw new NullPointerException("jcas is null");
		if (treeStr == null)
			throw new NullPointerException("treeStr is null");
		
		Tree tree = Tree.valueOf(treeStr);
		tree.setSpans();
		
		List<Token> tokens = new ArrayList<>(
				JCasUtil.select(jcas, Token.class));
		return buildConstituentsFromStanfordTree(jcas, tree, tokens, tokenStart);		
	}
	
	/**
	 * Builds constituents for the supplied Stanford tree object.
	 * 
	 * @param jcas A jcas object
	 * @param tree A Stanford tree object
	 * @param tokens A list of tokens
	 * @return The constituent for the specified tree
	 */
	private static Constituent buildConstituentsFromStanfordTree(JCas jcas, Tree tree, List<Token> tokens, int tokenStart) { 
		assert jcas != null;
		assert tree != null;
		assert tokens != null;
		
		int begin = tokens.get(tree.getSpan().get(0) + tokenStart).getBegin();
		int end = tokens.get(tree.getSpan().get(1) + tokenStart).getEnd();
		Constituent constituent = newConstituent(jcas, begin, end, tree.value());
			
		// JUST FOR DEBUG
		/*
		String doctxt = jcas.getDocumentText();
		System.out.println("type " + tree.value() +  ", " +
						   "span: " + tree.getSpan() + ", " +
						   "span(sentence alligned): (" + (tree.getSpan().get(0) + tokenStart) + ", " + (tree.getSpan().get(1) + tokenStart) + "), " +
						   "PPT: " + tree.isPrePreTerminal() + ", " + 
				           "begin: " + begin + ", " +
				           "end: " + end + ", " + 
				           "childrenNum: " + tree.getChildrenAsList().size() + ", " +
				           "children: " +
				           "text: " + doctxt.substring(begin, end) + " " + 
				           "children: ");
		*/           
		List<Tree> children = tree.getChildrenAsList();
		constituent.setChildren(new FSArray(jcas, children.size()));
		
		// JUST FOR DEBUG
		/*
		for (int i = 0; i < children.size(); i++) {
			Tree child = tree.getChild(i);
			
			int childBegin = tokens.get(child.getSpan().get(0) + tokenStart).getBegin();
			int childEnd = tokens.get(child.getSpan().get(1) + tokenStart).getEnd();
			
			String childText = doctxt.substring(childBegin, childEnd);
			
			System.out.println(childText + "/" + child.value() + " ");
		}
		System.out.println();
		*/
		
		for (int i = 0; i < children.size(); i++) {
			Tree child = tree.getChild(i);
			
			if (child.isPreTerminal()) {
				// Add token to children annotations
				int tokenNum = child.getSpan().get(0) + tokenStart;
				
				Token token = tokens.get(tokenNum);
			     // Replace tokens poss with the BerkeleyParser Poss
				/*
				 POS pos = token.getPos();
				 pos.setPosValue(child.value());
				*/
				constituent.setChildren(i, token);
			} else {

				// Add constituent to children annotations
				Constituent childConstituent = buildConstituentsFromStanfordTree(jcas, child, tokens, tokenStart);
				childConstituent.setParent(constituent);
				constituent.setChildren(i, childConstituent);
			}
		}
		
		return constituent;
	}
	
	/**
	 * Create a new constituent object.
	 * 
	 */
	private static Constituent newConstituent(JCas jcas, int begin, int end, String constType) { 
		return newConstituent(jcas, begin, end, constType, null);
	}
	
	/**
	 * Create a new constituent object.
	 */
	private static Constituent newConstituent(JCas jcas, int begin, int end, String constType, Constituent parent) {
		if (jcas == null)
			throw new NullPointerException("jcas is null");
		if (begin < 0) 
			throw new IllegalArgumentException("begin < 0: " + begin);
		if (end < 0) 
			throw new IllegalArgumentException("end < 0: " + end);
		/*
		if (begin > end)
			throw new IllegalArgumentException("begin > end: " + begin + " > " + end);
		*/
		
		Constituent constituent = new Constituent(jcas, begin, end);
		constituent.setParent(parent);
		constituent.setConstituentType(constType);
		constituent.addToIndexes();
		return constituent;
	}

}
