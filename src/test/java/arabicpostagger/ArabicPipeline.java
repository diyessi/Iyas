package arabicpostagger;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;
import org.junit.Test;
import org.uimafit.util.JCasUtil;

import qa.qcri.qf.annotators.arabic.ArabicAnalyzer;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.type.NormalizedText;

public class ArabicPipeline {
	
	private Analyzer ae;

	private JCas preliminaryCas;
	
	private JCas cas;
	
	public static final String CAS_DIR = "arabicCASes/";
	
	public static final String SAMPLE_SENTENCE = "قال بيان اصدره حزب الحريه والعداله، الذراع السياسي لجماعه الإخوان المسلمين، حصلت \"رصد\" علي نسخه منه، بشان الاحداث الدمويه بالمدينه الجامعيه بالازهر، ان الانقلابيين فقدوا صوابهم، ويحاولون صرف الانظار عن فشلهم في تحقيق الامن للمصريين، وحمايه جنودنا بسيناء في مشهد يؤكد تكراره تورط الانقلابيين في تدبيره.";
	
	public ArabicPipeline() throws UIMAException {
		this.preliminaryCas = JCasFactory.createJCas();
		this.cas = JCasFactory.createJCas();
	}
	
	public JCas getPreliminarCas(String sentenceId, String sentence) {
		this.preliminaryCas.reset();

		// Carry out preliminary analysis
		Analyzable content = new SimpleContent(sentenceId, sentence, ArabicAnalyzer.ARABIC_LAN);
		
		this.ae.analyze(preliminaryCas, content);
		
		// Copy data to a new CAS and use normalized text ad DocumentText
		this.cas.reset();	
		this.cas.setDocumentLanguage(ArabicAnalyzer.ARABIC_LAN);
	
		CasCopier.copyCas(this.preliminaryCas.getCas(), this.cas.getCas(), false);
	
		String normalizedText = JCasUtil.selectSingle(this.preliminaryCas, NormalizedText.class).getText();
		this.cas.setDocumentText(normalizedText);

		return this.cas;
	}
	
	@Test
	public void testArabicPipeline() throws UIMAException, IOException {
		this.ae = new Analyzer(new UIMAFilePersistence("CASes/test"));
		this.ae.addAEDesc(createEngineDescription(ArabicAnalyzer.class));
		
		JCas cas = this.getPreliminarCas("arabic-test", SAMPLE_SENTENCE);
	}
}
