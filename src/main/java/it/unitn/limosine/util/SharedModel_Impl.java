package it.unitn.limosine.util;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
/***
 * Implements the SharedModel interface
 * 
 * Used e.g. by the NER tagger to load model from resource manager
 * 
 * @author bplank
 *
 */
public class SharedModel_Impl implements SharedModel, SharedResourceObject {

	private String pathToResource; 
	
	public void load(DataResource data) throws ResourceInitializationException {
		pathToResource = data.getUri().getRawPath(); //without file: prefix!!
	}
	public String getPath() {
		return pathToResource;
	}

}
