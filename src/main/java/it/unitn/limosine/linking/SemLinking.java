package it.unitn.limosine.linking;

import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.wlinking.WikiLink;
import it.unitn.limosine.util.JCasUtility;
import it.unitn.limosine.util.StanfordToken;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;

import java.net.*;
import java.io.*;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;


public class SemLinking  extends JCasAnnotator_ImplBase{
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		
		//needs input from tokenizer
		
		 FSIndex<?> sentencesIndex = cas.getAnnotationIndex(Sentence.type);
		 
		 //iterate over all sentences
		 
		 Iterator<?> sentenceIterator = sentencesIndex.iterator();
		 while (sentenceIterator.hasNext()) {
			 Sentence sentence = (Sentence) sentenceIterator.next();
			 List<AnnotationFS> myTokens = JCasUtility.selectCovered(cas, Token.class, sentence);
			 StringBuilder sentenceString = new StringBuilder();
			 sentenceString.append("http://vps.limosine-project.eu:8080/semantic-mining-module/commonness?input=");
			 List<String> tokenstrings = new ArrayList<String>();
			 for (AnnotationFS tok : myTokens) {
				 String curtok=tok.getCoveredText();
				 sentenceString.append(curtok+"%20");
				 tokenstrings.add(curtok.toLowerCase());
			 }
			 System.out.println(sentenceString.toString());
		     try
		     {
		        /*
		         * Get a connection to the URL and start up
		         * a buffered reader.
		         */
		    	 URL url = new URL(sentenceString.toString());
		    	 URLConnection urlConnection=url.openConnection();
		    	 
		    	 BufferedReader inputReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

		         StringBuilder sb = new StringBuilder();
		         String inline = "";
		         while ((inline = inputReader.readLine()) != null) {
		           sb.append(inline);
		         }

		         System.out.println(sb.toString());
		         
		         SAXBuilder builder = new SAXBuilder();

		    	 
		    	 
		         Document document = (Document) builder.build(new ByteArrayInputStream(sb.toString().getBytes()));
		    	    Element rootNode = document.getRootElement();
		    	    
		    	    Element resp = rootNode.getChild("Response");
		    	    List list = resp.getChildren("Sense");
		    	    
		    	    for (int i = 0; i < list.size(); i++) {
		    	      Element node = (Element) list.get(i);
		    	      System.out.println("====================");
		    	      
	    	    	  //now we have to find all the occurences of this fragment in the original text
		    	      String wikiid=node.getAttribute("title").getValue();
		    	      System.out.println("Found wikiid " + wikiid);
		    	      List mentions = node.getChildren("Ngram");
		    	      for (int j= 0; j<mentions.size(); j++) {
			    	      System.out.println("---------------");
			    	      Element m = (Element) mentions.get(j);
		    	    	  Double wscore=Double.parseDouble(m.getAttribute("score").getValue());
		    	    	  String msurface=m.getAttribute("text").getValue();
		    	    	  String[] splitArray = msurface.split("\\s+");
		    	    	  int tottok=splitArray.length;
		    	    	  System.out.println(" child text=" + msurface + " score=" + wscore);
		    	    	  
		    	    	  for (int ii=0; ii<=tokenstrings.size()-tottok;ii++) {
		    	    		  int jj=0;
		    	    		  for (; jj<tottok; jj++) {
		    	    			  if (!tokenstrings.get(ii+jj).equals(splitArray[jj])) break;
		    	    		  }	
		    	    		  if (jj==tottok) {
		    	    			  
		    	    			  //register a new mention, starts at token sentstart+ii, ends at token sentstart+ii+jj
		    	  				WikiLink wl=new WikiLink(cas);
		    	  				//ToDo here -- get correct offsets
		    	  				wl.setBegin(myTokens.get(ii).getBegin());
		    	  				wl.setEnd(myTokens.get(ii+jj-1).getEnd());
		    	  				wl.setWikiId(wikiid);
		    	  				wl.setWikiScore(wscore);
		    	  				wl.addToIndexes();
		    	    			  
		    	    		  }
		    	    	  	}
		    	    	  
		    	    	  
		    	      }
		    	    }

		    	    //
		    	 
		    	 /*
		    	 // ToDo here
		    	 InputStream reader = url.openStream();
		    	 System.out.println(reader.toString());
		    	 // end of ToDo
		    	 reader.close();
		    	 
		    	 */
		     	}
		     	catch (MalformedURLException e)
		     	{
		     		e.printStackTrace();
		     	}
		     	catch (JDOMException e)
		     	{
		     		e.printStackTrace();
		     	}
		     	catch (IOException e)
		     	{
		     		e.printStackTrace();
		     	}
			 
		 
		 	}
				 
	/*		 
			 
			 final List<CoreLabel> ner = classifier.classifySentence(tokensOfSentence);    
			 int i=-1;
			 
			 String oldtag="O";
			 NER ne=null;
			 for (CoreLabel label : ner)
		
			 { 
				 i++;				 
				String netag=label.get(AnswerAnnotation.class);
				//TODO: they should actually be IOB -- but no traces of those in stanford docs :(
				// quick hack: consider sequences with the same netag to span over a single NE
				
				if (netag.equals(oldtag)) continue;
				if (i>0 && ne !=null) {
					AnnotationFS token=myTokens.get(i-1);
					ne.setEnd(token.getEnd());	
					ne.setEndToken((Token)token);
					ne.setAnnotatorId(getClass().getCanonicalName());
					ne.addToIndexes();
				}
				oldtag=netag;
				if (netag.equals("O")) {
					ne=null;
					continue;
				}
				ne=new NER(cas);
				AnnotationFS token=myTokens.get(i);
				ne.setBegin(token.getBegin());
				ne.setStartToken((Token)token);
				ne.setNetag(netag);

				
			 }
		 }
		 
*/

	}



}
