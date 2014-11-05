package qa.qcri.qf.pipeline.qademo;

import java.io.IOException;

import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.ir.lucene.Lucene;
import qa.qcri.qf.ir.lucene.LuceneDocumentBuilder;

/**
 * 
 * Index a list of paragraphs or sentences with Lucene search engine.
 * 
 * The input file must have the following format:
 * 
 * <paragraph id><tab><paragraph text><tab><metadata>
 * 
 */
public class IndexWikiDump {
	
	public static final String DATA_PATH = "data/wiki/wiki-en-dedup-sentences.txt";
	
	public static final String INDEX_PATH = "data/wiki/index";
	
	public static void main(String[] args) {	
		try {
			Lucene indexer = new Lucene();	
			indexer.createIndex(INDEX_PATH);

			ReadFile in = new ReadFile(DATA_PATH);
			
			LuceneDocumentBuilder builder = new LuceneDocumentBuilder();
			
			while(in.hasNextLine()) {
				String line = in.nextLine().trim();
				String[] fields = line.split("\t");
				
				String id = fields[0];
				String sentence = fields[1];
				String par = fields[2];
				
				builder.addStringField("id", id);
				builder.addIndexedTextField("text", sentence);
				builder.addStringField("par", par);
				
				indexer.addDocumentToIndex(builder.build());
			}
			
			in.close();			
			indexer.closeIndex();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
