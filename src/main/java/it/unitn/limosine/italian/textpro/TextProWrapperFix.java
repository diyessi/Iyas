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

			//process = new ProcessBuilder(processCmd).start();  //run it for each doc
			
			
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}
		
	}

 	
 	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
 		//prepare textpro input	
 		String txt = jcas.getDocumentText();
 		String doctxt = txt;
 		//String normtxt = stripAccentsUnicode(doctxt);

 		//run textpro

 		if (doctxt.length()==0) return;
 		
		try {
			
			String randomFileLabel = "_txp" + getRandomLabel() + "-in";
			
			//String inputFile = "/tmp/" + randomFileLabel + "-in.txt";
			//String outputFile = "/tmp/" + randomFileLabel + "-in.txp";
			
			
			File txtFile = new File("/tmp/" + randomFileLabel + ".txt");
			while (txtFile.exists()) { 
				randomFileLabel = "_txp" + getRandomLabel() + "-in";
				txtFile = new File("/tmp/" + randomFileLabel + ".txt");
			}						
			writeTextFile(doctxt, txtFile.getPath(), "UTF-8");
			
			String txpFile = "/tmp/" + randomFileLabel + ".txt.txp";			

			String fulltxpcommand = txppath + "/" + txpcommand; 
			List<String> fulltxpcmdline=new ArrayList<String>();
			fulltxpcmdline.add(fulltxpcommand);
			fulltxpcmdline.addAll(Arrays.asList(txpparams));
			
			fulltxpcmdline.add("-n");
			fulltxpcmdline.add("/tmp/" + randomFileLabel);
			fulltxpcmdline.add("/tmp/" + randomFileLabel + ".txt");			
			
			ProcessBuilder processBuilder = new ProcessBuilder(fulltxpcmdline);
			
			Map<String, String> env = processBuilder.environment();
			env.put("TEXTPRO", txppath);

			Process process = processBuilder.start();
			process.waitFor();
			//System.out.println("doctxt: " + doctxt);
			//PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()), "UTF-8"),true);
			//PrintStream writer = new PrintStream(process.getOutputStream(), true, "UTF-8");
			
			
			//writer.println(doctxt.get);
			//writer.println();
			//writer.flush();
			//writer.close();
        
			/*
			StreamGobbler outGobbler = new StreamGobbler(process.getInputStream());
			StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream());
			Thread outThread = new Thread(outGobbler);
			Thread errThread = new Thread(errGobbler);
			outThread.start();
			errThread.start();

			outThread.join();
			errThread.join();

			process.waitFor();
			
			//BARBARA: WHY DO WE NEED TO PRINT ALL THAT STUFF? JUST DEBUG
        
			for(String out : outGobbler.getOuput()) 
				System.err.println(out.trim());
        	
			List<String> output = outGobbler.getOuput();
			List<String> outputErr = errGobbler.getOuput();
        
			for(String out : outputErr) 
				System.err.println(out.trim());
        	
			*/
			List<String> output = FileUtils.readLines(
					new File(txpFile), "UTF-8");
			
 			//System.err.println("RETURNED:");
 			
			//for(String out : output) 
			//	System.err.println(out.trim());
        	
			//parse textpro output
        
			int tokenCount=0;
			int sentenceCount = 0;
			StringBuilder tokenizedSentence = new StringBuilder();
			int tokenSentCount = 0; //sentence-level counter for tokens
			Sentence sentence = new Sentence(jcas);
		
			boolean nestart=false;
			//NER ne = new NER(jcas);
			NamedEntity ne = new NamedEntity(jcas);
		
			boolean chunkstart=false;
			Chunk chunk = new Chunk(jcas);
     
			for(int lineNum = 0; lineNum < output.size(); lineNum++) {
				String out = output.get(lineNum);
				//System.out.println("TEXTPRO OUTPUT: " +out);
				String[] txpvals = out.trim().split("\t");

				
				// TOKEN
				if (lineNum < 2 || txpvals.length<7) continue;
				Token token = new Token(jcas);
			
				//token.setBegin(Integer.parseInt(txpvals[1]));
				//token.setEnd(Integer.parseInt(txpvals[2]));
				//token.setTokenId(tokenCount);
				//token.setTokenSentId(tokenSentCount);
				//token.setAnnotatorId(getClass().getCanonicalName()+":token");
			
				String text = txpvals[0];
				token.setBegin(Integer.parseInt(txpvals[1]));
				token.setEnd(Integer.parseInt(txpvals[2]));		
				
				//token.setNormalizedText(forXML(txt.substring(begin, end)));
				//token.setStanfordNormalizedText(forXML(txt.substring(begin, end)));
				
	
				//token.setNormalizedText(forXML(token.getCoveredText()));
				//			token.setStanfordNormalizedText(coreLab.value());
				//			token.setStanfordNormalizedText(forXML(token.getCoveredText())); //RE uses this as input!
				token.addToIndexes();

				tokenCount++;
				
				// LEMMA
				Lemma lemma = new Lemma(jcas);
				lemma.setValue(txpvals[5]);
				token.setLemma(lemma);
				lemma.addToIndexes();
			
				// SENTENCE
			
				if (tokenSentCount==0) {
					sentence = new Sentence(jcas);
					//sentence.setSentenceId(sentenceCount);
					//sentence.setStartToken(token);
					sentence.setBegin(token.getBegin());
					//sentence.setAnnotatorId(getClass().getCanonicalName()+":sentence");
					sentenceCount++;
					tokenizedSentence.setLength(0);
				}
				//tokenizedSentence.append(token.getNormalizedText() + " ");
				tokenSentCount++;
			
				if (txpvals[3].equals("<eos>")) {
					//sentence.setEndToken(token);
					sentence.setEnd(token.getEnd());
					//sentence.setTokenizedSentence(tokenizedSentence.toString().trim());
					sentence.addToIndexes();

					tokenSentCount = 0; //sentence-level counter for tokens
				}
			
				// LEMMA ? MORPH ?
			
				// NE
				// store finished/ongoing NE
				
				if (nestart) {
					if (txpvals[6].startsWith("I-")) {
						//ne.setEndToken(token);
						ne.setEnd(token.getEnd());					
					} else {
						//ne.addToIndexes();
						ne.addToIndexes();
						nestart=false;
					}
				}
				// start new NE
				if (txpvals[6].startsWith("B-")) {
					nestart=true;
					//ne= new NER(jcas);
					ne = new NamedEntity(jcas);
					//ne.setStartToken(token);
					ne.setBegin(token.getBegin());
					//ne.setEndToken(token);
					ne.setEnd(token.getEnd());					
					//ne.setNetag(txp2ner(txpvals[6].substring(2,txpvals[6].length())));
					ne.setValue(txp2ner(txpvals[6].substring(2,txpvals[6].length())));
					//ne.setAnnotatorId(getClass().getCanonicalName()+":ner");				
				}
				
			
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
							: txpvals[4];
					chunk.setChunkValue(chunkVal);
				}
			
				// POS
				//Pos pos = new Pos(jcas);
				POS pos = new POS(jcas);
				token.setPos(pos);
				pos.setBegin(token.getBegin());
				pos.setEnd(token.getEnd());
				pos.setPosValue(txpvals[4]);
				//pos.setPostag(txpvals[4]);
				//pos.setToken(token);
				//pos.setAnnotatorId(getClass().getCanonicalName()+":pos");				
				pos.addToIndexes();
			}
 
			// store NE finishing at the last input line
			if (nestart) 
				ne.addToIndexes();
			
			// Store Chunk finishing at the last input line
			if (chunkstart) 
				chunk.addToIndexes();
			
        } catch (Exception e) {
        	e.printStackTrace();
		}
	}
 	
 	private String stripAccentsUnicode(String str) { 
 		String nfdStr = Normalizer.normalize(str, Normalizer.Form.NFD);
 		String newStr = "";
 		for (int i = 0; i < nfdStr.length(); i++) {
 			char ch = nfdStr.charAt(i);
 			if (UCharacter.getCombiningClass(ch) == 0) {
 				newStr += ch;
 			}
 		}
 		return newStr;
 	}
 	
 	
 	public static String getRandomLabel() { 
 		String lbl = "";
 		
 		Random random = new Random();
 		for (int i = 0; i < 9; i++) {
 			lbl += random.nextInt(9) + 1;
 		}
 		lbl += ".";
 		for (int i = 0; i < 6; i++) {
 			lbl += random.nextInt(9) + 1;
 		}
 		return lbl;
 	}
 	
 	private static void writeTextFile(String text, String filename, String encoding) { 
 		assert text != null;
 		assert filename != null;
 		assert encoding != null;
 		
 		File file = new File(filename);
		while (file.exists()) { 
			file = new File(filename);
		}
		
		try {
			FileUtils.write(file, text, encoding);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		try (Writer writer = 
				new BufferedWriter(
						new OutputStreamWriter(
								new FileOutputStream(filename), encoding))) {
			writer.write(text);		
			writer.flush();
		*
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
 	}
		
 	
 	public static void main(String[] args) throws Exception { 
 		//String lbl = getRandomLabel();
 		
 		//String randomFileLabel = "/tmp/" + "_txp" + getRandomLabel() + "-in.txt";
 		String randomFileLabel = "/tmp/_txp493113931.243137-in.txt";
 		//System.out.println("Got random filename: " + randomFileLabel);
 		
 		//writeTextFile("Più bello.", randomFileLabel, "UTF-8");
 		
 		String text = " Più Quagliarella di Llorente";
 		Analyzable content = new SimpleContent("0", text);

		AnalysisEngineDescription desc = createEngineDescription("desc/Limosine/TextProFixAllInOneDescriptor");
 		Analyzer analyzer = new Analyzer();
 		
 		analyzer.addAEDesc(desc);
 		JCas cas = JCasFactory.createJCas();
 		analyzer.analyze(cas, content);
 		//System.out.println(lbl); 				
 		
 		System.out.println("size: " + text.length());
 		Collection<Token> tokens = JCasUtil.select(cas, Token.class);
 		System.out.println("tokens.size():" + tokens.size());
 		
 		Collection<NamedEntity> nes = JCasUtil.select(cas, NamedEntity.class);
 		System.out.println("nes.size(): " + nes.size());
 		/*
 		for (Token token : JCasUtil.select(cas, Token.class)) { 
 			System.out.println(token.getCoveredText() + ", tokenstart: " + token.getBegin() + ", tokenend: " + token.getEnd());
 		}
 		*/
 	}
}
