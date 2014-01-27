package qa.qcri.qf.pipeline.retrieval;

/**
 * 
 * Simple content contains just the data needed for carrying out analysis on
 * text
 */
public class SimpleContent implements Analyzable {

	private String id;
	private String content;
	private String lang = "en";

	public SimpleContent(String id, String content) {
		this.id = id;
		this.content = content;
	}
	
	public SimpleContent(String id, String content, String lang) {
		this.id = id;
		this.content = content;
		this.content = lang;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getContent() {
		return this.content;
	}

	@Override
	public String getLanguage() {
		return this.lang;
	}

	@Override
	public String toString() {
		return "[id=" + this.id + ",content=" + this.content + "]";
	}

}
