package it.unitn.limosine.italian.textpro;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
//import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
* Test for BowProvider class.
*
*/
public class ItalianPipelineFailsOnUtf8 {

	private JCas cas = null;

	@Before
	public void setUp() throws Exception {
		String str = "Ok hai ragione e prodotta in Italia , in ogni caso non è un prodotto che può competere con altri modelli dello stesso segmento A . basta pensare alla Renault che già possiede una completa gamma di veicoli elettrici cosa che la Fiat si sogna.... . Quattroute prima era una rivista seria non ora che spesso e di parte...... . ";
		// String str = "Ok Quattroute prima era una rivista seria non ora che spesso e di parte...... . ";
		SimpleContent content = new SimpleContent("problem", str);
		cas = initCas(content);
	}

	private JCas initCas(Analyzable content) throws Exception {
		assert content != null;

		JCas cas = JCasFactory.createJCas();
		// Analyzer analyzer = new Analyzer(new UIMAFilePersistence("CASes/problem"));
		Analyzer analyzer = new Analyzer();
		analyzer.addAEDesc(createEngineDescription("desc/Limosine/TextProAllInOneDescriptor"));
		analyzer.analyze(cas, content);

		return cas;
	}

	@Test
	public void testAnnotate() {
		for (Sentence sent : JCasUtil.select(cas, Sentence.class)) {
			System.out.println(sent.getCoveredText());
		}
		//for (Token token : JCasUtil.select(cas, Token.class)) {
		//	System.out.println(token.getCoveredText() + " " + token.getBegin() + ":" + token.getEnd());
	}
}