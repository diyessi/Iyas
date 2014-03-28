package qa.qcri.qf.pipeline.trec;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;

public abstract class AnalyzerFactory {
	
	public static Analyzer newTrecPipeline(String lang, UIMAPersistence persistence) 
		throws UIMAException {
		if (lang == null) { 
			throw new NullPointerException("lang is null");
		}
		if (persistence == null) { 
			throw new NullPointerException("persistence is null");
		}
		
		Analyzer analyzer = null;		
		if (lang.equals("en")) {
			analyzer = newTrecPipelineEnAnalyzer(persistence);			
		} else if (lang.equals("it")) {
			analyzer = newTrecPipelineItAnalyzer(persistence);
		}
				
		return analyzer;
	}
	
	private static Analyzer newTrecPipelineEnAnalyzer(UIMAPersistence persistence) 
		throws UIMAException {
		assert persistence != null;
		
		Analyzer ae = new Analyzer(persistence);
		
		ae.addAEDesc(createEngineDescription(StanfordSegmenter.class))
		  .addAEDesc(createEngineDescription(StanfordPosTagger.class))
		  .addAEDesc(createEngineDescription(StanfordLemmatizer.class))
		  .addAEDesc(createEngineDescription(IllinoisChunker.class));
		  
		return ae;
	}
	
	private static Analyzer newTrecPipelineItAnalyzer(UIMAPersistence persistence) 
		throws UIMAException {
		assert persistence != null;
		
		Analyzer ae = new Analyzer(persistence);
		
		try {
			ae.addAEDesc(createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor"));
		} catch (IOException e) { 
			throw new UIMAException(e);
		}
		
		return ae;
	}

}
