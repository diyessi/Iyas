package it.unitn.limosine.italian.textpro;


import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.util.SharedModel;
import it.unitn.limosine.util.StreamGobbler;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.CharacterIterator;
import java.text.Normalizer;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.fop.area.inline.Character;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.uimafit.descriptor.ConfigurationParameter;

import com.ibm.icu.lang.UCharacter;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import it.unitn.limosine.types.pos.Pos;


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
 		String doctxt = stripAccentsUnicode(txt);
 		//String normtxt = stripAccentsUnicode(doctxt);

 		//run textpro

 		if (doctxt.length()==0) return;
 		
		try {

			String fulltxpcommand = txppath + "/" + txpcommand; 
			List<String> fulltxpcmdline=new ArrayList<String>();
			fulltxpcmdline.add(fulltxpcommand);
			fulltxpcmdline.addAll(Arrays.asList(txpparams));
			ProcessBuilder processBuilder = new ProcessBuilder(fulltxpcmdline);
			
			Map<String, String> env = processBuilder.environment();
			env.put("TEXTPRO", txppath);

			Process process = processBuilder.start();
		
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream())),true);
			//PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()), StandardCharsets.UTF_8), true);
			writer.println(doctxt);
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

			//BARBARA: WHY DO WE NEED TO PRINT ALL THAT STUFF? JUST DEBUG
        
			for(String out : outGobbler.getOuput()) 
				System.err.println(out.trim());
        
			List<String> output = outGobbler.getOuput();
			List<String> outputErr = errGobbler.getOuput();
        
			for(String out : outputErr) 
				System.err.println(out.trim());
        
			System.err.println("RETURNED:");
			for(String out : output) 
				System.err.println(out.trim());
        
			//parse textpro output
        
			int tokenCount=0;
			int sentenceCount = 0;
			StringBuilder tokenizedSentence = new StringBuilder();
			int tokenSentCount = 0; //sentence-level counter for tokens
			Sentence sentence = new Sentence(jcas);
		
			boolean nestart=false;
			NER ne = new NER(jcas);
		
			boolean chunkstart=false;
			Chunk chunk = new Chunk(jcas);
     
			int idx = 0;
			for(String out : output) {
				System.out.println("TEXTPRO OUTPUT: " +out);
				String[] txpvals = out.trim().split("\t");

				// TOKEN
				if (txpvals.length<7) continue;
				Token token = new Token(jcas);
			
				//token.setBegin(Integer.parseInt(txpvals[1]));
				//token.setEnd(Integer.parseInt(txpvals[2]));
				token.setTokenId(tokenCount);
				token.setTokenSentId(tokenSentCount);
				token.setAnnotatorId(getClass().getCanonicalName()+":token");
			
				String text = txpvals[0];
				int begin = doctxt.indexOf(text, idx);
				int end = begin + text.length();
				token.setBegin(begin);
				token.setEnd(end);
				idx = end;
				token.setNormalizedText(forXML(txt.substring(begin, end)));
				token.setStanfordNormalizedText(forXML(txt.substring(begin, end)));
	
				//token.setNormalizedText(forXML(token.getCoveredText()));
				//			token.setStanfordNormalizedText(coreLab.value());
				//			token.setStanfordNormalizedText(forXML(token.getCoveredText())); //RE uses this as input!
				token.addToIndexes();

				tokenCount++;
			
				// SENTENCE
			
				if (tokenSentCount==0) {
					sentence = new Sentence(jcas);
					sentence.setSentenceId(sentenceCount);
					sentence.setStartToken(token);
					sentence.setBegin(token.getBegin());
					sentence.setAnnotatorId(getClass().getCanonicalName()+":sentence");
					sentenceCount++;
					tokenizedSentence.setLength(0);
				}
				tokenizedSentence.append(token.getNormalizedText() + " ");
				tokenSentCount++;
			
				if (txpvals[3].equals("<eos>")) {
					sentence.setEndToken(token);
					sentence.setEnd(token.getEnd());
					sentence.setTokenizedSentence(tokenizedSentence.toString().trim());
					sentence.addToIndexes();

					tokenSentCount = 0; //sentence-level counter for tokens
				}
			
				// LEMMA ? MORPH ?
			
				// NE
				// store finished/ongoing NE
				if (nestart) {
					if (txpvals[6].startsWith("I-")) {
						ne.setEndToken(token);
						ne.setEnd(token.getEnd());					
					} else {
						ne.addToIndexes();
						nestart=false;
					}
				}
				// start new NE
				if (txpvals[6].startsWith("B-")) {
					nestart=true;
					ne= new NER(jcas);
					ne.setStartToken(token);
					ne.setBegin(token.getBegin());
					ne.setEndToken(token);
					ne.setEnd(token.getEnd());					
					ne.setNetag(txp2ner(txpvals[6].substring(2,txpvals[6].length())));
					ne.setAnnotatorId(getClass().getCanonicalName()+":ner");				
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
				Pos pos = new Pos(jcas);
				pos.setBegin(token.getBegin());
				pos.setEnd(token.getEnd());
				pos.setPostag(txpvals[4]);
				pos.setToken(token);
				pos.setAnnotatorId(getClass().getCanonicalName()+":pos");				
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
		
}
