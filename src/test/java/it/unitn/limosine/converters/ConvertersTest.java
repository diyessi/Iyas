package it.unitn.limosine.converters;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;

public class ConvertersTest {

	@Test
	public void convertersTest() throws UIMAException {

		LimosineConverter converter = new LimosineConverter("CASes/test")
				.readLimosineAnnotations("limosine-converters");

		JCas cas = converter.getLimosineCas();
		converter.convertSegmentation(cas);
		converter.convertNamedEntities(cas);
		
		new UIMAFilePersistence("CASes/test").serialize(cas, "limosine-converted");
	}
}
