package qa.qcri.qf.uimaas;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class PipelineDescriptorWriter {
	
	public static void main(String[] args) throws ResourceInitializationException, FileNotFoundException, SAXException, IOException {
		
		
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(createEngineDescription(StanfordSegmenter.class));
		builder.add(createEngineDescription(OpenNlpPosTagger.class,
				 StanfordPosTagger.PARAM_LANGUAGE, "en"));
		
		/*
		builder.add(createEngineDescription(StanfordLemmatizer.class));
		builder.add(createEngineDescription(IllinoisChunker.class));
		builder.add(createEngineDescription(StanfordNamedEntityRecognizer.class,
				StanfordNamedEntityRecognizer.PARAM_LANGUAGE, "en",
				StanfordNamedEntityRecognizer.PARAM_VARIANT, "muc.7class.distsim.crf"));			
		builder.add(createEngineDescription(StanfordParser.class));
		*/
		
		AnalysisEngineDescription desc = builder.createAggregateDescription();
		
		desc.toXML(new FileOutputStream("desc/uima-as/pipeline4uima-as.xml"));
		
		createEngineDescription(StanfordSegmenter.class)
			.toXML(new FileOutputStream("desc/uima-as/segmenter.xml"));
		
		createEngineDescription(OpenNlpPosTagger.class,
				 StanfordPosTagger.PARAM_LANGUAGE, "en")
			.toXML(new FileOutputStream("desc/uima-as/postagger.xml"));
	}
	
}
