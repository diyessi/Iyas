package qa.qcri.qf.pipeline;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import util.ChunkReader;

import com.google.common.base.Function;

public class ChunkReaderTest {
	
	public static final int NUMBER_OF_CHUNKS = 8;
	public static final int[] CHUNK_SIZES = new int[]{3, 1, 1, 1, 3, 1, 3, 1};
	
	@Test
	public void testChunkReader() {
		
		ChunkReader cr = new ChunkReader("data/chunkreader.txt",
				new Function<String, String>() {				
					@Override
					public String apply(String str) {
						return str.substring(0, str.indexOf(" "));
					}
				});
		
		int number_of_chunks = 0;
		int[] chunk_sizes = new int[CHUNK_SIZES.length];
		
		Iterator<List<String>> chunk = cr.iterator();
		
		while(chunk.hasNext()) {
			List<String> lines = chunk.next();
			chunk_sizes[number_of_chunks] = lines.size();	
			number_of_chunks++;
		}
		
		Assert.assertEquals(NUMBER_OF_CHUNKS, number_of_chunks);
		for(int i = 0; i < CHUNK_SIZES.length; i++) {
			Assert.assertEquals(CHUNK_SIZES[i], chunk_sizes[i]);
		}
	}
	
}
