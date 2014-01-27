package qa.qcri.qf.trees;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.maltparser.core.helper.HashMap;

public class TokenTree extends BaseRichNode {
	
	private List<RichTokenNode> tokens;
	
	private Map<String, RichTokenNode> idToToken;
	
	private int nextFreeId = 0;
	
	public TokenTree() {
		this.tokens = new ArrayList<>();
		this.idToToken = new HashMap<>();
	}
	
	public List<RichTokenNode> getTokens() {
		return this.tokens;
	}
	
	public void addToken(RichTokenNode token) {
		this.tokens.add(token);
		this.idToToken.put(String.valueOf(nextFreeId++), token);
	}
	
	public RichTokenNode getTokenById(String id) {
		assert(this.idToToken.containsKey(id) == true);
		
		return this.idToToken.get(id);
	}

}
