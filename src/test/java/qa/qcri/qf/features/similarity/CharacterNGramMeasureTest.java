package qa.qcri.qf.features.similarity;

import static org.junit.Assert.*;

import org.junit.Test;

import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.CharacterNGramMeasure;

public class CharacterNGramMeasureTest {

	@Test
	public void test() throws Exception {
		final String IDFMODEL_FILE = "data/ngrams-idf.txt";		
		CharacterNGramMeasure measure = new CharacterNGramMeasure(2, IDFMODEL_FILE);
		double sim = measure.getSimilarity("aac", "acc");
		assertTrue(sim == 1.0);
	}

}
