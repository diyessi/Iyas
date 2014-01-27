package util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import qa.qcri.qf.fileutil.ReadFile;
import util.functions.InStringOutString;

public class ChunkReader implements Iterable<List<String>> {

	private ReadFile in;
	
	private List<String> linesBuffer = new ArrayList<>();
	
	private String previousExtractedItem = null;
	
	private InStringOutString groupingItemExtraction;
	
	public ChunkReader(String path, InStringOutString groupingItemExtraction) {
		this.in = new ReadFile(path);
		this.groupingItemExtraction = groupingItemExtraction;
	}
	
	@Override
	public Iterator<List<String>> iterator() {
		Iterator<List<String>> iterator = new Iterator<List<String>>() {

			@Override
			public boolean hasNext() {
				if(linesBuffer.isEmpty() && !in.hasNextLine()) {
					return false;
				} else {
					return true;
				}
			}

			@Override
			public List<String> next() {
				
				List<String> returnList = new ArrayList<>();
				
				if(!linesBuffer.isEmpty() && !in.hasNextLine()) {
					returnList = copyList(linesBuffer);
					linesBuffer.clear();
					return returnList;
				}
				
				while(in.hasNextLine()) {
					String currentLine = in.nextLine().trim();
					
					if(currentLine.isEmpty()) continue;
					
					String extractedItem = groupingItemExtraction.apply(currentLine);
					if(previousExtractedItem == null) {
						previousExtractedItem = extractedItem;
						linesBuffer.add(currentLine);
					} else {
						if(previousExtractedItem.equals(extractedItem)) {
							linesBuffer.add(currentLine);
						} else {						
							returnList = copyList(linesBuffer);
							previousExtractedItem = extractedItem;
							linesBuffer.clear();
							linesBuffer.add(currentLine);
							
							return returnList;
						}
					}
				}
				
				if(linesBuffer.isEmpty()) {
					return new ArrayList<String>();
				} else {
					returnList = copyList(linesBuffer);
					linesBuffer.clear();
					return returnList;
				}
				
			}
			
			private List<String> copyList(List<String> list) {
				List<String> copy = new ArrayList<>();
				for(String item : list) {
					copy.add(item);
				}
				return copy;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		
		return iterator;
	}

	
}
