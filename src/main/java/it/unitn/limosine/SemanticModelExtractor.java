package it.unitn.limosine;

import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

/***
 * Main class to call SemanticModelExtractor from command line
 * 
 * @author bplank
 *
 */
public class SemanticModelExtractor {
	
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
			
		
			FileOutputStream out = new FileOutputStream("/tmp/test.xmi");
			XmiCasSerializer ser = new XmiCasSerializer(jcas.getTypeSystem());
			XMLSerializer xmlSer = new XMLSerializer(out, false);
			ser.serialize(jcas.getCas(), xmlSer.getContentHandler());
			System.err.println("File written: /tmp/test.xmi");
			
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
			} catch (SAXException e) {
				// TODO Auto-generated catch block
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
