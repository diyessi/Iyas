package qa.qcri.qf.lda;

import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.File;

import org.slf4j.LoggerFactory;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.slf4j.Logger;

import cc.mallet.topics.ParallelTopicModel;

public class LdaModelResource implements LdaModel,
		SharedResourceObject {
	
	private ParallelTopicModel topicModel;
	
	private static Logger logger = LoggerFactory.getLogger(LdaModelResource.class);

	@Override
	public void load(DataResource aData) throws ResourceInitializationException {
		String modelFile = aData.getUri().getPath();
		System.out.println("modelFile: " + modelFile);
				
		try {
			topicModel = ParallelTopicModel.read(new File(modelFile));
		} catch (Exception e) {
			logger.error("Error while reading topicModel from file: " + modelFile);
			throw new ResourceInitializationException(e);
		}		
	}

	@Override
	public ParallelTopicModel getModel() {
		return topicModel;
	}
	
	public static void main(String[] args) { 
		File file = new File("data/lda/answerbag/model/train.topics100.model");
		System.out.println("Does file \"" + file + "\" exist? " + file.exists());
		ExternalResourceDescription extDesc = createExternalResourceDescription(
				LdaModelResource.class, new File("data/lda/answerbag/model/train.topics100.model"));
		
	}

}
