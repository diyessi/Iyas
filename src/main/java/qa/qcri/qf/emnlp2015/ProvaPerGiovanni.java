package qa.qcri.qf.emnlp2015;

import it.uniroma2.sag.kelp.data.dataset.SimpleDataset;
import it.uniroma2.sag.kelp.data.example.Example;
import it.uniroma2.sag.kelp.data.label.StringLabel;
import it.uniroma2.sag.kelp.data.manipulator.TreePairRelTagger;
import it.uniroma2.sag.kelp.data.representation.structure.filter.LexicalStructureElementFilter;
import it.uniroma2.sag.kelp.data.representation.structure.similarity.ExactMatchingStructureElementSimilarity;
import it.uniroma2.sag.kelp.data.representation.tree.node.filter.ContentBasedTreeNodeFilter;
import it.uniroma2.sag.kelp.data.representation.tree.node.filter.TreeNodeFilter;
import it.uniroma2.sag.kelp.data.representation.tree.node.similarity.ContentBasedTreeNodeSimilarity;
import it.uniroma2.sag.kelp.data.representation.tree.node.similarity.TreeNodeSimilarity;
import it.uniroma2.sag.kelp.kernel.Kernel;
import it.uniroma2.sag.kelp.kernel.cache.DynamicIndexKernelCache;
import it.uniroma2.sag.kelp.kernel.cache.DynamicIndexSquaredNormCache;
import it.uniroma2.sag.kelp.kernel.onPairs.UncrossedPairwiseSumKernel;
import it.uniroma2.sag.kelp.kernel.standard.LinearKernelCombination;
import it.uniroma2.sag.kelp.kernel.standard.NormalizationKernel;
import it.uniroma2.sag.kelp.kernel.tree.PartialTreeKernel;
import it.uniroma2.sag.kelp.kernel.tree.deltamatrix.DynamicDeltaMatrix;
import it.uniroma2.sag.kelp.kernel.vector.LinearKernel;
import it.uniroma2.sag.kelp.learningalgorithm.classification.libsvm.BinaryCSvmClassification;
import it.uniroma2.sag.kelp.predictionfunction.classifier.BinaryClassifier;
import it.uniroma2.sag.kelp.predictionfunction.classifier.BinaryMarginClassifierOutput;
import it.uniroma2.sag.kelp.utils.evaluation.BinaryClassificationEvaluator;

import java.util.HashSet;



public class ProvaPerGiovanni {
	
	private static final String representationName = "tree";
	
