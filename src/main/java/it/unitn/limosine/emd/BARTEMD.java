package it.unitn.limosine.emd;

// this is a dummy solution -- it creates a tmp BART directory and just dumps everything there and runs BART normally
//ToDo: this also has a very stupid method of resolving token ids to actual tokens, no clue how to just get it by id..


import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import elkfed.mmax.Corpus;
import elkfed.mmax.minidisc.MiniDiscourse;
import elkfed.mmax.pipeline.Pipeline;
import elkfed.mmax.pipeline.CoNLLClosedPipeline;
import elkfed.mmax.minidisc.MarkableHelper;
import elkfed.config.ConfigProperties;
import elkfed.coref.mentions.Mention;
import elkfed.coref.mentions.DefaultMentionFactory;
import elkfed.coref.mentions.MentionFactory;
import elkfed.lang.EnglishLanguagePlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.unitn.limosine.types.emd.EMD;
import it.unitn.limosine.util.*;
import edu.stanford.nlp.trees.Tree;

public class BARTEMD extends JCasAnnotator_ImplBase{


	String tmpbartdatadir;
	String bartconfigdir;
    ConfigProperties cp;
    protected final MentionFactory _mfact=new DefaultMentionFactory();

	private int MMAXTokenId(Token t) {
//		return t.getTokenId()+1;
		return t.getTokenId();
	}

	private void usentence2mmax(JCas cas, MiniDiscourse doc) {
		FSIndex sentsIndex = cas.getAnnotationIndex(Sentence.type);
		Iterator sentIterator = sentsIndex.iterator();

		int oid=0;
		int mid=0;
		
		while (sentIterator.hasNext()) {
			Sentence t = (Sentence) sentIterator.next();
			int startwid=MMAXTokenId(t.getStartToken());
			int endwid=MMAXTokenId(t.getEndToken());
			Map<String, String> attrs = new HashMap<String, String>();
			attrs.put("orderid", Integer.toString(oid));
			doc.getMarkableLevelByName("sentence").addMarkable(startwid, endwid, attrs);
			oid++;
			mid++;
		}
		
	}	
	private void uparse2mmax(JCas cas, MiniDiscourse doc) {
		FSIndex parseIndex = cas.getAnnotationIndex(ConstituencyTree.type);
		Iterator parseIterator = parseIndex.iterator();

		int oid=0;
		int mid=0;
		
		while (parseIterator.hasNext()) {
			ConstituencyTree p = (ConstituencyTree) parseIterator.next();
			int startwid=MMAXTokenId(p.getSentence().getStartToken());
			int endwid=MMAXTokenId(p.getSentence().getEndToken());
			String parsetag=p.getRawParse();

			/*
			out.println("<markable id=\"markable_" + mid + 
						"\" span=\"word_"+ startwid + "..word_" + endwid + 
						"\" tag=\"" + parsetag +
						"\" mmax_level=\"parse\" />");
			*/
			Map<String, String> attrs = new HashMap<String, String>();
			attrs.put("tag", parsetag);
			doc.getMarkableLevelByName("parse").addMarkable(startwid, endwid, attrs);
			oid++;
			mid++;
		}
		
	}	

	private void uner2mmax(JCas cas, MiniDiscourse doc) {
		FSIndex nerIndex = cas.getAnnotationIndex(NER.type);
		Iterator nerIterator = nerIndex.iterator();


			int mid=0;
			
			while (nerIterator.hasNext()) {
				NER ne = (NER) nerIterator.next();
				int startwid=MMAXTokenId(ne.getStartToken());
				int endwid=MMAXTokenId(ne.getEndToken());
				String netag=ne.getNetag().toLowerCase();
				
				Map<String, String> attrs = new HashMap<String, String>();
				attrs.put("tag", netag);
				doc.getMarkableLevelByName("enamex").addMarkable(startwid, endwid, attrs);
/*
				out.println("<markable id=\"markable_" + mid + 
							"\" span=\"word_"+ startwid + "..word_" + endwid + 
							"\" tag=\"" + netag +
							"\" mmax_level=\"enamex\" />");
*/
				mid++;
			}


		
		}	
	
