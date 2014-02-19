package it.unitn.limosine.syntax.dependency;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Lemma;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.CoNLL2008DependencyTree;
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

import se.lth.cs.nlp.depsrl.format.CoNLL2008Format;
import se.lth.cs.nlp.depsrl.format.PAStructure;
import se.lth.cs.nlp.nlputils.core.Triple;
import se.lth.cs.nlp.nlputils.depgraph.DepGraph;
import se.lth.cs.nlp.nlputils.depgraph.DepNode;

public class LTHParserFast extends JCasAnnotator_ImplBase {

	public static final String MESSAGE_DIGEST = "it.unitn.limosine.internationalization.messages.LTHParserAnnotator_Messages";
	
	//Process process; //the interface to the parser
	private String parserCommand;
	
	private boolean debug = false;
	
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext); //don't accidentally remove this! (then no context is here...)
		
		try {
			
			// init lth parser
			SharedModel sharedModel = (SharedModel) getContext().getResourceObject("RunLTHParser");

			getContext().getLogger().log(Level.INFO, "Starting LTHParser parser...");
			
			String parserCmd = sharedModel.getPath();
		
			getContext().getLogger().log(Level.INFO, parserCmd);
			
			//process = new ProcessBuilder(parserCmd).start(); //use ProcessBuilder rather than RunTime.exec()
			parserCommand = parserCmd;
			
			if (debug) {
			Process process = new ProcessBuilder(parserCommand).start();
			
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream())),true);
			
			writer.println("I am testing");
			writer.println("This is Mr. Blue.");
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
 
            List<String> output = outGobbler.getOuput();
			
        	StringBuilder sb = new StringBuilder();
			for (String out : output) {
				sb.append(out);
			}
            System.out.println(sb.toString());
            Triple<DepGraph, DepGraph, List<PAStructure>> parse = CoNLL2008Format
					.readNextGraph(new BufferedReader(new StringReader(sb.toString())));
			if (parse == null) {
				getContext().getLogger().log(Level.SEVERE, "Initializing LTHParser FAILED.");
				throw new RuntimeException("could not read parse!");
			} else {
				System.out.println(parse.first.toString());
				System.out.println("Done.");
			}
            
			/*final PrintWriter toParser = new PrintWriter(
					process.getOutputStream());
			final Scanner fromParser = new Scanner(process.getInputStream());

			toParser.println("I am testing .");
			toParser.flush();
			Triple<DepGraph, DepGraph, List<PAStructure>> parse = CoNLL2008Format
					.readNextGraph(fromParser);
			if (parse == null) {
				getContext().getLogger().log(Level.SEVERE, "Initializing LTHParser FAILED.");
				throw new RuntimeException("could not read parse!");
			}*/
			/*else {
				System.out.println(parse.first.toString());
				System.out.println("Done.");
			}*/
			
			}
			getContext().getLogger().log(Level.INFO, "Initializing LTHParser parser finished.");
		}  catch (IOException e) {
			e.printStackTrace();
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		
		//needs input from tokenizer
		FSIndex<Annotation> sentencesIndex = cas.getAnnotationIndex(Sentence.type);

		// iterate over all sentences

		Iterator<Annotation> sentenceIterator = sentencesIndex.iterator();

		
		Process process;
		try {
			process = new ProcessBuilder(parserCommand).start();
		
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream())),true);
		
			while (sentenceIterator.hasNext()) {
				Sentence sentence = (Sentence) sentenceIterator.next();
				
				// get tokens of sentence
				List<AnnotationFS> myTokens = JCasUtility.selectCovered(cas,
						Token.class, sentence);
				
				StringBuilder sentenceString = new StringBuilder();
				for (AnnotationFS token : myTokens) 
					sentenceString.append(token.getCoveredText()+" ");
				
				writer.println(sentenceString.toString().trim());
			}
		
