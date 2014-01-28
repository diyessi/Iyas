package qa.qcri.qf.treemarker;

import java.util.ArrayList;
import java.util.List;

import qa.qcri.qf.trees.RichNode;

/**
 * 
 * Marking strategy selecting the parent of the given node
 */
public class MarkParent implements MarkingStrategy {

	@Override
	public List<RichNode> getNodesToMark(RichNode node) {
		List<RichNode> nodes = new ArrayList<>();
		if (node.getParent() != null) {
			nodes.add(node.getParent());
		}
		return nodes;
	}

}
