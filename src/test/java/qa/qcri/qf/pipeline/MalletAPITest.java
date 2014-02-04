package qa.qcri.qf.pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import qa.qcri.qf.features.FeaturesUtil;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;

import com.google.common.collect.Lists;

public class MalletAPITest {

	@Test
	public void test() {

		Alphabet alphabet = new Alphabet();

		String aDocument = "I was born in June".toLowerCase();
		String bDocument = "I was not born in July".toLowerCase();

		FeatureVector aFv = produceFV(aDocument, alphabet);
		System.out.println(aDocument);
		System.out.println(aFv.toString(true));
		System.out.println(FeaturesUtil.serialize(aFv));
		
		System.out.println("");

		FeatureVector bFv = produceFV(bDocument, alphabet);
		System.out.println(bDocument);
		System.out.println(bFv.toString(true));
		System.out.println(FeaturesUtil.serialize(bFv));
		
		Assert.assertEquals(
				"i(0)=1.0 was(1)=1.0 born(2)=1.0 in(3)=1.0 june(4)=1.0 numWords(5)=5.0 hasNegation(6)=0.0",
				aFv.toString(true).trim());
		
		Assert.assertEquals(
				"i(0)=1.0 was(1)=1.0 born(2)=1.0 in(3)=1.0 numWords(5)=6.0 hasNegation(6)=1.0 not(7)=1.0 july(8)=1.0",
				bFv.toString(true).trim());
	}

	private FeatureVector produceFV(String document, Alphabet alphabet) {
		List<String> tokens = tokenize(document);

		FeatureSequence seq = new FeatureSequence(alphabet);
		for (String token : tokens) {
			seq.add(token);
		}
		
		AugmentableFeatureVector fv = new AugmentableFeatureVector(alphabet);
		fv.add(new FeatureVector(seq));

		Integer numWords = tokens.size();
		Boolean hasNegation = document.contains(" not ");

		Map<String, Double> features = new HashMap<>();
		features.put("numWords", new Double(numWords));
		features.put("hasNegation", hasNegation ? 1. : 0.);
		
		for(String featureName : features.keySet()) {
			fv.add(featureName, features.get(featureName));
		}

		return fv;
	}

	private List<String> tokenize(String document) {
		return Lists.newArrayList(document.split(" "));
	}

}