	private void upos2mmax(JCas cas, MiniDiscourse doc) {

		FSIndex posIndex = cas.getAnnotationIndex(Pos.type);
		Iterator posIterator = posIndex.iterator();

			int mid=0;
			
			while (posIterator.hasNext()) {
				Pos p = (Pos) posIterator.next();
				int startwid=MMAXTokenId(p.getToken());
				String postag=p.getPostag().toLowerCase();

				/*
				out.println("<markable id=\"markable_" + mid + 
							"\" span=\"word_"+ startwid + 
							"\" tag=\"" + postag +
							"\" mmax_level=\"pos\" />");
				*/
				Map<String, String> attrs = new HashMap<String, String>();
				attrs.put("tag", postag);
				doc.getMarkableLevelByName("pos").addMarkable(startwid, startwid, attrs);
				mid++;
			}


		
		}	
		
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		try {
			
		//initialize model, read from resource manager
				
		SharedModel sharedModel = (SharedModel) getContext().getResourceObject("BARTtmpdirectory");
		//String modelName = sharedModel.getPath();
		tmpbartdatadir = sharedModel.getPath();
		
		SharedModel sharedModelConfig = (SharedModel) getContext().getResourceObject("BARTconfigdirectory");
		//String modelName = sharedModel.getPath();
		bartconfigdir = sharedModelConfig.getPath();
		
        cp=elkfed.config.ConfigProperties.getInstance(bartconfigdir);
		//System.out.println("Using: \n"+tmpbartdatadir+"\n"+bartconfigdir);
		} catch (ClassCastException e) {
			e.printStackTrace();
		}  catch (ResourceAccessException e) {
			e.printStackTrace();
		}
		
	}
	
	public static Token id2token (FSIndex tind, int i) {
		//MMAX ids are cas ids + 1, but we don't ever store MMAX ids in cas, so no adjustment needed
		Iterator tokenIterator = tind.iterator();
		while (tokenIterator.hasNext()) {
			Token t = (Token) tokenIterator.next();
			if (t.getTokenId()==i) return t;
		}
		return null;

	}
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		
		String docId = UUID.randomUUID().toString();

        
		File mmaxDir = new File(tmpbartdatadir);
		ConfigProperties cp=elkfed.config.ConfigProperties.getInstance(bartconfigdir);
		
		FSIndex tokensIndex = cas.getAnnotationIndex(Token.type);
		Iterator<?> tokensIterator = tokensIndex.iterator();

		ArrayList<String> toks=new ArrayList<String>();
		while (tokensIterator.hasNext()) {
			Token tok = (Token) tokensIterator.next();
			toks.add(tok.getNormalizedText());
		}
	
		String [] toks_array = toks.toArray(new String[toks.size()]);
		//		MiniDiscourse doc = MiniDiscourse.createFromTokens(mmaxDir, docId, (String[]) toks.toArray());
		MiniDiscourse doc = MiniDiscourse.createFromTokens(mmaxDir, docId, toks_array);

		usentence2mmax(cas, doc);
		uner2mmax(cas, doc);
		upos2mmax(cas,doc);
		uparse2mmax(cas,doc);


		Corpus c = new Corpus();
		c.add(doc);

		Pipeline pipeline = new CoNLLClosedPipeline();
		pipeline.setData(c);
		pipeline.annotateData();
        
		List<Mention> mentions=null;
		try {	
			mentions=_mfact.extractMentions(doc);
		}catch (IOException e){
			e.printStackTrace();       
        }
        for (Mention m: mentions) {
        	EMD mcas=new EMD(cas);
        	String acetype="NOM";
        	if (m.getProperName()) acetype="NAM";
        	if (m.getPronoun()) acetype="PRO";
        	mcas.setACEtype(acetype);
        	mcas.setHead(m.getHeadString()); //B: add entity type
        	
//        	if (m.getEnamexType().equals("np"))
        	String etype="ENTITY";
           	if (m.isEnamex())
        		etype=m.getEnamexType().toUpperCase(); //PERSON, LOCATION, ORGANIZATION
           	mcas.setEntityType(etype);
        	int startext=m.getMarkable().getLeftmostDiscoursePosition();
        	int endext=m.getMarkable().getRightmostDiscoursePosition();
        	Token tstext=id2token(tokensIndex,startext);	
        	Token tenext=id2token(tokensIndex,endext);	
        	mcas.setStartToken(tstext);
        	mcas.setEndToken(tenext);
        	String stype=m.getSemanticClass().toString();
        	if (stype==null)
        		stype="null";
        	if (stype.equals("UNKNOWN") && ! etype.equals("ENTITY"))
        		stype=etype;
        	mcas.setSemanticType(stype);
/*
        	mcas.setBegin(tstext.getBegin());
           	mcas.setEnd(tenext.getEnd());
*/
           	
          	String minspan=m.getMarkable().getAttributeValue("min_ids");
          	Token tstmin=tstext;
          	Token tenmin=tenext;
          	if (minspan!=null) {
          		String[] spanIDs = MarkableHelper.parseRange(minspan);
          		int startmin =
          				doc.getDiscoursePositionFromDiscourseElementID(spanIDs[0]);
          		int endmin =        
          				doc.getDiscoursePositionFromDiscourseElementID(spanIDs[1]);
         	
          		tstmin=id2token(tokensIndex,startmin);	
          		tenmin=id2token(tokensIndex,endmin);	
          	}
          	Tree tm=m.getParseHead();
          	List<Tree> stree=m.getSentenceTree().getLeaves();
          	int sind;
          	for (sind=0;sind<stree.size();sind++) {
          		if (stree.get(sind)==tm) break;
          	}
          	
          	sind+=m.getSentenceStart();
          	Token thead=id2token(tokensIndex,sind);
          	mcas.setHeadToken(thead);
          	
        	mcas.setStartTokenMIN(tstmin);
        	mcas.setEndTokenMIN(tenmin);
           	mcas.setBegin(tstmin.getBegin());
           	mcas.setEnd(tenmin.getEnd());
           	
//explicitly store all the attributes needed for bart
    		if (ConfigProperties.getInstance().getLanguagePlugin()  instanceof EnglishLanguagePlugin) {

    			mcas.setBsentenceid(m.getMarkable().getAttributeValue("sentenceid"));
    			mcas.setBlabel(m.getMarkable().getAttributeValue("label"));
    			mcas.setBtype(m.getMarkable().getAttributeValue("type"));
    			mcas.setBlemmata(m.getMarkable().getAttributeValue("lemmata"));
       			mcas.setBisprenominal(m.getMarkable().getAttributeValue("isprenominal"));
       			mcas.setBpos(m.getMarkable().getAttributeValue("pos"));
    		}
        	mcas.setAnnotatorID(getClass().getCanonicalName());
           	mcas.addToIndexes();
           	

        }
 //       doc.saveAllLevels();
        doc.deleteAll();	//don't delete stuff for now, as it may be needed by BART
	}


}
