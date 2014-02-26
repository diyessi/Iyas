package qa.qcri.qf.lda;

import java.io.File;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.topics.ParallelTopicModel;

public class LdaModelResource implements LdaModel,
		SharedResourceObject {
	
	private ParallelTopicModel topicModel;
	
	private static Logger logger = LoggerFactory.getLogger(LdaModelResource.class);

	@Override
	public void load(DataResource aData) throws ResourceInitializationException {
		String modelFile = aData.getUri().getPath();
		logger.info("LDA model file: " + modelFile);
				
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

}
