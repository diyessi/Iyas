package qa.qcri.qf.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;

public class Analyzer {

	private List<AnalysisEngine> aes;

	private UIMAPersistence persistence;

	private final Logger logger = LoggerFactory.getLogger(Analyzer.class);

	public Analyzer(UIMAPersistence persistence) throws UIMAException {
		this.persistence = persistence;
		this.aes = new ArrayList<>();
	}

	public Analyzer addAEDesc(AnalysisEngineDescription aed) {
		try {
			this.aes.add(AnalysisEngineFactory.createEngine(aed));
		} catch (ResourceInitializationException e) {
			logger.error("Failed to instantiate annotator {}",
					aed.getAnalysisEngineMetaData().getName());
			e.printStackTrace();
		}
		return this;
	}

	public void analyze(JCas cas, Analyzable analyzable) {
		cas.reset();
		
		cas.setDocumentText(analyzable.getContent());
		cas.setDocumentLanguage(analyzable.getLanguage());

		String id = analyzable.getId();

		if (this.persistence.isAlreadySerialized(id)) {
			this.persistence.deserialize(cas, id);
		} else {
			for (AnalysisEngine ae : this.aes) {
				try {
					SimplePipeline.runPipeline(cas, ae);
				} catch (AnalysisEngineProcessException e) {
					logger.error("Failed to run annotator {} on content: {}",
							ae.getAnalysisEngineMetaData().getName());
					e.printStackTrace();
				}
			}

			this.persistence.serialize(cas, id);
		}
	}
}
