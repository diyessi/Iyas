package qa.qcri.qf.treemarker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qa.qcri.qf.trees.RichTokenNode;
import qa.qcri.qf.trees.TokenTree;

public class MarkTreesOnRepresentation {
	
	private MarkingStrategy markingStrategy;
	
	public MarkTreesOnRepresentation(MarkingStrategy markingStrategy) {
		this.markingStrategy = markingStrategy;
	}
	
	public void markTrees(TokenTree a, TokenTree b, String parameterList) {
		
		List<RichTokenNode> tokenNodesFromA = a.getTokens();
		List<RichTokenNode> tokenNodesFromB = b.getTokens();
		
		List<RichTokenNode> longestList = tokenNodesFromA;
		List<RichTokenNode> shortestList = tokenNodesFromB;
		
		if(longestList.size() < shortestList.size()) {
			longestList = tokenNodesFromB;
			shortestList = tokenNodesFromA;
		}
		
		Map<String, List<RichTokenNode>> formToNodes = new HashMap<>();
		for(RichTokenNode richToken : longestList) {
			String form = richToken.getRepresentation(parameterList);
			if(!formToNodes.containsKey(form)) {
				formToNodes.put(form, new ArrayList<RichTokenNode>());
			}
			formToNodes.get(form).add(richToken);
		}
		
		for(RichTokenNode richToken : shortestList) {
			String form = richToken.getRepresentation(parameterList);
			List<RichTokenNode> matchingNodes = formToNodes.get(form);
			if(matchingNodes != null) {		
				for(RichTokenNode matchingNode : matchingNodes) {
					Marker.addRelationalTag(matchingNode, this.markingStrategy);
				}
				Marker.addRelationalTag(richToken, this.markingStrategy);
			}
		}
	}
	
}
