package qa.qcri.qf.datagen;

import java.util.Map;

public class DataObject implements Labelled {
	
	protected Double label;
	
	protected String id;
	
	protected Map<String, Double> features;
	
	protected Map<String, String> metadata;

	public DataObject(Double label, String id, Map<String, Double> features,
			Map<String, String> metadata) {
		this.label = label;
		this.id = id;
		this.features = features;
		this.metadata = metadata;
	}
	
	public String getId() {
		return this.id;
	}
	
	@Override
	public Double getLabel() {
		return this.label;
	}
	
	@Override
	public boolean isPositive() {
		return this.label.compareTo(Labelled.POSITIVE_LABEL) == 0;
	}
	
 	public Map<String, Double> getFeatures() {
		return this.features;
	}
 	
 	public Map<String, String> getMetadata() {
 		return this.metadata;
 	}

}
