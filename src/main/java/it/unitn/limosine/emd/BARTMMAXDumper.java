package it.unitn.limosine.emd;

//ToDo:  encoding, 
//ToDo: get doc names from cas :(
//ToDo: ensure that sentences are ordered correctly :(



//NB: BART token ids should start with 1, not with 0 for some weird reason (fixed here, just keep in mind) :(

import java.io.IOException;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.Iterator;

import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import elkfed.config.ConfigProperties;
import elkfed.mmax.minidisc.MiniDiscourse;


public class BARTMMAXDumper extends JCasAnnotator_ImplBase{

	//String tmpbartdatadir="/Users/oliunya/semod/tmp/bartdatadir";
	//String tmpbartdatadir="/tmp/bartdatadir";
	String tmpbartdatadir; 
	
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		
		try {
			
			//SharedModel sharedModel = (SharedModel) getContext().getResourceObject("TmpBARTDataDir");
			SharedModel sharedModel = (SharedModel) getContext().getResourceObject("BARTtmpdirectory");
			tmpbartdatadir = sharedModel.getPath();
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		} 

	}

	private int MMAXTokenId(Token t) {
		return t.getTokenId()+1;
	}

		  
	private void dumptokens(JCas cas, String docId) {
		FSIndex tokensIndex = cas.getAnnotationIndex(Token.type);
		Iterator tokenIterator = tokensIndex.iterator();

		//TODO: get encoding from uima
		
		PrintWriter out;
		try {
			out = new PrintWriter(new FileWriter(tmpbartdatadir + 
						File.separator + "Basedata" + File.separator + 
						docId+"_words.xml"));

			out.println("<?xml version=\"1.0\" encoding=\"" + "UTF-8" + "\"?>");
			out.println("<!DOCTYPE words SYSTEM \"words.dtd\">");
			out.println("<words>");

			while (tokenIterator.hasNext()) {
				Token t = (Token) tokenIterator.next();
				int tid=MMAXTokenId(t);
				String tsurf=t.getNormalizedText();
				out.println("<word id=\"word_" + tid + "\">" +
						tsurf + "</word>");
			}
			out.println("</words>");
			out.close();

			} catch (IOException e) {
				e.printStackTrace();
			} 	

		
		}	
	
	private void dumpheader(JCas cas, String docId) {

		//TODO: get encoding from uima
		
		PrintWriter out;
		try {
			out = new PrintWriter(new FileWriter(tmpbartdatadir + 
						File.separator  + 
						docId+".mmax"));

			
			out.println("<?xml version=\"1.0\" encoding=\"" + "UTF-8" + "\"?>");
			out.println("<mmax_project>");
			out.println("<words>" + docId +"_words.xml</words>");
			out.println("<keyactions></keyactions>");
			out.println("<gestures></gestures>");
			out.println("</mmax_project>");

			out.close();

			} catch (IOException e) {
				e.printStackTrace();
			} 	

		
	}
	
	private void dumpsentences(JCas cas, String docId) {
		FSIndex sentsIndex = cas.getAnnotationIndex(Sentence.type);
		Iterator sentIterator = sentsIndex.iterator();

		//TODO: get encoding from uima
		
		PrintWriter out;
		try {
			out = new PrintWriter(new FileWriter(tmpbartdatadir + 
						File.separator + "markables" + File.separator + 
						docId+"_sentence_level.xml"));

			out.println("<?xml version=\"1.0\" encoding=\"" + "UTF-8" + "\"?>");
			out.println("<!DOCTYPE markables SYSTEM \"markables.dtd\">");
			out.println("<markables xmlns=\"www.eml.org/NameSpaces/sentence\">");
//			<markable id="markable_0" span="word_1..word_31" orderid="0" mmax_level="sentence" />

			int oid=0;
			int mid=0;
			
			while (sentIterator.hasNext()) {
				Sentence t = (Sentence) sentIterator.next();
				int startwid=MMAXTokenId(t.getStartToken());
				int endwid=MMAXTokenId(t.getEndToken());

				out.println("<markable id=\"markable_" + mid + 
							"\" span=\"word_"+ startwid + "..word_" + endwid + 
							"\" orderid=\"" + oid + 
							"\" mmax_level=\"sentence\" />");
				oid++;
				mid++;
			}
			out.println("</markables>");
			out.close();

			} catch (IOException e) {
				e.printStackTrace();
			} 	

		
		}	
	private void dumppos(JCas cas, String docId) {
		FSIndex posIndex = cas.getAnnotationIndex(Pos.type);
		Iterator posIterator = posIndex.iterator();

		//TODO: get encoding from uima
		
		PrintWriter out;
		try {
			out = new PrintWriter(new FileWriter(tmpbartdatadir + 
						File.separator + "markables" + File.separator + 
						docId+"_pos_level.xml"));

			out.println("<?xml version=\"1.0\" encoding=\"" + "UTF-8" + "\"?>");
			out.println("<!DOCTYPE markables SYSTEM \"markables.dtd\">");
			out.println("<markables xmlns=\"www.eml.org/NameSpaces/pos\">");

//			<markable id="markable_0" span="word_1" tag="nnp" mmax_level="pos" />

			int mid=0;
			
			while (posIterator.hasNext()) {
				Pos p = (Pos) posIterator.next();
				int startwid=MMAXTokenId(p.getToken());
				String postag=p.getPostag().toLowerCase();
				
				out.println("<markable id=\"markable_" + mid + 
							"\" span=\"word_"+ startwid + 
							"\" tag=\"" + postag +
							"\" mmax_level=\"pos\" />");
				mid++;
			}
			out.println("</markables>");
			out.close();

			} catch (IOException e) {
				e.printStackTrace();
			} 	

		
		}	
	

	private void dumpparse(JCas cas, String docId) {
		FSIndex parseIndex = cas.getAnnotationIndex(ConstituencyTree.type);
		Iterator parseIterator = parseIndex.iterator();

		//TODO: get encoding from uima
		
		PrintWriter out;
		try {
			out = new PrintWriter(new FileWriter(tmpbartdatadir + 
						File.separator + "markables" + File.separator + 
						docId+"_parse_level.xml"));

			out.println("<?xml version=\"1.0\" encoding=\"" + "UTF-8" + "\"?>");
			out.println("<!DOCTYPE markables SYSTEM \"markables.dtd\">");
			out.println("<markables xmlns=\"www.eml.org/NameSpaces/parse\">");

//			<markable id="markable_2" span="word_61..word_66" tag="(ROOT (S (NP (NN Revenue)) (VP (VBD totaled) (NP (QP ($ $) (CD 5) (CD million)))) (. .)))" mmax_level="parse" />

			int mid=0;
			
			while (parseIterator.hasNext()) {
				ConstituencyTree p = (ConstituencyTree) parseIterator.next();
				int startwid=MMAXTokenId(p.getSentence().getStartToken());
				int endwid=MMAXTokenId(p.getSentence().getEndToken());
				String parsetag=p.getRawParse();
				
				out.println("<markable id=\"markable_" + mid + 
							"\" span=\"word_"+ startwid + "..word_" + endwid + 
							"\" tag=\"" + parsetag +
							"\" mmax_level=\"parse\" />");
				mid++;
			}
			out.println("</markables>");
			out.close();

			} catch (IOException e) {
				e.printStackTrace();
			} 	

		
		}	
	

	private void dumpner(JCas cas, String docId) {
		FSIndex nerIndex = cas.getAnnotationIndex(NER.type);
		Iterator nerIterator = nerIndex.iterator();

		//TODO: get encoding from uima
		
		PrintWriter out;
		try {
			out = new PrintWriter(new FileWriter(tmpbartdatadir + 
						File.separator + "markables" + File.separator + 
						docId+"_enamex_level.xml"));

			out.println("<?xml version=\"1.0\" encoding=\"" + "UTF-8" + "\"?>");
			out.println("<!DOCTYPE markables SYSTEM \"markables.dtd\">");
			out.println("<markables xmlns=\"www.eml.org/NameSpaces/enamex\">");


			int mid=0;
			
			while (nerIterator.hasNext()) {
				NER ne = (NER) nerIterator.next();
				int startwid=MMAXTokenId(ne.getStartToken());
				int endwid=MMAXTokenId(ne.getEndToken());
				String netag=ne.getNetag().toLowerCase();
				
//<markable id="markable_0" span="word_1..word_2" tag="organization" mmax_level="enamex" />

				out.println("<markable id=\"markable_" + mid + 
							"\" span=\"word_"+ startwid + "..word_" + endwid + 
							"\" tag=\"" + netag +
							"\" mmax_level=\"enamex\" />");
				mid++;
			}
			out.println("</markables>");
			out.close();

			} catch (IOException e) {
				e.printStackTrace();
			} 	

		
		}	
	

	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
        String docId = "doc1";
        
        //weird solution to ensure that all the remnants of whatever MMAX-like stuff that exists in the tmpbartdatadir are deleted
        dumpheader(cas,docId);
 		dumptokens(cas,docId);

        File mmaxDir = new File(tmpbartdatadir);
        MiniDiscourse doc = MiniDiscourse.load(mmaxDir, docId);
        doc.deleteAll();
        
        dumpheader(cas,docId);
  		dumptokens(cas,docId);
		
		dumpsentences(cas,docId);
		dumpner(cas,docId);
		dumppos(cas,docId);
		dumpparse(cas,docId);
		
	}


}



