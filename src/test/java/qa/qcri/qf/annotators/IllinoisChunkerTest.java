package qa.qcri.qf.annotators;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;

public class IllinoisChunkerTest {

	@Test
	public void test() throws Exception {
		String text = "Jack and Jill went up the hill to fetch a pail of water";
		//String text = "Bernardo Magnini works at Kessler Foundation in Povo, near Trento.";
		
		AnalysisEngineDescription segmenter = createEngineDescription(OpenNlpSegmenter.class);
		AnalysisEngineDescription ptagger = createEngineDescription(OpenNlpPosTagger.class);
		AnalysisEngineDescription chunker = createEngineDescription(IllinoisChunker.class);
		
		AnalysisEngineDescription aggregate = createEngineDescription(segmenter, ptagger, chunker);
		AnalysisEngine engine = createEngine(aggregate);
		
		JCas jcas = engine.newJCas();
		jcas.setDocumentLanguage("en");
		jcas.setDocumentText(text);
		engine.process(jcas);
		
		List<String> chunks = new ArrayList<>();
		for (Chunk chunk : JCasUtil.select(jcas, Chunk.class)) {
			chunks.add("[" + chunk.getChunkValue() + " " + chunk.getCoveredText() + " ]");
			/*
			for (POS pos : JCasUtil.selectCovered(jcas, POS.class, chunk)) {
				System.out.print(pos.getCoveredText() + "/" + pos.getPosValue() + " ");
			}
			System.out.println();
			*/
		}
		chunks.add(".");
		String expected = "[NP Jack and Jill ] [VP went ] [ADVP up ] [NP the hill ] [VP to fetch ] [NP a pail ] [PP of ] [NP water ] .";
		String actual = Joiner.on(" ").join(chunks);
		System.out.println(actual);
		
		assertEquals(actual, expected);
	}

}
