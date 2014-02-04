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
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;

public class Analyzer {

	private List<AnalysisEngine> aes;

	private UIMAPersistence persistence;

	private final Logger logger = LoggerFactory.getLogger(Analyzer.class);
	
	/**
	 * Constructor for Analyzer with no serialization
	 * @throws UIMAException
	 */
	public Analyzer() throws UIMAException {
		this(new UIMANoPersistence());
	}

	/**
	 * Constructor for Analyzer
	 * 
	 * @param persistence
	 *            the object implementing the serialization mechanism
	 * @throws UIMAException
	 */
	public Analyzer(UIMAPersistence persistence) throws UIMAException {
		this.persistence = persistence;
		this.aes = new ArrayList<>();
	}

	/**
	 * Instantiates an analysis engine and adds it to the internal list
	 * 
	 * @param aed
	 *            an analysis engine description
	 * @return the Analyzer object instance for chaining
	 */
	public Analyzer addAEDesc(AnalysisEngineDescription aed) {
		try {
			this.aes.add(AnalysisEngineFactory.createEngine(aed));
		} catch (ResourceInitializationException e) {
			logger.error("Failed to instantiate annotator {}", aed
					.getAnalysisEngineMetaData().getName());
			e.printStackTrace();
		}
		return this;
	}

	/**
	 * Carries out the analysis on a piece of content
	 * 
	 * @param cas
	 *            the CAS used to store the annotations
	 * @param analyzable
	 *            an Analyzable piece of content
	 */
	public void analyze(JCas cas, Analyzable analyzable) {
		/**
		 * Makes sure it works with a clean CAS
		 */
		cas.reset();

		/**
		 * Fills the CAS with the content to analyze
		 */
		cas.setDocumentText(analyzable.getContent());
		cas.setDocumentLanguage(analyzable.getLanguage());

		/**
		 * Retrieves the content id, vital for the serialization mechanism to
		 * retrieve the content
		 */
		String id = analyzable.getId();

		if (this.persistence.isAlreadySerialized(id)) {
			this.persistence.deserialize(cas, id);
		} else {
			for (AnalysisEngine ae : this.aes) {
				try {
					SimplePipeline.runPipeline(cas, ae);
				} catch (AnalysisEngineProcessException e) {
					logger.error("Failed to run annotator {} on content: {}",
							ae.getAnalysisEngineMetaData().getName(),
							cas.getDocumentText());
					e.printStackTrace();
				}
			}

			this.persistence.serialize(cas, id);
		}
	}

	/**
	 * Changes the persistence submodule
	 * 
	 * @param persistence
	 */
	public void setPersistence(UIMAPersistence persistence) {
		this.persistence = persistence;
	}

}
