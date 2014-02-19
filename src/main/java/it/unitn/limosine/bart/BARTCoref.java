package it.unitn.limosine.bart;

import it.unitn.limosine.emd.BARTEMD;
import it.unitn.limosine.types.coref.CorefMention;
import it.unitn.limosine.types.ner.NER;
import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.emd.EMD;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import net.cscott.jutil.DisjointSet;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import elkfed.config.ConfigProperties;
import elkfed.coref.CorefResolver;
import elkfed.coref.mentions.DefaultMentionFactory;
import elkfed.coref.mentions.Mention;
import elkfed.coref.mentions.MentionFactory;
import elkfed.coref.util.Clustering;
import elkfed.lang.EnglishLanguagePlugin;
import elkfed.main.XMLAnnotator;
import elkfed.main.xml.CorefExperimentDocument;
import elkfed.mmax.DiscourseUtils;
import elkfed.mmax.minidisc.MarkableHelper;
import elkfed.mmax.minidisc.MiniDiscourse;
import elkfed.mmax.pipeline.CoNLLClosedPipeline;
import elkfed.mmax.pipeline.Pipeline;

public class BARTCoref  extends JCasAnnotator_ImplBase{
	
	String tmpbartdatadir;
	String bartconfigdir;
    protected final MentionFactory mfact=new DefaultMentionFactory();
    protected final Pipeline pipeline = new CoNLLClosedPipeline();
    CorefExperimentDocument expxml;
    CorefResolver cr;
    ConfigProperties cp;
    
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

		/*
        InputStream tmpst=ClassLoader.getSystemResourceAsStream(bartconfigdir +
        		File.separator + "config" + File.separator +"bart4uima.xml");
        System.err.println(tmpst.toString());
        expxml=CorefExperimentDocument.Factory.parse(tmpst);
        */
	    expxml=CorefExperimentDocument.Factory.parse(new File(bartconfigdir +
                File.separator + "config" + File.separator +"bart4uima.xml"));
	       		
