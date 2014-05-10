package qa.qcri.qf.tools.questionclassifier;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;

/**
 * Read questions with category from file.
 * 
 * Both the question id (qid) and question-category need to
 *     be stored in file.
 * Lines should have the following format:
 *	<qid><sep><category:focus><sep><text>
 */
public class QuestionWithIdReader implements Iterable<CategoryContent> {
	
	private final static String DEFAULT_SEPARATOR = " ";
	
	private final ReadFile in;	
	private final String separator;
	
	
	public QuestionWithIdReader(String path) {
		this(path, DEFAULT_SEPARATOR);		
	}
	
	public QuestionWithIdReader(String path, String separator) {
		this.in = new ReadFile(path);
		this.separator = separator;
	}
	
	@Override
	public Iterator<CategoryContent> iterator() {
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
				
				List<String> fields = Lists.newArrayList(Splitter.on(separator).limit(3).split(line));
				
				String id = fields.get(0);
				String category = fields.get(1).substring(0, fields.get(1).indexOf(":"));
				String content = fields.get(2);
				
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
