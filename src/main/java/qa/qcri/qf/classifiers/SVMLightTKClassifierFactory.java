package qa.qcri.qf.classifiers;

import svmlighttk.SVMLightTK;
import svmlighttk.SVMLightTK_C;

public class SVMLightTKClassifierFactory implements ClassifierFactory {

	@Override
	public Classifier createClassifier(String path) {
		return new SVMLightTK_C(path);
	}

}
