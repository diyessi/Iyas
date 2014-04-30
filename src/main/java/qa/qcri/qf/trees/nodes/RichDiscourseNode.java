package qa.qcri.qf.trees.nodes;

import qa.qcri.qf.type.Discourse;

/**
 * 
 * The RichDiscourseNode class wraps a Discourse object, a datatype from the Iyas
 * typesystem
 */
public class RichDiscourseNode extends BaseRichNode {
	
	private Discourse discourse;
	
	public RichDiscourseNode(Discourse discourse) {
		super();
		this.discourse = discourse;
		this.metadata.put(RichNode.TYPE_KEY, RichNode.TYPE_DISCOURSE_NODE);
		this.value = discourse.getValue();
	}
	
	/**
	 * 
	 * @return the Iyas Discourse object
	 */
	public Discourse getDiscourse() {
		return this.discourse;
	}
	
}
