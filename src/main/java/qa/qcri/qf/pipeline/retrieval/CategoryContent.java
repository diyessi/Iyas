package qa.qcri.qf.pipeline.retrieval;

public class CategoryContent implements Analyzable {
	
	private String id;
	private String content;
	private String lang = "en";
	private String category;

	public CategoryContent(String id, String content, String category) {
		this.id = id;
		this.content = content;
		this.category = category;
	}
	
	public CategoryContent(String id, String content, String category, String lang) {
		this.id = id;
		this.content = content;
		this.category = category;
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
		return "[id=" + this.id + ",content=" + this.content + ",category=" + this.category + "]";
	}
	
	public String getCategory() {
		return this.category;
	}
}
