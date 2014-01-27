package qa.qcri.qf.trees;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

public class BaseRichNode implements RichNode {
	
	protected Map<String, String> metadata;
	
	protected Set<String> additionalLabels;

	protected List<RichNode> children;
	
	protected RichNode parent;
	
	protected String value;
	
	public BaseRichNode() {
		this.metadata = new HashMap<>();
		this.additionalLabels = new HashSet<>();
		this.children = new ArrayList<>();
		this.parent = null;
		this.value = "NOT_INITIALIZED";
	}
	
	@Override
	public Map<String, String> getMetadata() {
		return this.metadata;
	}
	
	@Override
	public List<String> getAdditionalLabels() {
		List<String> labels = Lists.newArrayList(this.additionalLabels);
		Collections.sort(labels);
		return labels;
	}
	
	@Override
	public void addAdditionalLabel(String label) {
		this.additionalLabels.add(label);
	}
	
	@Override
	public List<RichNode> getChildren() {
		return this.children;
	}
	
	@Override
	public void addChild(RichNode node) {
		node.setParent(this);
		this.children.add(node);
	}
	
	@Override
	public RichNode getParent() {
		return this.parent;
	}

	@Override
	public void setParent(RichNode node) {
		this.parent = node;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean isLeaf() {
		return this.children.isEmpty();
	}

	@Override
	public String getRepresentation(String parameterList) {
		return this.value;
	}

}
