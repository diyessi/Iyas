package it.unitn.limosine.italian.syntax.constituency;

import static org.junit.Assert.assertEquals;
import it.unitn.limosine.types.syntax.ConstituencyTree;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

public class BerkeleyWrapperTest {
	
	private static final  String TEXTPRO_ALL_IN_ONE_DESCRIPTOR = "desc/Limosine/TextProAllInOneDescriptor";
	private static final String BERKELEY_IT_DESCRIPTOR = "desc/Limosine/BerkeleyITDescriptor";	
	
	private final String grammar_file = "tools/TextPro1.5.2_Linux64bit/ParseBer/italian_parser/BerkeleyParser-Italian/tutall-fulltrain";
	private final String expectdParsedTree = 
	"(ROOT (Start (S (NP (SPN Renault)) (VXI (VI ha)) (NP (RS una) (ADJP (AS completa)) (SS gamma) (PX (E di) (NP (SP veicoli) (ADJP (AP elettrici)))))) (XPS .)))";
	
	private AnalysisEngine analysisEngine = null;

	@Before
	public void setUp() throws Exception { 
		List<AnalysisEngineDescription> aeDescList = new ArrayList<>();
		aeDescList.add(AnalysisEngineFactory.createEngineDescription(TEXTPRO_ALL_IN_ONE_DESCRIPTOR));
		aeDescList.add(AnalysisEngineFactory.createEngineDescription(BERKELEY_IT_DESCRIPTOR,
				BerkeleyWrapper.PARAM_GRAMMARFILE, grammar_file,
				BerkeleyWrapper.PARAM_ACCURATE, true,
				BerkeleyWrapper.PARAM_MAXLENGTH, 250,
				BerkeleyWrapper.PARAM_USEGOLDPOS, true));
		AnalysisEngineDescription aeDesc = AnalysisEngineFactory.
				createEngineDescription(aeDescList.toArray(new AnalysisEngineDescription[0]));
		analysisEngine = AnalysisEngineFactory.createEngine(aeDesc);
	}
	
	@Test
	public void test() throws Exception {
		JCas cas = JCasFactory.createJCas();
		String jcastxt = "Renault ha una completa gamma di veicoli elettrici.";
		cas.setDocumentText(jcastxt);		
		
		analysisEngine.process(cas);
		
		ConstituencyTree pennTree = JCasUtil.selectSingle(cas, ConstituencyTree.class);
		//TokenTree tokenTree = RichTree.getConstituencyTree(cas);
		//String parsedTree = new TreeSerializer().serializeTree(tokenTree);
		System.out.println(pennTree.getRawParse());
		assertEquals(expectdParsedTree, pennTree.getRawParse());
	}
	
	@Test
	public void testMultiple() throws Exception {
		JCas cas = JCasFactory.createJCas();
		
		String jcastxt =  "Quagliarella favorito su Llorente. ";
		jcastxt += "Juve, la sfida per affiancare Tevez. ";
		jcastxt += "Più Quagliarella di Llorente. ";
		jcastxt += "Il borsino dell’attacco bianconero a tre giorni ";
		jcastxt += "dal Derby d’Italia segna in deciso rialzo le quotazioni ";
		jcastxt += "della punta napoletana nel ballottaggio per un posto da titolare con Carlitos Tevez.";
		cas.setDocumentText(jcastxt);
	}

}
