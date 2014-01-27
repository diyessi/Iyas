package qa.qcri.qf.datagen;

public interface Labelled {
	
	public Double POSITIVE_LABEL = 1.0;
	
	public Double NEGATIVE_LABEL = 0.0;
	
	public Double getLabel();
	
	public boolean isPositive();
}
