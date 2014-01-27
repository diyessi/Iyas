package qa.qcri.qf.trees;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

public class TreeSerializer {
	
	private static final String LABEL_SEPARATOR = "-";
	
	private String lbr = "(";
	private String rbr = ")";
	
	private boolean enableAdditionalLabels = false;
	private boolean enableRelationalTags = false;
	
	public TreeSerializer useSquareBrackets() {
		this.lbr = "[";
		this.rbr = "]";
		return this;
	}
	
	public TreeSerializer useRoundBrackets() {
		this.lbr = "(";
		this.rbr = ")";
		return this;
	}
	
	public TreeSerializer enableAdditionalLabels() {
		this.enableAdditionalLabels = true;
		return this;
	}
	
	public TreeSerializer disableAdditionalLabels() {
		this.enableAdditionalLabels = false;
		return this;
	}
	
	public TreeSerializer enableRelationalTags() {
		this.enableRelationalTags = true;
		return this;
	}
	
	public TreeSerializer disableRelationalTags() {
		this.enableRelationalTags = false;
		return this;
	}
	
	public String serializeTree(RichNode node, String parameterList) {
		List<String> leftParts = new ArrayList<>();
		List<String> rightParts = new ArrayList<>();
		
		leftParts.add(this.lbr);
		
		List<String> labels = new ArrayList<>();
		labels.add(node.getRepresentation(parameterList));
		
		if(this.enableAdditionalLabels) {
			labels.addAll(node.getAdditionalLabels());
		}
		
		if(this.enableRelationalTags) {
			if(node.getMetadata().containsKey(RichNode.REL_KEY)) {
				labels.add(node.getMetadata().get(RichNode.REL_KEY));
			}
		}
		
		leftParts.add(Joiner.on(LABEL_SEPARATOR).join(labels));
		
		if(!node.isLeaf()) {
			leftParts.add(" ");
		}
		rightParts.add(0, this.rbr);
		
		for(RichNode child : node.getChildren()) {
			leftParts.add(serializeTree(child, parameterList));
		}
		
		leftParts.addAll(rightParts);
		
		return Joiner.on("").join(leftParts);
	}
}
