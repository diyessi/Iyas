package it.unitn.limosine.italian.syntax.constituency;
import it.unitn.limosine.util.SharedModel;
import it.unitn.limosine.util.StreamGobbler;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;


public class BerkeleyWrapper extends JCasAnnotator_ImplBase {

 	private String berkeleyitpath;
 	private String berkeleycommand="tools/berkeleyparser-runio.sh";
 	
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

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		Collection<Token> tokens = JCasUtil.select(cas, Token.class);
		
		System.out.println("tokens num: " + tokens.size());
		
		//needs input from tokenizer
		 Collection<Sentence> sentences = JCasUtil.select(cas, Sentence.class);
		 
		 System.out.println("sentences num: " + sentences.size());
		
		 StringBuilder sb = new StringBuilder("(Start "); 
		
		 FSIndex<Annotation> sentencesIndex = cas.getAnnotationIndex(Sentence.type);
		 
		 //iterate over all sentences
		 
		 Iterator<Annotation> sentenceIterator = sentencesIndex.iterator(); 

		 // this will be superslow for a moment
		 // ToDo: process the whole doc at one go
		 
		 String doctxt = cas.getDocumentText();

		 System.out.print("Processing sentences... ");
		 int sentNum = 0;
		 while (sentenceIterator.hasNext()) {
			 System.out.print(sentNum++ + " ");
			 Sentence sentence = (Sentence) sentenceIterator.next();
			 
			 int sentStart = sentence.getBegin();
			 int sentEnd = sentence.getEnd();
			 
			 System.out.println("BerkeleyWrapper: sent: " + doctxt.substring(sentStart, sentEnd));
			 
			//get tokens+pos of sentence, prepare txp input
			 List<AnnotationFS> myPos = JCasUtility.selectCovered(cas, Pos.class, sentence);
	
			 
			 
			 try {

				 //DUMP 1 sentence in the TXP format
				 
					//String fullberkitcommand = berkeleyitpath + "/" + berkeleycommand;
				 	String fullberkitcommand = berkeleycommand;
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

					 for (AnnotationFS p : myPos) {
						 Pos pos = (Pos)p;
						 Token tok = pos.getToken();
						 writer.println(tok.getNormalizedText() + "\t" + pos.getPostag());
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
				        	String subtree = out.trim();
				        	sb.append(subtree);
				        	
				        	/*
				        	ConstituencyTree pennTree = new ConstituencyTree(cas);
				        	pennTree.setBegin(sentence.getBegin());
				        	pennTree.setEnd(sentence.getEnd());
				        	pennTree.setRawParse(out.trim());
				        	pennTree.setAnnotatorId(getClass().getCanonicalName());
				        	pennTree.setSentence(sentence);
				        	pennTree.addToIndexes();
				        	*/
				        	subtree = subtree.replaceAll("([()])\\s+([()])", "$1$2");
				        	subtree = subtree.replaceAll("\\(Start", "\\(ROOT");
				        	subtree = subtree.substring(1, subtree.length() - 1);
				        	
				        	//StanfordTreeConstituentsProvider.buildConstituents(cas, subtree.toString());
				        	
				        	System.out.println("BerkeleyWrapper: subtree: " + subtree.toString());
				        }
				} catch (Exception e) {
					e.printStackTrace();
				}
			 
			 }
		 
		 	 sb.append(")");
		 	 System.out.println("BerkeleyWrapper: tree: " + sb.toString());
		 	
		 	 /*
		 	 TokenTree tree = RichTree.getConstituencyTree(cas);
		 	 TreeSerializer ts = new TreeSerializer();
		 	 System.out.println(ts.serializeTree(tree, TokenTree.OUTPUT_PAR_TOKEN));
		 	 */
		}
	}



