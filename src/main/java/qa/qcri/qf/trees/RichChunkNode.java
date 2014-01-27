package qa.qcri.qf.trees;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;

public class RichChunkNode extends BaseRichNode {

	private Chunk chunk;
	
	public RichChunkNode(Chunk chunk) {
		super();
		this.chunk = chunk;
		this.metadata.put(RichNode.TYPE_KEY, RichNode.TYPE_CHUNK_NODE);
		this.value = chunk.getChunkValue();
	}
	
	public Chunk getChunk() {
		return this.chunk;
	}
	
}
