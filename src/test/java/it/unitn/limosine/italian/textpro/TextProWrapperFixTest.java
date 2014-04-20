package it.unitn.limosine.italian.textpro;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;

public class TextProWrapperFixTest {
	
	@Test
	public void testSsplitter() throws UIMAException, IOException {
		String text2 = "";
		text2 += "Quagliarella favorito su Llorente. ";
		text2 += "Juve, la sfida per affiancare Tevez. ";
		text2 += "Più Quagliarella di Llorente. ";
		text2 += "Il borsino dell’attacco bianconero a tre giorni dal Derby d’Italia segna in deciso rialzo le quotazioni della punta napoletana nel ballottaggio per un posto da titolare con Carlitos Tevez.";
		
		byte[] bytes = text2.getBytes();
		text2 = new String(bytes, "UTF-8");
		
		Analyzable content = new SimpleContent("0", text2);
		Analyzer analyzer = new Analyzer();
		
		analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor")));

		JCas cas = JCasFactory.createJCas();
		analyzer.analyze(cas, content);
		
		System.out.println("text length: " + text2.length());
		
		for (Sentence sent : JCasUtil.select(cas, Sentence.class)) {
			System.out.println(sent.getCoveredText());
		}
	}
	
	
	@Test
	public void testChunker() throws IOException, UIMAException {
		String text3 = "Bernardo Magnini lavora alla Fondazione Kessler di Povo, vicino a Trento";

		Analyzable content = new SimpleContent("0", text3);
		Analyzer analyzer = new Analyzer();
		
		analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor")));
		
		JCas cas = JCasFactory.createJCas();
		analyzer.analyze(cas, content);
		
		Collection<Chunk> chunks = JCasUtil.select(cas, Chunk.class);
		System.out.println("#chunks: " + chunks.size());
		for (Chunk chunk : chunks) {
			System.out.println("[" + chunk.getChunkValue() + " " + chunk.getCoveredText() + " ]");
		}
	}

	
	@Test
	public void testPOS() throws UIMAException, IOException {
		String text3 = "Bernardo Magnini lavora alla Fondazione Kessler di Povo, vicino a Trento";
		
		Analyzable content = new SimpleContent("0", text3);
		
		Analyzer analyzer = new Analyzer();		
		analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor")));
		
		JCas cas = JCasFactory.createJCas();
		analyzer.analyze(cas, content);
		
		List<String> poss  = new ArrayList<>();
		for (POS pos : JCasUtil.select(cas, POS.class)) {
			poss.add(pos.getCoveredText() + "/" + pos.getPosValue());
		}
		
		System.out.println(Joiner.on(' ').join(poss));
	}
	
	@Test
	public void buildPosChunkTree() throws UIMAException, IOException {
		//String text3 = "Bernardo Magnini lavora alla Fondazione Kessler di Povo, vicino a Trento";
		String text3 = "Quagliarella favorito su Llorente.";
		
		Analyzable content = new SimpleContent("0", text3);
		
		Analyzer analyzer = new Analyzer();	
		analyzer.addAE(AnalysisEngineFactory.createEngine(
				createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor")));
		
		JCas cas = JCasFactory.createJCas();
		analyzer.analyze(cas, content);
		
		TokenTree tree = RichTree.getPosChunkTree(cas);
		TreeSerializer ts = new TreeSerializer();
		System.out.println(ts.serializeTree(tree, RichNode.OUTPUT_PAR_LEMMA));
		System.out.println(ts.serializeTree(tree, RichNode.OUTPUT_PAR_TOKEN));
		
	}

}
