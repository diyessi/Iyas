package it.unitn.limosine.italian.syntax.constituency;
import it.unitn.limosine.util.SharedModel;
import it.unitn.limosine.util.StreamGobbler;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;


public class BerkeleyWrapperFix extends JCasAnnotator_ImplBase {

 	private String berkeleyitpath;
	//private String berkeleycommand="bin/berkeleyparser-runio.sh";
 	private String berkeleycommand="berkeleyparser-runio.pl";
 	
 	private String fullberkitcommand;
 	
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
			
			fullberkitcommand = berkeleyitpath + "/" + berkeleycommand;
			
			getContext().getLogger().log(Level.INFO, fullberkitcommand);
					
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}	
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		//needs input from tokenizer
		
		 FSIndex<Annotation> sentencesIndex = cas.getAnnotationIndex(Sentence.type);
		 
		 //iterate over all sentences
		 
		 Iterator<Annotation> sentenceIterator = sentencesIndex.iterator(); 

		 // this will be superslow for a moment
		 // ToDo: process the whole doc at one go

		 //StringBuilder tree = new StringBuilder("(Start ");
		 StringBuilder tree = new StringBuilder("(ROOT ");
		 
		 //String doctxt = cas.getDocumentText();
		 //Constituent start = new Constituent(cas, 0, doctxt.length());
		 //start.setConstituentType("Start");
		 
		 int startTokenNum = 0;
		 int tokenNum = 0;
			
		 while (sentenceIterator.hasNext()) {
			 Sentence sentence = (Sentence) sentenceIterator.next();
		 	
			 /*
			 int sentBegin = sentence.getBegin();
			 int sentEnd = sentence.getEnd();
			 System.out.println("sentence(begin: " + sentBegin + ", end: " + sentEnd + "): " + doctxt.substring(sentBegin, sentEnd));
			 */
			 
			//get tokens+pos of sentence, prepare txp input
			 //List<AnnotationFS> myPos = JCasUtility.selectCovered(cas, POS.class, sentence);
			 List<AnnotationFS> myToks = JCasUtility.selectCovered(cas, Token.class, sentence);
		  
		 	//List<Annotation> myPos = JCasUtility.
		 	//Collection<Token> myToks = JCasUtil.select(cas, Token.class);
		 	//Collection<POS> myPos = JCasUtil.select(cas, POS.class);
	
			 System.out.println("sent: " + sentence.getCoveredText());
			 
			 
			 try {

				 //DUMP 1 sentence in the TXP format
				 
					//String fullberkitcommand = berkeleyitpath + "/" + berkeleycommand;
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
						 
						 tokenNum++;
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
				        /*
				        List<String> outputErr = errGobbler.getOuput();
				        
				        
				        for(String out : outputErr) {
				        	System.err.println(out.trim());
				        }
				        */
				        
				        for(String out : output) {
				        	String subtree = out.trim();
				        	
				        	// Strip spaces around brackets
				        	subtree = subtree.replaceAll("([()])\\s+([()])", "$1$2");
				        	
				        	System.out.println("BerkeleyWrapper: subtree: " + subtree.toString());
				        	//if (subtree.startsWith("(Start (S")) {
				        		//subtree = subtree.replaceAll("\\(Start", "\\(ROOT");
				        	if (subtree.startsWith("((Start (S ")) {
				        		// if parse already as S node, replace (Start .*) node with (ROOT .*) one
				        		subtree = subtree.replaceAll("Start ", "ROOT ");
				        	} else if (subtree.startsWith("((Start ")) {
				        		// if parse does not have S node, add it
				        		subtree = subtree.replaceAll("\\(\\(Start", "\\(ROOT \\(S") + ")";
				        	} else if (subtree.equals("(())")){
				        		// if parse fais, add trailing tokens
				        		subtree = "(ROOT (S ";
				        		for (AnnotationFS t : myToks) {
				        			Token tok = (Token) t;
				        			subtree += "(" + tok.getPos().getPosValue() + " " + tok.getCoveredText() + ")";
				        		}
				        		subtree += "))";
				        	}
				        	/*
				        	} else {
				        		subtree = subtree.replaceAll("\\(Start", "\\(ROOT (S") + ")";
				        	}
				        	*/
				        	//subtree = subtree.substring(1, subtree.length() - 1);
				        	
				        	System.out.println("BerkeleyWrapper: subtree: " + subtree.toString());

						 	StanfordTreeConstituentsProvider.buildConstituents(cas, subtree.toString(), startTokenNum);
						 							 	
				        	//System.out.println("subtree: " + subtree.toString());
				          	tree.append(subtree.substring(6, subtree.length() - 1));
				        					        
				        	/*
				        	ConstituencyTree pennTree = new ConstituencyTree(cas);
				        	pennTree.setBegin(sentence.getBegin());
				        	pennTree.setEnd(sentence.getEnd());
				        	pennTree.setRawParse(out.trim());
				        	pennTree.setAnnotatorId(getClass().getCanonicalName());
				        	
				        	pennTree.setSentence(sentence);
				        	pennTree.addToIndexes();
				        	 */   
				          	
				          	startTokenNum = tokenNum;
				     }				        
				} catch (Exception e) {
					e.printStackTrace();
				}	 	
			 }

		 	tree.append(")");
		 	String treeStr = tree.toString();
		 	treeStr = treeStr.replaceAll(" ([^()]+)\\)",  " \\($1\\)\\)");
		 	//treeStr = treeStr.replaceAll(" ([^]]*)\\)", " \\($1\\)\\)");
		 	System.out.println("BerkeleyWrapper:  "  + treeStr);
		 	TokenTree constituencyTree = RichTree.getConstituencyTree(cas);
		 	
		 	TreeSerializer ts = new TreeSerializer();
		 	System.out.println("constituencyTree: " + ts.serializeTree(constituencyTree, TokenTree.OUTPUT_PAR_TOKEN));

		}
	
	}