	public static void main(String[] args) throws Exception {
		if(args.length!=3){
			System.out.println("EXPECTED 3 ARGUMENTS:");
			int index = 1;
			System.out.println(index + ") train path");
			index++;
			System.out.println(index + ") test path");
			index++;
			System.out.println(index + ") kernel type (PTK, SIM, PTK+SIM)");
			index++;
			System.exit(0);
		}
		
		int index = 0;
		String traindataPath = args[index];
		index++;
		String testdataPath = args[index];
		index++;
		String kernelType = args[index];
		index++;

		SimpleDataset trainset = new SimpleDataset();
		trainset.populate(traindataPath);
		
		SimpleDataset testset = new SimpleDataset();
		testset.populate(testdataPath);
		
		SimpleDataset completeDataset = new SimpleDataset();
		completeDataset.addExamples(trainset);
		completeDataset.addExamples(testset);
		
		StringLabel label = new StringLabel("Good");

		HashSet<String> stopwords = new HashSet<String>();
		stopwords.add("be");
		stopwords.add("have");

		HashSet<String> posOfInterest = new HashSet<String>();
		posOfInterest.add("n");
		posOfInterest.add("v");
		posOfInterest.add("j");
		posOfInterest.add("r");
		LexicalStructureElementFilter elementFilter = new LexicalStructureElementFilter(stopwords, posOfInterest);

		TreeNodeFilter nodeFilter = new ContentBasedTreeNodeFilter(elementFilter);


		ExactMatchingStructureElementSimilarity exactMatching = new ExactMatchingStructureElementSimilarity(true);
		TreeNodeSimilarity contentNodeSimilarity = new ContentBasedTreeNodeSimilarity(exactMatching);


		
		TreePairRelTagger newTagger = new TreePairRelTagger(2, 0, representationName, nodeFilter, it.uniroma2.sag.kelp.data.manipulator.TreePairRelTagger.MARKING_POLICY.ON_NODE_LABEL, contentNodeSimilarity, 1);
		completeDataset.manipulate(newTagger);

		
		System.out.println("----- TRAINING STATS: ");
		System.out.println("good examples: " + trainset.getNumberOfPositiveExamples(label));
		System.out.println("not good examples: " + trainset.getNumberOfNegativeExamples(label));
		System.out.println("total: " + trainset.getNumberOfExamples());
		
		System.out.println();
		System.out.println("----- TEST STATS: ");
		System.out.println("good examples: " + testset.getNumberOfPositiveExamples(label));
		System.out.println("not good examples: " + testset.getNumberOfNegativeExamples(label));
		System.out.println("total: " + testset.getNumberOfExamples());
		
		float c = 1;

		Kernel kernel = getKernel(kernelType, completeDataset.getNumberOfExamples());
		BinaryCSvmClassification svm = new BinaryCSvmClassification(kernel, label, c, c, false);
		
		svm.learn(trainset);
		BinaryClassifier classifier = svm.getPredictionFunction();
		BinaryClassificationEvaluator evaluator = new BinaryClassificationEvaluator(label);
	
		
		for(Example example : testset.getExamples()){
			BinaryMarginClassifierOutput prediction = classifier.predict(example);
			evaluator.addCount(example, prediction);
		}


		
		
		System.out.println("ACC: " + evaluator.getAccuracy());
		System.err.println("PREC: " + evaluator.getPrecision());
		System.out.println("REC: " + evaluator.getRecall());
		System.out.println("F1: " + evaluator.getF1());
		
	}
	
	public static Kernel getKernel(String kernelType, int numberOfExamples){
		if(kernelType.equals("PTK")){
			PartialTreeKernel ptk = new PartialTreeKernel(0.4f, 0.4f, 1, "tree");
			ptk.setSquaredNormCache(new DynamicIndexSquaredNormCache(numberOfExamples*2));
			ptk.setDeltaMatrix(new DynamicDeltaMatrix());
			NormalizationKernel norm = new NormalizationKernel(ptk);
//			UncrossedSumOfProductsPairKernel pairwiseKernel = new UncrossedSumOfProductsPairKernel(norm);
			UncrossedPairwiseSumKernel pairwiseKernel = new UncrossedPairwiseSumKernel(norm, true);
			pairwiseKernel.setKernelCache(new DynamicIndexKernelCache(numberOfExamples));
			return pairwiseKernel;
		}else if(kernelType.equals("SIM")){
			LinearKernel kernel = new LinearKernel("features");
			kernel.setKernelCache(new DynamicIndexKernelCache(numberOfExamples));
			return kernel;
		}else if(kernelType.equals("PTK+SIM")){
			PartialTreeKernel ptk = new PartialTreeKernel(0.4f, 0.4f, 1, "tree");
			ptk.setSquaredNormCache(new DynamicIndexSquaredNormCache(numberOfExamples*2));
			ptk.setDeltaMatrix(new DynamicDeltaMatrix());
			NormalizationKernel norm = new NormalizationKernel(ptk);
//			UncrossedSumOfProductsPairKernel pairwiseKernel = new UncrossedSumOfProductsPairKernel(norm);
			UncrossedPairwiseSumKernel pairwiseKernel = new UncrossedPairwiseSumKernel(norm, true);
			LinearKernel linearKernel = new LinearKernel("features");
			LinearKernelCombination comb = new LinearKernelCombination();
			comb.addKernel(1, pairwiseKernel);
			comb.addKernel(1, linearKernel);
			comb.setKernelCache(new DynamicIndexKernelCache(numberOfExamples));
			return comb;
		}else{
			System.out.println("UNRECOGNIZED KERNEL " + kernelType);
			System.exit(0);
			return null;
		}
	}

}
