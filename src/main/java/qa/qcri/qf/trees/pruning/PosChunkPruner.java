package qa.qcri.qf.trees.pruning;

import java.util.List;

import qa.qcri.qf.trees.TreeUtil;
import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class PosChunkPruner implements Pruner {
	
	private int radius;
	
	public PosChunkPruner(int radius) {
		this.radius = radius;
	}

	@Override
	public RichNode prune(RichNode tree, Function<List<RichNode>, List<Boolean>> pruningCriteria) {
		List<RichNode> leaves = TreeUtil.getLeaves(tree);	
		List<Boolean> leavesToPruneIndexes = pruningCriteria.apply(leaves);
		
		final int leavesSize = leaves.size();
		
		assert leavesSize == leavesToPruneIndexes.size();
		
		if(this.radius > 0) {
			Boolean[] pruneIndexes = new Boolean[leavesSize];
			for(int i = 0; i < leavesSize; i++) {
				pruneIndexes[i] = true;
			}
			
			for(int i = 0; i < leavesSize; i++) {
				if(leavesToPruneIndexes.get(i) == false) {
					/*
					 * If a node must not be pruned we give the same
					 * status to its neighbors within the radius
					 */
					for(int j = i - this.radius; j <= i + this.radius; j++) {
						if(0 <= j && j < leavesSize) {
							pruneIndexes[j] = false;
						}
					}
				}
			}
			
			leavesToPruneIndexes = Lists.newArrayList(pruneIndexes);
		}
		
		for(int i = 0; i < leavesSize; i++) {
			if(leavesToPruneIndexes.get(i)) {
				RichNode posTag = leaves.get(i).getParent();
				RichNode chunk = posTag.getParent();
				chunk.getChildren().remove(posTag);
				if(chunk.isLeaf()) {
					RichNode sentence = chunk.getParent();
					sentence.getChildren().remove(chunk);
				}
			}
		}
		
		return tree;
	}

}
