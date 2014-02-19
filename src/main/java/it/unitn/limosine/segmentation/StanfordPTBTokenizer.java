package it.unitn.limosine.segmentation;

import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;

import java.io.StringReader;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;

public class StanfordPTBTokenizer extends JCasAnnotator_ImplBase {

	// Limo: add these options to get annotation (offset of original tokens)
	private static boolean keepNewLines = true;
	private static boolean invertible = true;
	
	private String encoding = "utf-8";
	
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
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
		edu.stanford.nlp.process.TokenizerFactory<? extends HasWord> tokenizerFactory = PTBTokenizer.factory(keepNewLines,invertible);
		
		// tokenize
		DocumentPreprocessor docPreprocessor = new DocumentPreprocessor(
				new StringReader(jcas.getDocumentText()));
		docPreprocessor.setTokenizerFactory(tokenizerFactory);

		int sentenceCount = 0;
		int tokenCount=0; //document-level counter for tokens 
		for (List<HasWord> docProcSentence : docPreprocessor) {
			Sentence sentence = new Sentence(jcas);
			sentence.setSentenceId(sentenceCount);
			
			//save tokenized sentence, too (getCoveredText() returns original string)
			StringBuilder tokenizedSentence = new StringBuilder();
			
			int tokenSentCount = 0; //sentence-level counter for tokens
			int sentenceEndPosition = -1;
			for (HasWord iw : docProcSentence) {
				CoreLabel coreLab = (CoreLabel) iw;
				//ignore 'empty' tokens, like newline
				if (coreLab.originalText().trim().length()==0) 
					continue;
				Token token = new Token(jcas);
				token.setBegin(coreLab.beginPosition());
				token.setEnd(coreLab.endPosition());
				token.setTokenId(tokenCount);
				token.setTokenSentId(tokenSentCount);
				token.setAnnotatorId(getClass().getCanonicalName());   
				token.setNormalizedText(forXML(token.getCoveredText()));
				token.setStanfordNormalizedText(coreLab.value());
				token.addToIndexes();
				
				if (tokenSentCount==0) {
					sentence.setBegin(coreLab.beginPosition());
					sentence.setStartToken(token);
				}
				tokenizedSentence.append(token.getNormalizedText() + " ");
				
				// update end position of sentence
				sentenceEndPosition = coreLab.endPosition();
				sentence.setEndToken(token);
				tokenCount++;
				tokenSentCount++;
				
			}
			if (!(tokenSentCount==0)) {
				sentence.setEnd(sentenceEndPosition);
				sentence.setTokenizedSentence(tokenizedSentence.toString().trim());
				sentence.setAnnotatorId(getClass().getCanonicalName());
				sentence.addToIndexes();
				sentenceCount+=1;
			}
		}
	}

}
