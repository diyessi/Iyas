package qa.qcri.qf.datagen.ngram;

import static org.junit.Assert.*;

import org.apache.uima.UIMAException;
import org.junit.Before;
import org.junit.Test;

import qa.qcri.qf.datagen.ngram.CharacterNGramIdf;
import qa.qcri.qf.datagen.ngram.IdfModel;
import qa.qcri.qf.datagen.ngram.IdfStore;

public class CharacterNGramIdfTest {
	
	@Before
	public void saveModel() throws UIMAException {
		IdfModel idfModel = CharacterNGramIdf.buildModel(1, 2, "data/ngrams-test.txt");
		idfModel.saveModel("data/ngrams-idf.txt");
	}

	@Test
	public void tesIdfModelWith2GramAppearing2TimesOutOf3() throws UIMAException {
		IdfModel idfModel = new IdfStore("data/ngrams-idf.txt");				
		assertTrue(idfModel.getIdf("ac") == 1.5);
	}
	
	@Test
	public void testIdfModelWith1GramAppearing3TimesOutOf3() throws UIMAException {
		IdfModel idfModel = new IdfStore("data/ngrams-idf.txt");
		assertTrue(idfModel.getIdf("a") == 1);
	}

}
