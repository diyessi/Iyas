package qa.qcri.qf.treemarker;

import qa.qcri.qf.trees.RichNode;

public class Marker {
	
	public static void addRelationalTag(RichNode node, MarkingStrategy strategy) {
		for(RichNode nodeToMark : strategy.getNodesToMark(node)) {
			nodeToMark.getMetadata().put(RichNode.REL_KEY, RichNode.REL_KEY);
		}
	}
}
