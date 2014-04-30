package it.unitn.limosine.italian.syntax.constituency;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.uimafit.util.JCasUtil;

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
	public static Constituent buildConstituents(JCas jcas, String treeStr) {
		if (jcas == null)
			throw new NullPointerException("jcas is null");
		if (treeStr == null)
			throw new NullPointerException("treeStr is null");
		
		System.out.println("str(tree): " + treeStr);
		
		Tree tree = Tree.valueOf(treeStr);
		tree.setSpans();
		
		List<Token> tokens = new ArrayList<>(
				JCasUtil.select(jcas, Token.class));
		return buildConstituentsFromStanfordTree(jcas, tree, tokens);		
	}
	
	/**
	 * Builds constituents for the supplied Stanford tree object.
	 * 
	 * @param jcas A jcas object
	 * @param tree A Stanford tree object
	 * @param tokens A list of tokens
	 * @return The constituent for the specified tree
	 */
	private static Constituent buildConstituentsFromStanfordTree(JCas jcas, Tree tree, List<Token> tokens) { 
		assert jcas != null;
		assert tree != null;
		assert tokens != null;
		
		int begin = tokens.get(tree.getSpan().get(0)).getBegin();
		int end = tokens.get(tree.getSpan().get(1)).getEnd();
		Constituent constituent = newConstituent(jcas, begin, end, tree.value());
		/*
		System.out.println("type " + tree.value() +  ", " +
						   "span: " + tree.getSpan() + ", " +
						   "PPT: " + tree.isPrePreTerminal() + ", " + 
				           "begin: " + begin + ", " +
				           "end: " + end + ", " + 
				           "childrenNum: " + tree.getChildrenAsList().size());
		*/	           
		
		List<Tree> children = tree.getChildrenAsList();
		constituent.setChildren(new FSArray(jcas, children.size()));
		
		for (int i = 0; i < children.size(); i++) {
			Tree child = tree.getChild(i);			
			
			if (child.isPreTerminal()) {
				// Add token to children annotations
				int tokenNum = child.getSpan().get(0);
				Token token = tokens.get(tokenNum);
				constituent.setChildren(i, token);
			} else {
				// Add constituent to children annotations
				Constituent childConstituent = buildConstituentsFromStanfordTree(jcas, child, tokens);
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
