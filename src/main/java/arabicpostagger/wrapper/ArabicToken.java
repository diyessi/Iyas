package arabicpostagger.wrapper;

public class ArabicToken {
	
	public final String surfaceForm;
	public final int beginPos;
	public final int endPos;
	public String posTag;
	public String bioTag;
	public String originalTaggedToken;
	
	public ArabicToken(String surfaceForm, int beginPos, int endPos) {
		this.surfaceForm = surfaceForm;
		this.beginPos = beginPos;
		this.endPos = endPos;
		this.posTag = "";
		this.bioTag = "O";
		this.originalTaggedToken = "";
	}
	
	public void setPosTag(String tag) {
		this.posTag = tag;
	}
	
	public void setBioTag(String tag) {
		this.bioTag = tag;
	}
	
	public void setOriginalTaggedToken(String originalTaggedToken) {
		this.originalTaggedToken = originalTaggedToken;
	}
	
	public String getPosTag() {
		return this.posTag;
	}
	
	public String getBioTag() {
		return this.bioTag;
	}
	
	public String getOriginalTaggedToken() {
		return this.originalTaggedToken;
	}
	
}
