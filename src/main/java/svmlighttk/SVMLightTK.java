package svmlighttk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import qa.qcri.qf.classifiers.Classifier;

/**
 * 
 * Modified line in svm_common.h
 * 
 * #define MAX_NUMBER_OF_PAIRS 50000*10
 * 
 * Max number of pairs is higher because of the error:
 * "The number of identical parse nodes exceeds the current capacity"
 * 
 * This happened for some long paragraphs
 * 
 */

/**
 * 
 * Wrapper class for the native library svmlight_tk.so All the programs which
 * use this class should be invoked giving more memory to the Java Virtual
 * Machine:
 * 
 * -Xss128m
 * 
 */
public class SVMLightTK implements Classifier {

	/**
	 * svmlight_tk.so path
	 */
	public static final String SVMLIGHT_TK_RELATIVE_PATH = "tools/SVM-Light-TK-1.5.Lib/svmlight_tk.so";
	public static final String SVMLIGHT_TK;

	static {
		String path = new File(SVMLIGHT_TK_RELATIVE_PATH).getAbsolutePath();
		SVMLIGHT_TK = path;
		System.load(SVMLIGHT_TK);
	}

	final private int modelHandle;
	final private double modelThreshold;

	private static native int load_model(String modelFile);

	private static native double get_threshold();

	private static native double classify_instance(int modelNumber,
			String instance);

	/**
	 * Load a model from file
	 * 
	 * @param modelFile
	 */
	public SVMLightTK(String modelFile) {
		this.modelHandle = load_model(modelFile);
		this.modelThreshold = get_threshold();
	}

	/**
	 * Classify an instance (dependency tree string)
	 * 
	 * @param instance
	 * @return the classification confidence
	 */
	@Override
	public double classify(String instance) {
		return classify_instance(this.modelHandle, instance);
	}

	/**
	 * Get the threshold of the model
	 * 
	 * @return the threshold of the model
	 */
	public double getThreshold() {
		return this.modelThreshold;
	}

	public static void main(String[] args) {

		Classifier model = new SVMLightTK(
				"data/trec/train/svm.model");

		try {
			System.out.println("Reading file");
			BufferedReader in = new BufferedReader(new FileReader(
					"data/trec/test/svm.test"));

			if (!in.ready()) {
				in.close();
				throw new IOException();
			}

			String line;

			while ((line = in.readLine()) != null) {
				System.out.println("SCORE model: " + model.classify(line));
			}

			in.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public static Classifier newInstance(String path) {
		return new SVMLightTK(path);
	}
}
