package qa.qcri.qf.treemarker;

import qa.qcri.qf.trees.RichNode;

/**
 * 
 * Utility class for marking nodes
 */
public class Marker {

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
}
