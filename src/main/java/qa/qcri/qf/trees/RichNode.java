package qa.qcri.qf.trees;

import java.util.List;
import java.util.Map;

public interface RichNode {
	
	public static final String TYPE_KEY = "type_key";
	public static final String TYPE_TOKEN_NODE = "type_token";
	public static final String TYPE_CHUNK_NODE = "type_chunk";
	public static final String TYPE_CONSTITUENT_NODE = "type_constituent";
	
	public static final String REL_KEY = "REL";
	
	public static final String OUTPUT_PAR_TOKEN_LOWERCASE = "TOKEN_LOWERCASE";
	public static final String OUTPUT_PAR_TOKEN = "TOKEN";
	public static final String OUTPUT_PAR_LEMMA = "LEMMA";
	
	public Map<String, String> getMetadata();
	
	public List<String> getAdditionalLabels();
	
	public void addAdditionalLabel(String label);
	
	public List<RichNode> getChildren();
	
	public void addChild(RichNode node);
	
	public RichNode getParent();
	
	public void setParent(RichNode node);
	
	public String getValue();
	
	public void setValue(String value);
		
	public boolean isLeaf();
	
	public String getRepresentation(String parameterList);
	
}
