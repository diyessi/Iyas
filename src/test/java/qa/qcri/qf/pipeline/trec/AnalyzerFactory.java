package qa.qcri.qf.pipeline.trec;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import qa.qcri.qf.italian.syntax.constituency.BerkeleyWrapper;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.annotators.QuestionClassifier;
import qa.qcri.qf.annotators.QuestionFocusClassifier;
import qa.qcri.qf.chunker.ConstituencyTreeChunker;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;

public abstract class AnalyzerFactory {

	public static final String QUESTION_ANALYSIS = "QUESTION_ANALYSIS";
	private static final Logger logger = LoggerFactory
			.getLogger(AnalyzerFactory.class);

	public static Analyzer newTrecPipeline(String lang,
			UIMAPersistence persistence) throws UIMAException {

		if (lang == null) {
			throw new NullPointerException("The String parameter lang is null");
		}

		if (persistence == null) {
			throw new NullPointerException(
					"The UIMAPersistence parameter persistence is null");
		}

		Analyzer analyzer = null;

		switch (lang) {
		case "en":
			logger.info("instantiating en analyzer");
			analyzer = newTrecPipelineEnAnalyzer(persistence);
			break;
		case "it":
			logger.info("instantiating it analyzer");
			analyzer = newTrecPipelineItAnalyzer(persistence);
			break;
		default:
			logger.info("instantiating en analyzer (default)");
			analyzer = newTrecPipelineEnAnalyzer(persistence);
			break;
		}

		return analyzer;
	}

	private static Analyzer newTrecPipelineEnAnalyzer(
			UIMAPersistence persistence) throws UIMAException {
		assert persistence != null;

		Analyzer ae = new Analyzer(persistence);

		AnalysisEngine stanfordSegmenter = AnalysisEngineFactory
				.createEngine(createEngineDescription(StanfordSegmenter.class));

		/**
		 * StanfordPosTagger puts wrong POS-tags on parentheses
		 * The OpenNlpPosTagger is our choice for now.
		 */
		AnalysisEngine stanfordPosTagger = AnalysisEngineFactory
				.createEngine(createEngineDescription(OpenNlpPosTagger.class,
						 StanfordPosTagger.PARAM_LANGUAGE, "en"));

		AnalysisEngine stanfordLemmatizer = AnalysisEngineFactory
				.createEngine(createEngineDescription(StanfordLemmatizer.class));

		AnalysisEngine illinoisChunker = AnalysisEngineFactory
				.createEngine(createEngineDescription(IllinoisChunker.class));

		AnalysisEngine stanfordParser = AnalysisEngineFactory
				.createEngine(createEngineDescription(StanfordParser.class));

		AnalysisEngine stanfordNamedEntityRecognizer = AnalysisEngineFactory
				.createEngine(createEngineDescription(StanfordNamedEntityRecognizer.class,
						StanfordNamedEntityRecognizer.PARAM_LANGUAGE, "en",
						StanfordNamedEntityRecognizer.PARAM_VARIANT, "muc.7class.distsim.crf"));

		AnalysisEngine questionClassifier = AnalysisEngineFactory
				.createEngine(createEngineDescription(QuestionClassifier.class,
						QuestionClassifier.PARAM_LANGUAGE, "en",
						QuestionClassifier.PARAM_MODELS_DIRPATH, "data/question-classifier_en/models"));

		AnalysisEngine questionFocusClassifier = AnalysisEngineFactory
				.createEngine(createEngineDescription(QuestionFocusClassifier.class,
						QuestionFocusClassifier.PARAM_LANGUAGE, "en",
						QuestionFocusClassifier.PARAM_MODEL_PATH, "data/question-focus_en/svm.model"));

		ae.addAE(stanfordSegmenter)
			.addAE(stanfordPosTagger)
			.addAE(stanfordLemmatizer)
			.addAE(illinoisChunker)
			.addAE(stanfordNamedEntityRecognizer);

		ae.addAE(stanfordSegmenter, QUESTION_ANALYSIS)
			.addAE(stanfordPosTagger, QUESTION_ANALYSIS)
			.addAE(stanfordLemmatizer, QUESTION_ANALYSIS)
			.addAE(illinoisChunker, QUESTION_ANALYSIS)
			.addAE(stanfordNamedEntityRecognizer, QUESTION_ANALYSIS)
			.addAE(stanfordParser, QUESTION_ANALYSIS)
			.addAE(questionClassifier, QUESTION_ANALYSIS)
			.addAE(questionFocusClassifier, QUESTION_ANALYSIS);

		return ae;
	}

	private static Analyzer newTrecPipelineItAnalyzer(
			UIMAPersistence persistence) throws UIMAException {
		assert persistence != null;

		Analyzer ae = new Analyzer(persistence);
		final String GRAMMAR_FILE = "tools/TextPro1.5.2_Linux64bit/ParseBer/italian_parser/BerkeleyParser-Italian/tutall-fulltrain";

		try {
			ae.addAEDesc(createEngineDescription("desc/Iyas/TextProAllInOneDescriptor"));
			ae.addAEDesc(createEngineDescription("desc/Iyas/BerkeleyITDescriptor",
					BerkeleyWrapper.PARAM_GRAMMARFILE, GRAMMAR_FILE,
					BerkeleyWrapper.PARAM_ACCURATE, true,
					BerkeleyWrapper.PARAM_MAXLENGTH, 250,
					BerkeleyWrapper.PARAM_USEGOLDPOS, true));
			ae.addAE(createEngine(ConstituencyTreeChunker.class));
		} catch (IOException e) {
			throw new UIMAException(e);
		}

		return ae;
	}

}