/*			//TODO: check if this is efficient
			final PrintWriter toParser = new PrintWriter(
					process.getOutputStream());
			final Scanner fromParser = new Scanner(process.getInputStream());
			
			toParser.println(sentenceString.toString().trim());
			toParser.flush();
			*/		
		
			
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
 
            List<String> output = outGobbler.getOuput();
            List<String> outputErr = errGobbler.getOuput();
      
            List<String> parses = new ArrayList<String>();
            
			//Triple<DepGraph, DepGraph, List<PAStructure>> parse = CoNLL2008Format
			//		.readNextGraph(fromParser);
			
			// store parse in CoNLL 2008
			StringBuilder sb = new StringBuilder();
			for (String out : output) {
				
				if (out.trim().length()>0)
					sb.append(out);
				else {
					parses.add(sb.toString());
					sb = new StringBuilder();
				}
			}
			
			//String parseStringCoNLL2008 = sb.toString();
			//System.out.println(parseStringCoNLL2008);
			
			Triple<DepGraph, DepGraph, List<PAStructure>> parse = null;
			
				//iterate once more 
				sentenceIterator = sentencesIndex.iterator();
				int sentenceIndex =0;
				while (sentenceIterator.hasNext()) {
					Sentence sentence = (Sentence) sentenceIterator.next();
					String parseStringCoNLL2008 = parses.get(sentenceIndex);
					parse = CoNLL2008Format.readNextGraph(new BufferedReader(new StringReader(parseStringCoNLL2008)));
					sentenceIndex++;
					// get tokens of sentence
					List<AnnotationFS> myTokens = JCasUtility.selectCovered(cas,
							Token.class, sentence);
				
				if (myTokens.size() != getLengthNodesWithoutRoot(parse)) {
					throw new AnalysisEngineProcessException(MESSAGE_DIGEST, "tokens_tags_error", 
							new Object[]{sentence.getCoveredText()});
				}

				getContext().getLogger().log(Level.FINE, parse.first.toString());
				
				//System.out.println(parseStringCoNLL2008);
				
				int curIdx=0;
				for (DepNode node : getNodesWithoutRoot(parse)) {
					Token token = (Token) myTokens.get(curIdx);
									
					Pos pos = new Pos(cas);
					pos.setBegin(token.getBegin());
					pos.setEnd(token.getEnd());
					pos.setPostag(node.pos);
					pos.setAnnotatorId(LTHParserFast.class.getCanonicalName());
					pos.setToken(token);
					pos.addToIndexes();
					
					Lemma lemma = new Lemma(cas);
					lemma.setBegin(token.getBegin());
					lemma.setEnd(token.getEnd());
					lemma.setLemma(node.lemma);
					lemma.setAnnotatorId(LTHParserFast.class.getCanonicalName());
					lemma.setToken(token);
					lemma.addToIndexes();
					
					curIdx++;
				}
			
				CoNLL2008DependencyTree depTree = new CoNLL2008DependencyTree(cas);
				depTree.setBegin(sentence.getBegin());
				depTree.setEnd(sentence.getEnd());
				depTree.setRawParse(parseStringCoNLL2008);
				depTree.setAnnotatorId(LTHParserFast.class.getCanonicalName());
				depTree.setSentence(sentence);
				depTree.addToIndexes();
				
				getContext().getLogger().log(Level.INFO, "Done.");
			}
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	private List<DepNode> getNodesWithoutRoot(
			Triple<DepGraph, DepGraph, List<PAStructure>> parse) {
		int i=0;
		List<DepNode> outNodes = new ArrayList<DepNode>();
		for (DepNode node : parse.first.nodes) {
			if (i>0)
				outNodes.add(node);
			i++;
		}
		return outNodes;
	}


	private int getLengthNodesWithoutRoot(
			Triple<DepGraph, DepGraph, List<PAStructure>> parse) {
		return parse.first.nodes.length -1 ; // do not count root token
	}

}
