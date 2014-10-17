package qa.qcri.qf.tools.questionclassifier;

import java.util.Iterator;
import java.util.List;

import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class QuestionReader implements Iterable<CategoryContent> {

	//private ReadFile in;	//
	private final String path; //
	
	private int counter;
	
	public QuestionReader(String path) {
		//this.in = new ReadFile(path); //
		this.path = path; //
		this.counter = 0;
	}
	
	@Override
	public Iterator<CategoryContent> iterator() {
		final ReadFile in = new ReadFile(path);//
		Iterator<CategoryContent> iterator = new Iterator<CategoryContent>() {

			@Override
			public boolean hasNext() {
				if (!in.hasNextLine()) {
					in.close();
					return false;
				}
				return in.hasNextLine();
			}

			@Override
			public CategoryContent next() {
				String line = in.nextLine().trim();
				
				List<String> fields = Lists.newArrayList(Splitter.on(" ").limit(2).split(line));
				
				String id = String.valueOf(counter);
				String category = fields.get(0).substring(0, fields.get(0).indexOf(":"));
				String content = fields.get(1);

				counter++;
				
				return new CategoryContent(id, content, category);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}			
		};
		
		return iterator;
	}
}
