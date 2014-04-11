package it.unitn.limosine.italian.syntax.constituency;
import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.SharedModel;
import it.unitn.limosine.util.StreamGobbler;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;









//import it.unitn.limosine.types.pos.Pos;
//import it.unitn.limosine.types.segmentation.Sentence;
//import it.unitn.limosine.types.segmentation.Token;
//import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import edu.stanford.nlp.trees.ConstituentFactory;


public class BerkeleyWrapper extends JCasAnnotator_ImplBase {

 	private String berkeleyitpath;
	private String berkeleycommand="bin/berkeleyparser-runio.sh";
 	
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		try {

			
			SharedModel sharedModel = (SharedModel) getContext().getResourceObject("BerkeleyIt");

			getContext().getLogger().log(Level.INFO,
					"Launching Berkeley-IT...");

			berkeleyitpath = sharedModel.getPath();

			getContext().getLogger().log(Level.INFO, berkeleyitpath);
			
			
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}	
	}
	
	/* 
	 * replace '( (' with '(('
	 * replace ') )' with '))'
	 * replace ') (' with ')('
	 */
	private String stripSpacesAroundBrackets(String str) {
		assert str != null;
		
		String result = str;
		return result.replaceAll("([()])\\s+([()])", "$1$2");
		/*
		result = result.replaceAll("\\( \\(", "\\(\\(");
        result = result.replaceAll("\\) \\)", "\\)\\)");
        result = result.replaceAll("\\) \\(", "\\)\\(");
		
        return result;
        */
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		//needs input from tokenizer
		
		 FSIndex<Annotation> sentencesIndex = cas.getAnnotationIndex(Sentence.type);
		 
		 //iterate over all sentences
		 
		 Iterator<Annotation> sentenceIterator = sentencesIndex.iterator(); 

		 // this will be superslow for a moment
		 // ToDo: process the whole doc at one go

		 while (sentenceIterator.hasNext()) {
			 Sentence sentence = (Sentence) sentenceIterator.next();
			 
			//get tokens+pos of sentence, prepare txp input
			// List<AnnotationFS> myPos = JCasUtility.selectCovered(cas, POS.class, sentence);
			 List<AnnotationFS> myToks = JCasUtility.selectCovered(cas, Token.class, sentence);
	
			 
			 
			 try {

				 //DUMP 1 sentence in the TXP format
				 
					String fullberkitcommand = berkeleyitpath + "/" + berkeleycommand; 	
					/*
					List<String> fulltxpcmdline=new ArrayList<String>();
					fulltxpcmdline.add(fulltxpcommand);
					fulltxpcmdline.addAll(Arrays.asList(txpparams));
					ProcessBuilder processBuilder = new ProcessBuilder(fulltxpcmdline);
					
					Map<String, String> env = processBuilder.environment();
					env.put("TEXTPRO", txppath);
*/
					
					ProcessBuilder processBuilder = new ProcessBuilder(fullberkitcommand);
					Map<String, String> env = processBuilder.environment();
					env.put("CLASSPATH", "$CLASSPATH:"+berkeleyitpath+"/berkeleyParser.jar");
					Process process = processBuilder.start();
				
					PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream())),true);

					/*
					 for (AnnotationFS p : myPos) {
						 POS pos = (POS)p;
						 Token tok;
						 
						 //Token tok = pos.getToken();
						 Token tok = JCasUtil.selectS
						 writer.println(tok.getN);
						 //writer.println(tok.getNormalizedText() + "\t" + pos.getPostag());
					 }
					 */
					
					 for (AnnotationFS t : myToks) {
						 Token tok = (Token)t;
						 
						 POS pos = tok.getPos();
						 writer.println(tok.getCoveredText() + "\t" + pos.getPosValue());
					 }
					 writer.println();
					 
				
					 writer.flush();
					 writer.close();
		        // RUN PARSER + CONVERTER TO ENG TAGS
					 //ToDo: run 2 processes separately
		 
				        StreamGobbler outGobbler = new StreamGobbler(process.getInputStream());
				        StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream());
				        Thread outThread = new Thread(outGobbler);
				        Thread errThread = new Thread(errGobbler);
				        outThread.start();
				        errThread.start();

				        outThread.join();
				        errThread.join();

				        process.waitFor();


				        List<String> output = outGobbler.getOuput();
				        List<String> outputErr = errGobbler.getOuput();
				        
				        for(String out : outputErr) 
				        	System.err.println(out.trim());
				        
				        for(String out : output) {
				        	String constituencyTree = out.trim();
				        	
				        /*
				        constituencyTree = constituencyTree.replaceAll("\\( \\(", "\\(\\(");
				        constituencyTree = constituencyTree.replaceAll("\\) \\)", "\\)\\)");
				        constituencyTree = constituencyTree.replaceAll("\\) \\(", "\\)\\(");
				        constituencyTree = constituencyTree.substring(1, constituencyTree.length() - 1);
				        */
				        	
				        // Strip spaces around brackets
				        constituencyTree = constituencyTree.replaceAll("([()])\\s+([()])", "$1$2");
				        constituencyTree = constituencyTree.replaceAll("\\(Start", "\\(TOP");
				        constituencyTree = constituencyTree.substring(1, constituencyTree.length() - 1);
				        			        		
				        //ConstituencyUtils.addConstituentsToIndexes(constituencyTree, cas);
				        ConstituencyFactory.buildConstituents(cas, constituencyTree);
				        
				        /*
				        ConstituencyTree pennTree = new ConstituencyTree(cas);
				        pennTree.setBegin(sentence.getBegin());
				        pennTree.setEnd(sentence.getEnd());
				        pennTree.setRawParse(out.trim());
				        pennTree.setAnnotatorId(getClass().getCanonicalName());
				        	
				        pennTree.setSentence(sentence);
				        pennTree.addToIndexes();
				        */   	
				     }
				} catch (Exception e) {
					e.printStackTrace();
				}
			 
			 }
		}
	}



