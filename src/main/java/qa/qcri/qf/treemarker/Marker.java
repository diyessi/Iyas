package qa.qcri.qf.treemarker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.jcas.JCas;

import qa.qcri.qf.trees.TokenSelector;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import qa.qcri.qf.type.QuestionClass;
import qa.qcri.qf.type.QuestionFocus;
import util.Pair;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

/**
 * 
 * Utility class for marking nodes
 */
public class Marker {

	/**
	 * Label for used for marking focus node
	 */
	public static final String FOCUS_LABEL = "FOCUS";

	/**
	 * Add a relational tag to the node selected by the given marking strategy
	 * 
	 * @param node
	 *            the starting node
	 * @param strategy
	 *            the marking strategy
	 */
	public static void addRelationalTag(RichNode node, MarkingStrategy strategy) {
		for (RichNode nodeToMark : strategy.getNodesToMark(node)) {
			nodeToMark.getMetadata().put(RichNode.REL_KEY, RichNode.REL_KEY);
		}
	}

	/**
	 * Marks the named entities in a tree with their type
	 * 
	 * @param cas
	 *            the CAS from which the tree is extracted
	 * @param tree
	 *            the tree extracted from the CAS
	 * @param labelPrefix
	 *            the string to prepend to the label
	 */
	public static void markNamedEntities(JCas cas, TokenTree tree,
			String labelPrefix) {
		for (Pair<NamedEntity, List<RichTokenNode>> neAndToken : TokenSelector
				.selectTokenNodeCovered(cas, tree, NamedEntity.class)) {

			NamedEntity ne = neAndToken.getA();
			String namedEntityType = ne.getValue().toUpperCase();

			for (RichTokenNode tokenNode : neAndToken.getB()) {
				for (RichNode node : new MarkTwoAncestors()
						.getNodesToMark(tokenNode)) {
					String label = namedEntityType;
					if (!labelPrefix.isEmpty()) {
						label += labelPrefix + "-";
					}
					node.addAdditionalLabel(label);
				}
			}
		}
	}

	/**
	 * Marks the focus token (if present) of a question tree
	 * 
	 * @param questionCas
	 *            the CAS from which the question tree is extracted
	 * @param questionTree
	 *            the tree extracted from the CAS
	 */
	public static void markFocus(JCas questionCas, TokenTree questionTree) {
		markFocus(questionCas, questionTree, null);
	}

	/**
	 * Marks the focus token (if present) of a question with an additional tag
	 * capturing the class of the question
	 * 
	 * @param questionCas
	 *            the CAS from which the question tree is extracted
	 * @param questionTree
	 *            the tree extracted from the CAS
	 * @param questionClass
	 *            the QuestionClass annotation
	 */
	public static void markFocus(JCas questionCas, TokenTree questionTree,
			QuestionClass questionClass) {
		for (Pair<QuestionFocus, List<RichTokenNode>> qfAndToken : TokenSelector
				.selectTokenNodeCovered(questionCas, questionTree,
						QuestionFocus.class)) {

			for (RichTokenNode tokenNode : qfAndToken.getB()) {
				for (RichNode node : new MarkSecondParent()
						.getNodesToMark(tokenNode)) {
					if (questionClass == null) {
						node.addAdditionalLabel(Marker.FOCUS_LABEL);
					} else {
						String focusType = questionClass.getQuestionClass()
								.toUpperCase();
						node.addAdditionalLabel(Marker.FOCUS_LABEL + "-"
								+ focusType);
					}
				}
			}
		}
	}

	/**
	 * Marks the chunk containing named entities which are related to a specific
	 * question class, with an additional label composed by the FOCUS label and
	 * the question class.
	 * 
	 * @param cas
	 *            the CAS from which the tree is extracted
	 * @param tree
	 *            the tree extracted from the CAS
	 * @param questionClass
	 *            the question class used to retrieved the related named
	 *            entities
	 */
	public static void markNamedEntityRelatedToQuestionClass(JCas cas,
			TokenTree tree, QuestionClass questionClass) {

		Set<String> relatedNamedEntityTypes = getNamedEntityMappedToQuestionClass(questionClass);

		for (Pair<NamedEntity, List<RichTokenNode>> neAndToken : TokenSelector
				.selectTokenNodeCovered(cas, tree, NamedEntity.class)) {

			NamedEntity ne = neAndToken.getA();
			String namedEntityType = ne.getValue().toUpperCase();

			if (relatedNamedEntityTypes.contains(namedEntityType)) {
				for (RichTokenNode tokenNode : neAndToken.getB()) {
					for (RichNode node : new MarkSecondParent()
							.getNodesToMark(tokenNode)) {
						String label = questionClass.getQuestionClass();
						node.addAdditionalLabel(FOCUS_LABEL + "-" + label);
					}
				}
			}
		}
	}

	/**
	 * Returns the named entity types which are linked to a specific question
	 * class
	 * 
	 * @param questionClass
	 *            the question class
	 * @return the set of named entity types related to a question class
	 */
	public static Set<String> getNamedEntityMappedToQuestionClass(
			QuestionClass questionClass) {

		Set<String> mappedNamedEntityTypes = new HashSet<>();

		String questionClassValue = questionClass.getQuestionClass();

		switch (questionClassValue) {
		case "HUM":
			mappedNamedEntityTypes.add("PERSON");
			break;
		case "LOC":
			mappedNamedEntityTypes.add("LOCATION");
			break;
		case "NUM":
			mappedNamedEntityTypes.add("DATE");
			mappedNamedEntityTypes.add("TIME");
			mappedNamedEntityTypes.add("MONEY");
			mappedNamedEntityTypes.add("PERCENT");
			break;
		case "ENTY":
			mappedNamedEntityTypes.add("ORGANIZATION");
			mappedNamedEntityTypes.add("PERSON");
			break;
		}

		return mappedNamedEntityTypes;
	}
}