        cr=XMLAnnotator.createResolver(expxml.getCorefExperiment());
        
		} catch (ClassCastException e) {
			e.printStackTrace();
		}  catch (ResourceAccessException e) {
			e.printStackTrace();
    	}	catch (FileNotFoundException e) {
		e.printStackTrace();
    	}   catch (Exception e) {
			e.printStackTrace();
		}

		/* 
		catch (XmlException e) {
    		e.printStackTrace();

    	}	*/
	}

	private CorefMention Mbart2Mcoref (JCas cas, FSIndex tokensIndex, MiniDiscourse doc, Mention m) {
		
    	CorefMention mcor=new CorefMention(cas);
     	String minspan=m.getMarkable().getAttributeValue("min_ids");
// -------- set positions once again -------
       	int startext=m.getMarkable().getLeftmostDiscoursePosition();
    	int endext=m.getMarkable().getRightmostDiscoursePosition();
    	Token tstext=BARTEMD.id2token(tokensIndex,startext);	
    	Token tenext=BARTEMD.id2token(tokensIndex,endext);	
     	Token tstmin=tstext;
      	Token tenmin=tenext;
      	if (minspan!=null) {
      		String[] spanIDs = MarkableHelper.parseRange(minspan);
      		int startmin =
      				doc.getDiscoursePositionFromDiscourseElementID(spanIDs[0]);
      		int endmin =        
      				doc.getDiscoursePositionFromDiscourseElementID(spanIDs[1]);
     	
      		tstmin=BARTEMD.id2token(tokensIndex,startmin);	
      		tenmin=BARTEMD.id2token(tokensIndex,endmin);	
      	}
      	mcor.setBegin(tstmin.getBegin());
       	mcor.setEnd(tenmin.getEnd());
       return mcor;
	
	}
	
	
	private int MMAXTokenId(Token t) {
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
	
	
	private void uemd2mmax(JCas cas, MiniDiscourse doc) {

		FSIndex emdIndex = cas.getAnnotationIndex(EMD.type);
		Iterator emdIterator = emdIndex.iterator();

		int mid=0;
			
		while (emdIterator.hasNext()) {
			EMD m = (EMD) emdIterator.next();
			int startwid=MMAXTokenId(m.getStartToken());
			int endwid=MMAXTokenId(m.getEndToken());
			Map<String, String> attrs = new HashMap<String, String>();
				
			if (ConfigProperties.getInstance().getLanguagePlugin()  instanceof EnglishLanguagePlugin) {

				attrs.put("sentenceid",m.getBsentenceid());
				attrs.put("label",m.getBlabel());
				attrs.put("type",m.getBtype());
				attrs.put("lemmata",m.getBlemmata());
				attrs.put("isprenominal",m.getBisprenominal());
				attrs.put("pos",m.getBpos());
				attrs.put("min_ids", "word_"+ (MMAXTokenId(m.getStartTokenMIN())+1) + "..word_" + (MMAXTokenId(m.getEndTokenMIN())+1) );
			}
				
			doc.getMarkableLevelByName("markable").addMarkable(startwid, endwid, attrs);
			mid++;
		}

	}

	
	

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		
				
		try {	
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
		uemd2mmax(cas,doc);

//        DiscourseUtils.deleteResponses(doc);

        List<Mention> mentions=
                mfact.extractMentions(doc);
        Map<Mention,Mention> antecedents=new HashMap<Mention,Mention>();
        DisjointSet<Mention> partition=
                cr.decodeDocument(mentions,antecedents);
        //------- debug ----
        
        Clustering.addClustersToMMAX(partition,antecedents,doc);
        //----- end of debug -----
        
        
        Map<Mention, Mention> mentionMap = partition.asMap();
        Map<Mention, CorefMention> mention2setid = new HashMap<Mention,CorefMention>();
        int n_ids=0;
        
        // as UIMA only allows for fixed-size arrays, precompute all the sizes first
        
        Map<Mention, Integer> canon2chainsz = new HashMap<Mention, Integer>();
        Map<CorefMention, Integer> canon2lastid = new HashMap<CorefMention, Integer>();
        for (Mention m: mentionMap.values())
        {
        	canon2chainsz.put(m,0);
        }
        for (Mention m: mentionMap.keySet())
        {
            Mention key_m=mentionMap.get(m);
            canon2chainsz.put(key_m,canon2chainsz.get(key_m)+1);
        }      
        // first, make entries for the partition representatives only
        for (Mention m: mentionMap.values())
        {
            if (mention2setid.containsKey(m)) continue;
            int csz=canon2chainsz.get(m);
            CorefMention mcor=Mbart2Mcoref(cas,tokensIndex,doc,m);
            FSArray chain=new FSArray(cas,csz);
            chain.set(0, mcor);
            mcor.setCorefSetId(n_ids);
            mcor.setCorefSet(chain);
            mcor.addToIndexes();
            mention2setid.put(m,mcor);
        	canon2lastid.put(mcor,0);
            n_ids++;
                   
        }

        // add all other mentions to the map
        for (Mention m: mentionMap.keySet())
        {
            if (mention2setid.containsKey(m)) continue;
            CorefMention mcor=Mbart2Mcoref(cas,tokensIndex,doc,m);
            Mention key_m=mentionMap.get(m);
            CorefMention canonical=mention2setid.get(key_m);
            mention2setid.put(m,canonical);
            Integer lastid=canon2lastid.get(canonical)+1;
        	canon2lastid.put(canonical,lastid);
         	mcor.setCorefSetId(canonical.getCorefSetId());           	
         	canonical.getCorefSet().set(lastid, mcor);
            mcor.setCorefSet(canonical.getCorefSet());
         	mcor.addToIndexes();

        

        
        }

  //      doc.saveAllLevels();
       doc.deleteAll();	//don't delete stuff for now, as it may be needed by BART
        }   catch (Exception e) {
			e.printStackTrace();
		}
        
        
       
	}
}
