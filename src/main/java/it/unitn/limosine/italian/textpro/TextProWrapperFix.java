package it.unitn.limosine.italian.textpro;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

//import it.unitn.limosine.types.segmentation.Token;

import it.unitn.limosine.types.ner.NER;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
//import it.unitn.limosine.types.segmentation.Sentence;
//import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.util.SharedModel;
import it.unitn.limosine.util.StreamGobbler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.CharacterIterator;
import java.text.Normalizer;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.input.InputRequest;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;

import com.ibm.icu.lang.UCharacter;
import com.sleepycat.je.log.FileReader;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;


public class TextProWrapperFix extends JCasAnnotator_ImplBase {

 	private String txpcommand="textpro.pl";
//ToDo: change with a proper textpro command (using resources and environment variables)
 	private String[] txpparams={"-l", "ita", "-c", "token+tokenstart+tokenend+sentence+pos+lemma+entity+chunk"};
 	private String txppath;
 	String txpencoding = "ISO-8859-1"; // "Cp1252" = Windows Latin 1
 	
 	
 	private static String txp2ner(String txpner) {
 		if (txpner.equals("PER")) return "PERSON";
 		if (txpner.equals("ORG")) return "ORGANIZATION";
		if (txpner.equals("LOC")) return "LOCATION";
		if (txpner.equals("GPE")) return "LOCATION";
		return "ENTITY";
 	}
 	
	  private static String forXML(String aText){
		    final StringBuilder result = new StringBuilder();
		    final StringCharacterIterator iterator = new StringCharacterIterator(aText);
		    char character =  iterator.current();
		    while (character != CharacterIterator.DONE ){
		      if (character == '<') {
		        result.append("&lt;");
		      }
		      else if (character == '>') {
		        result.append("&gt;");
		      }
		      else if (character == '\"') {
		        result.append("&quot;");
		      }
		      else if (character == '\'') {
		        result.append("&#039;");
		      }
		      else if (character == '&') {
			         result.append("&amp;");
			      }
		      
		      else if (character == '(') {
			         result.append("-LRB-");
			      }
		      else if (character == ')') {
			         result.append("-RRB-");
			      }
			      
		      else {
		        //the char is not a special one
		        //add it to the result as is
		        result.append(character);
		      }
		      character = iterator.next();
		    }
		    return result.toString();
		  }

	  @Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		try {

			
			SharedModel sharedModel = (SharedModel) getContext().getResourceObject("RunTextPro");

			getContext().getLogger().log(Level.INFO,
					"Launching TextPro...");

			txppath = sharedModel.getPath();

			getContext().getLogger().log(Level.INFO, txppath);

			
			
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}
		
	}

 	
 	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
 		//prepare textpro input	
 		String doctxt_utf=jcas.getDocumentText();
 		
 		//run textpro
 		
 		if (doctxt_utf.length()==0) return;
 		
 		try {
 			
 			
 			
 			String fulltxpcommand = txppath + "/" + txpcommand;
 			List<String> fulltxpcmdline=new ArrayList<String>();
 			fulltxpcmdline.add(fulltxpcommand);
 			fulltxpcmdline.addAll(Arrays.asList(txpparams));
 			ProcessBuilder processBuilder = new ProcessBuilder(fulltxpcmdline);
 			
 			Map<String, String> env = processBuilder.environment();
 			env.put("TEXTPRO", txppath);
 			
 			Process process = processBuilder.start();
 			
 			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()), txpencoding);
 			PrintWriter writer = new PrintWriter(outputStreamWriter, true);
 			
 			writer.println(doctxt_utf);
 			
 			writer.println();
 			writer.flush();
 			writer.close();
 			
			
 			StreamGobbler outGobbler = new StreamGobbler(process.getInputStream());
 			StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream());
			Thread outThread = new Thread(outGobbler);
			Thread errThread = new Thread(errGobbler);
			outThread.start();
			errThread.start();
			
			outThread.join();
			errThread.join();
			
			process.waitFor();
			
			
			for (String out : outGobbler.getOuput())
				System.err.println(out.trim());
			
			List<String> output = outGobbler.getOuput();
			List<String> outputErr = errGobbler.getOuput();
			

			for (String out : outputErr) 
				System.err.println(out.trim());
			
			System.err.println("RETURNED:");
			for (String out : output) 
				System.err.println(out.trim());
			
			//parse textpro output
			StringBuilder tokenizedSentence = new StringBuilder();
			int tokenSentCount = 0; //sentence-level counter for tokens
			Sentence sentence = new Sentence(jcas);
		
			boolean nestart=false;
			NamedEntity ne = new NamedEntity(jcas);
			
			boolean chunkstart=false;
			Chunk chunk = new Chunk(jcas);
			
			for(String out : output) { 
				System.out.println("TEXTPRO OUTPUT: " + out);
				String[] txpvals = out.trim().split("\t");
			
				// TOKEN
				if (txpvals.length<7) continue;
				Token token = new Token(jcas);
				token.setBegin(Integer.parseInt(txpvals[1]));
				token.setEnd(Integer.parseInt(txpvals[2]));
				token.addToIndexes();
				
			
				// SENTENCE
			
				if (tokenSentCount==0) {
					sentence = new Sentence(jcas);
					sentence.setBegin(token.getBegin());
					tokenizedSentence.setLength(0);
				}
				tokenSentCount++;
			
				if (txpvals[3].equals("<eos>")) {
					sentence.setEnd(token.getEnd());
					sentence.addToIndexes();

					tokenSentCount = 0; //sentence-level counter for tokens
				}
			
				// LEMMA ? MORPH ?
			
				// NE
				// store finished/ongoing NE
				if (nestart) {
					if (txpvals[6].startsWith("I-")) {
						ne.setEnd(token.getEnd());					
					} else {
						ne.addToIndexes();
						nestart=false;
					}
				}
				// start new NE
				if (txpvals[6].startsWith("B-")) {
					nestart=true;
					ne = new NamedEntity(jcas);
					ne.setBegin(token.getBegin());
					ne.setEnd(token.getEnd());					
					ne.setValue(txp2ner(txpvals[6].substring(2,txpvals[6].length())));
				}	
				
				// LEMMA ? MORPH ?
			
				if (chunkstart) {
					if (txpvals[7].startsWith("I-")) {
						chunk.setEnd(token.getEnd());
					} else {
						chunk.addToIndexes();
						chunkstart=false;
					}
				}
				// start new chunk
				if (txpvals[7].startsWith("B-") || txpvals[7].equals("O")) {
					chunkstart=true;
					chunk = new Chunk(jcas);
					chunk.setBegin(token.getBegin());
					chunk.setEnd(token.getEnd());
					String chunkVal = txpvals[7].startsWith("B-")
							? txpvals[7].substring(2)
							: "O";	
							//: txpvals[4];
					chunk.setChunkValue(chunkVal);
				}
			
				// POS
				POS pos = new POS(jcas);
				token.setPos(pos);
				pos.setBegin(token.getBegin());
				pos.setEnd(token.getEnd());
				pos.setPosValue(txpvals[4]);			
				pos.addToIndexes();
				
				// lemma
				Lemma lem = new Lemma(jcas);
				lem.setValue(forXML(txpvals[5]));
				lem.setBegin(token.getBegin());
				lem.setEnd(token.getEnd());
				lem.addToIndexes();
				token.setLemma(lem);
			}
 
			// store NE finishing at the last input line
			if (nestart) 
				ne.addToIndexes();
			
			// Store chunk finishing at the last input line
			if (chunkstart) 
				chunk.addToIndexes();
			
        } catch (Exception e) {
        	e.printStackTrace();
		}
	}
}
