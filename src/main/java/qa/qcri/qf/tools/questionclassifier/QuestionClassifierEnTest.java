package qa.qcri.qf.tools.questionclassifier;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qa.qcri.qf.classifiers.OneVsAllClassifier;
import qa.qcri.qf.classifiers.SVMLightTKClassifierFactory;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.CategoryContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.providers.ConstituencyTreeProvider;
import qa.qcri.qf.trees.providers.TokenTreeProvider;

public class QuestionClassifierEnTest {

	public static final String TEST_CASES_DIRECTORY = "CASes/question-classifier/test.en/";

	public static final String TEST_QUESTIONS_PATH = Commons.QF_DIRECTORY
			+ "TREC_10.label";

	private static final Logger logger = LoggerFactory.getLogger(QuestionClassifierEnTest.class);

	public static void main(String[] args) throws UIMAException, IOException {
		/*
		Analyzer ae = Commons.instantiateAnalyzer(new UIMAFilePersistence(
				TEST_CASES_DIRECTORY));|
			String predictedCategory = ovaClassifier
					.getMostConfidentModel(example);

			if (predictedCategory.equals(question.getCategory())) {
				correctPredictionNumbers++;
			} else {
				String message = question.getContent() + " Predicted " + predictedCategory
						+ ". Was " + question.getCategory();
				fm.writeLn("data/question-classifier/errors.txt", message);
				logger.warn(message);
			}

			totalPredictionNumbers++;
		}

		fm.closeFiles();

		System.out.println("Correct prediction: " + correctPredictionNumbers
				+ " out of " + totalPredictionNumbers);
				*/
	}
}
