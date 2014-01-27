package qa.qcri.qf.fileutil;

import java.util.Map;

import org.maltparser.core.helper.HashMap;

public class FileManager {
	
	private Map<String, WriteFile> files;
	
	public FileManager() {
		this.files = new HashMap<>();
	}
	
	public void create(String path) {
		WriteFile out = new WriteFile(path);
		this.files.put(path, out);
	}
	
	public void write(String path, String content) {
		assert(this.files.containsKey(path));
		this.files.get(path).write(content);
	}
	
	public void writeLn(String path, String content) {
		assert(this.files.containsKey(path));
		this.files.get(path).writeLn(content);
	}
	
	public void close(String path) {
		assert(this.files.containsKey(path));
		this.files.get(path).close();
		this.files.remove(path);
	}
	
	public void closeFiles() {
		for(WriteFile out : this.files.values()) {
			out.close();
		}
		this.files.clear();
	}
}
