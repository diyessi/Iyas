package it.unitn.limosine.italian.textpro;

import static org.junit.Assert.*;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.util.Level;

import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;

import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.ibm.icu.lang.UCharacter;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;

public class TextProWrapperFixTest {
	
	@Test
	public void testSsplitter() throws UIMAException, IOException {
		String text2 = "";
		text2 += "Quagliarella favorito su Llorente. ";
		text2 += "Juve, la sfida per affiancare Tevez. ";
		text2 += "Più Quagliarella di Llorente. ";
		text2 += "Il borsino dell’attacco bianconero a tre giorni dal Derby d’Italia segna in deciso rialzo le quotazioni della punta napoletana nel ballottaggio per un posto da titolare con Carlitos Tevez.";
		
		Analyzable content = new SimpleContent("0", text2);
		Analyzer analyzer = new Analyzer();
		
		AnalysisEngineDescription desc = createEngineDescription("desc/Limosine/TextProAllInOneDescriptor");
		analyzer.addAEDesc(desc);

		JCas cas = JCasFactory.createJCas();
		analyzer.analyze(cas, content);
		
		System.out.println("text length: " + text2.length());
		
		for (Sentence sent : JCasUtil.select(cas, Sentence.class)) {
			System.out.println(sent.getCoveredText());
		}
	}

	
	@Test
	public void testPOS() throws UIMAException, IOException {
		String text3 = "Bernardo Magnini lavora alla Fondazione Kessler di Povo, vicino a Trento";
		
		Analyzable content = new SimpleContent("0", text3);
		AnalysisEngineDescription desc = createEngineDescription("desc/Limosine/TextProAllInOneDescriptor");
		Analyzer analyzer = new Analyzer();
		
		analyzer.addAEDesc(desc);
		JCas cas = JCasFactory.createJCas();
		analyzer.analyze(cas, content);
		
		List<String> poss  = new ArrayList<>();
		for (Pos pos : JCasUtil.select(cas, Pos.class)) {
			poss.add(pos.getCoveredText() + "/" + pos.getPostag());
		}
		
		System.out.println(Joiner.on(' ').join(poss));
	}

}
