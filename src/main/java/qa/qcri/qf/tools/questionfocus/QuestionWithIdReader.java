package qa.qcri.qf.tools.questionfocus;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import qa.qcri.qf.fileutil.ReadFile;

/**
 * Read questions with annotated focus from file.
 * 
 * Both the question id (qid) and focus-annotated text need to
 * 	be stored on file. 
 * Lines should have the following format: 
 *  <qid><sep><text-with-focus>
 *
 */
public class QuestionWithIdReader implements Iterable<QuestionWithAnnotatedFocus> {
	
	private final static String DEFAULT_SEPARATOR = " ";
	
	private final ReadFile in;	
	private final String separator;
	
	public QuestionWithIdReader(String path) {
		this(path, DEFAULT_SEPARATOR);
	}
	
	public QuestionWithIdReader(String path, String separator) { 
		if (path == null)
			throw new NullPointerException("path is null");
		if (separator == null)
			throw new NullPointerException("separator is null");
		
		this.in = new ReadFile(path);
		this.separator = separator;
	}

	@Override
	public Iterator<QuestionWithAnnotatedFocus> iterator() {
		Iterator<QuestionWithAnnotatedFocus> iterator = new Iterator<QuestionWithAnnotatedFocus>() {

			@Override
			public boolean hasNext() {
				if (!in.hasNextLine()) {
					in.close();
					return false;
				}
				return in.hasNextLine();
			}			

			@Override
			public QuestionWithAnnotatedFocus next() {
				String line = in.nextLine().trim();
				
				List<String> fields = 
						Lists.newArrayList(
								Splitter.on(separator).limit(2).split(line));
		
				String id = fields.get(0);
				String content = fields.get(1);
				
				return new QuestionWithAnnotatedFocus(id, content);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();				
			} 			
		};
		
		return iterator;
	}	

}
