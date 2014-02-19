package it.unitn.limosine.syntax.constituency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.*;
import it.unitn.limosine.util.StanfordToken;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

/***
 * Calls the Stanford parser and returns the constituent tree as string
 * 
 * @author bplank
 *
 */
public class StanfordParser extends JCasAnnotator_ImplBase {

	private LexicalizedParser parser; 
	
	public static final String MESSAGE_DIGEST = "it.unitn.limosine.internationalization.messages.StanfordParserAnnotator_Messages";
	
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
	
		// initialize parsing model
		SharedModel sharedModel;
		try {
			sharedModel = (SharedModel) getContext().getResourceObject(
					"StanfordParserModel");
			String modelName = sharedModel.getPath();		
			parser = LexicalizedParser.loadModel(modelName); 
			
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
		

		 while (sentenceIterator.hasNext()) {
			 Sentence sentence = (Sentence) sentenceIterator.next();
			 //System.out.println(sentence.getSentenceId() + ") "+sentence.getCoveredText());

			 
			//get tokens of sentence, cast them to Stanford tokens (see class below)
			 List<AnnotationFS> myTokens = JCasUtility.selectCovered(cas, Token.class, sentence);
			 			 
			 ArrayList<StanfordToken> tokensOfSentence = new ArrayList<StanfordToken>();
			 for (AnnotationFS tok : myTokens) {
				 Token token = (Token)tok;
				 tokensOfSentence.add(new StanfordToken(token)); 
			 }
			 
			 
			 /*
			  * this is very buggy code, will leave out some tokens! better: use selectCovered method above
			  * ArrayList<StanfordToken> tokensOfSentence = new ArrayList<StanfordToken>();
			 
			 if (tokenIterator.hasNext()) {
				 Token token = (Token)tokenIterator.next();
				 while (token.getBegin() >= sentence.getBegin() && token.getEnd() <= sentence.getEnd()) {
					 tokensOfSentence.add(new StanfordToken(token));
					 if (tokenIterator.hasNext())
						 token = (Token)tokenIterator.next();
					 else
						 break;
				 }
			 }*/
			 
			 /**
			  * Note: if we give parser the sentence (getCoveredText()) then it does its own tokenization.
			  *       with getTokenizedSentence still problems, like 1/3 will get 1\/3
			  *       Therefore, here we use a private helper class that wraps Limosine tokens into Stanford (HasWord) 
			  *       and call the parser with the list of those tokens.
			  */
			 Tree parseTree = parser.parseTree(tokensOfSentence);

			 //replace whitespace
			 String parseTreeStr = parseTree.pennString().replaceAll("\\s+"," ");

			 ConstituencyTree pennTree = new ConstituencyTree(cas);
			 pennTree.setBegin(sentence.getBegin());
			 pennTree.setEnd(sentence.getEnd());
			 pennTree.setRawParse(parseTreeStr);
			 pennTree.setAnnotatorId(StanfordParser.class.getCanonicalName());
			 pennTree.setSentence(sentence);
			 pennTree.addToIndexes();
			 
			 //get pos tags from leaves
			List<CoreLabel> preterminalRules = parseTree.taggedLabeledYield();
			//preterminalRules.add(new CoreLabel()); to test error message, uncomment this line
			if (tokensOfSentence.size() != preterminalRules.size()) {
				throw new AnalysisEngineProcessException(MESSAGE_DIGEST, "tokens_tags_error", 
						new Object[]{sentence.getCoveredText()});
			}
		    
			// get postags
			for (int idx=0; idx<tokensOfSentence.size(); idx++) {
				Token token = ((StanfordToken)tokensOfSentence.get(idx)).getToken();
				CoreLabel label = preterminalRules.get(idx);
				
				Pos pos = new Pos(cas);
				pos.setBegin(token.getBegin());
				pos.setEnd(token.getEnd());
				pos.setPostag(label.tag());
				pos.setToken(token);
				pos.addToIndexes();
			}
			
			
		 }
		 // do no longer need to instantiate logger object (like in snowball example!)
		 getContext().getLogger().log(Level.INFO, "Stanford Parser Annotator processing finished");
	}
	
	
	/**
	 * some code to test if by calling it from the program
	 */
	public static void main(String[] args) {
		
		//get Resource Specifier from XML file
		
		try {
			XMLInputSource in = new XMLInputSource("desc/pipelines/StanfordParserAnnotator.xml");
		
		ResourceSpecifier specifier = 
		    UIMAFramework.getXMLParser().parseResourceSpecifier(in);

		  //create AE here
		AnalysisEngine ae = 
		    UIMAFramework.produceAnalysisEngine(specifier);
		
		//create a JCas, given an Analysis Engine (ae)
		JCas jcas = ae.newJCas();
		  
		  //analyze a document
		String doc1text = "This cannot be 1/3.";
		jcas.setDocumentText(doc1text);
		ae.process(jcas);
		printAndCheckResults(jcas);
		jcas.reset();
		  
		  //done
		ae.destroy();
		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidXMLException e) {
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		} catch (AnalysisEngineProcessException e) {
			e.printStackTrace();
		}
	}
	//check whether we got back what expected
	private static void printAndCheckResults(JCas cas) {
		 FSIndex<Annotation> treeIndex = cas.getAnnotationIndex(ConstituencyTree.type);
		 Iterator<Annotation> treeIterator = treeIndex.iterator();
		 while (treeIterator.hasNext()) {
			 ConstituencyTree tree = (ConstituencyTree) treeIterator.next();
			 
			 String expected ="(ROOT (S (NP (DT This)) (VP (MD can) (RB not) (VP (VB be) (NP (CD 1/3)))) (. .))) ";
			 
			 System.out.println(tree.getCoveredText());
			
			 //get sentence
			 List<?> sentences = JCasUtility.selectCovered(cas, Sentence.class, tree);
			 Sentence s = (Sentence) sentences.get(0);
			 System.out.println(s.getCoveredText());
			 
			 //get tokens
			 List<AnnotationFS> tokens = JCasUtility.selectCovered(cas, Token.class, tree);
			 for (AnnotationFS t : tokens) 
				 System.out.println(t.getCoveredText());
			 
			 System.out.println("Expected: " +expected);
			 System.out.println("     Got: " +tree.getRawParse());
			 if (!expected.equals(tree.getRawParse()))
				 System.err.println("Error! Expected parse tree is different from obtained tree. Check StanfordParser.java");
			 
		 }
	}

}
