package qa.qcri.qf.trees;

import java.util.List;
import java.util.Map;

/**
 * Interface implemented by the nodes used in the trees
 * produced and managed by the tree framework
 */
public interface RichNode {
	
	/**
	 * Keys and values used for storing the type of
	 * the node as metadata
	 */
	public static final String TYPE_KEY = "type_key";
	public static final String TYPE_TOKEN_NODE = "type_token";
	public static final String TYPE_CHUNK_NODE = "type_chunk";
	public static final String TYPE_CONSTITUENT_NODE = "type_constituent";
	
	/**
	 * Key used as key and value to associate a relational
	 * label to a node. This is used when we have two trees
	 * and we want to state that the node matches some other
	 * nodes in the second tree.
	 */
	public static final String REL_KEY = "REL";
	
	/**
	 * Parameters for describing what output should be produced
	 * by nodes capable of understanding the meaning of these strings
	 */
	public static final String OUTPUT_PAR_TOKEN_LOWERCASE = "TOKEN_LOWERCASE";
	public static final String OUTPUT_PAR_TOKEN = "TOKEN";
	public static final String OUTPUT_PAR_LEMMA = "LEMMA";
	
	/**
	 * 
	 * @return the metadata structure
	 */
	public Map<String, String> getMetadata();
	
	/**
	 * 
	 * @return the additional labels associated with the node
	 */
	public List<String> getAdditionalLabels();
	
	/**
	 * Associate a label to the node
	 * @param label
	 */
	public void addAdditionalLabel(String label);
	
	/**
	 * 
	 * @return the children node of this node
	 */
	public List<RichNode> getChildren();
	
	/**
	 * Add a child to this node
	 * @param node
	 */
	public void addChild(RichNode node);
	
	/**
	 * 
	 * @return the parent of this node
	 */
	public RichNode getParent();
	
	/**
	 * Set the parent of this node
	 * @param node the parent node
	 */
	public void setParent(RichNode node);
	
	
	/**
	 * 
	 * @return the default value of the node
	 */
	public String getValue();
	
	/**
	 * Set the default value of the node 
	 * @param value
	 */
	public void setValue(String value);
	
	/**
	 * Check if the node is a leaf (does not have children)
	 * @return true if the node is a leaf, false otherwise
	 */
	public boolean isLeaf();
	
	/**
	 * Return a string representation of the node which may
	 * be affected by the provided parameter list.
	 * A node can parse this list and react according to it.
	 * In the default case the implementation should return
	 * the same value of getValue()
	 * @param parameterList parameter list (strings
	 * separated by comma)
	 * @return the node representation
	 */
	public String getRepresentation(String parameterList);
	
}
