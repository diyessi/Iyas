package qa.qcri.qf.treemarker;

import java.util.List;

import qa.qcri.qf.trees.RichNode;

public interface MarkingStrategy {
	
	List<RichNode> getNodesToMark(RichNode node);
}
