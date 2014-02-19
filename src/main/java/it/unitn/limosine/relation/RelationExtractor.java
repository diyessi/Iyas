package it.unitn.limosine.relation;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import limo.core.Mention;
import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IRelation;

import it.unitn.limosine.types.emd.EMD;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;
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


public class RelationExtractor extends JCasAnnotator_ImplBase {

 	private String command;
	
	private HashMap<String,String> semanticTypeMapping;
	
	private boolean debug = true;
	
	private String model;
	
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		try {

			// init relation extractor
			SharedModel sharedModel = (SharedModel) getContext().getResourceObject("RunRelationExtractor");
			model = "ace";
			
			if (sharedModel == null) {
				sharedModel = (SharedModel) getContext().getResourceObject("RunRelationExtractorConll04");
				model = "conll04";
			}
			
			if (sharedModel == null) {
				sharedModel = (SharedModel) getContext().getResourceObject("RunRelationExtractorConll04unlex");
				model = "conll04unlex";
			}

			getContext().getLogger().log(Level.INFO,
					"Starting relation extractor...");

			String processCmd = sharedModel.getPath();

			command = processCmd; 
			
			getContext().getLogger().log(Level.INFO, processCmd);

			//process = new ProcessBuilder(processCmd).start();  //run it for each doc
			
			//init mapping: from Bart semanticTypes to 3 Stanford NER types: PERSON, LOCATION, ORGANIZATION
			//PERSON, MALE, FEMALE, OBJECT, ORGANIZATION, LOCATION, DATE, TIME, MONEY, PERCENT, GPE, UNKNOWN, EVENT;  
			semanticTypeMapping = new HashMap<String,String>();
			semanticTypeMapping.put("PERSON", "PERSON");
			semanticTypeMapping.put("MALE", "PERSON");
			semanticTypeMapping.put("FEMALE", "PERSON");
			semanticTypeMapping.put("OBJECT", "ENTITY");
			semanticTypeMapping.put("UNKNOWN", "ENTITY");
			semanticTypeMapping.put("ORGANIZATION", "ORGANIZATION");
			semanticTypeMapping.put("LOCATION", "LOCATION");
			semanticTypeMapping.put("GPE", "LOCATION");  //everything not in here is mapped to ENTITY
			
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		//the relation extractor needs input from 2 sources:
		// - constituent trees
		// - entities mentions 

		getContext().getLogger().log(Level.INFO,
				"Processing...");

		FSIndex<Annotation> parseTrees = jcas.getAnnotationIndex(ConstituencyTree.type);
		Iterator<Annotation> parseTreeIterator = parseTrees.iterator(); 
		
		//key: line+tree, value: mentions in sentence
		ArrayList<Instance> inputForClassifier = new ArrayList<Instance>();
		ArrayList<List<AnnotationFS>> sentenceMentions = new ArrayList<List<AnnotationFS>>();
		
		int sentenceIndex=0;
		
		//first get all sentences containing at least 2 mentions
		while (parseTreeIterator.hasNext()) {
			 ConstituencyTree parseTree = (ConstituencyTree) parseTreeIterator.next();
			 
			 // mentions can be overlapping. check this!
			 List<AnnotationFS> mentions = JCasUtility.selectCovered(jcas, EMD.class, parseTree); 
			 List<AnnotationFS> tokens = JCasUtility.selectCovered(jcas, Token.class, parseTree);
			 
			 // check head-word: if two mentions have the same, ignore one
			 for (int i=0; i < mentions.size(); i++) {
				 for (int j=1; j < mentions.size(); j++) {
					 if (j!=i) {
						 EMD m1 = (EMD)mentions.get(i);
						 EMD m2 = (EMD)mentions.get(j);
						 
						 if (m1.getHeadToken() == null) {
							 System.err.println("missing head token for: "+m1.getCoveredText());
							 continue;
						 }
						 if (m2.getHeadToken() == null) {
							 System.err.println("missing head token for: "+m2.getCoveredText());
							 continue;
						 }
						 if (m1.getHeadToken().getBegin() == m2.getHeadToken().getBegin() &&
								 m1.getHeadToken().getEnd() == m2.getHeadToken().getEnd()) {
							 
							 if (debug == true) {
								 System.err.println("remove: "+m2.getHeadToken().getCoveredText());
								 System.err.println("same head: "+m1.getCoveredText() + " <> "+m2.getCoveredText());
							 }
							 mentions.remove(j);
						 }
					 }
				 }
			 }
			 
			 String taggedSentence = null;
			 
			 if (mentions.size() >=2) {
				 
				 String tree = parseTree.getRawParse();
			
				 for (int i=0; i < mentions.size(); i++) {
					 for (int j=i+1; j < mentions.size(); j++) {
						
						 EMD mention1 = (EMD)mentions.get(i);
						 EMD mention2 = (EMD)mentions.get(j);
						 
						 if (mention1.getHeadToken() ==null || mention2.getHeadToken() ==null) {
							 System.err.println("Head token of mention was null!");
							 continue;
						 }
						
						 
						 if (mention1.getHeadToken().getBegin() > mention2.getHeadToken().getBegin()) {
							 //exchange order
							 mention1 = (EMD)mentions.get(j);
							 mention2 = (EMD)mentions.get(i);
						 }
						
						 //if (mention1.equals(mention2))
							// continue;  // this is an EMD not RE mention, thus didn't work like this! added check above
						
						 
						 boolean useOnlyHead = false;
						 //check if they overlap -- then take only head
						 if (overlap(mention1,mention2))
							 useOnlyHead = true;
						 
						 List<AnnotationFS> twomentions = new ArrayList<AnnotationFS>();
						 twomentions.add(mention1);
						 twomentions.add(mention2);
						 taggedSentence = createEMDTaggedSentence(tokens, twomentions, useOnlyHead);
						 String input = sentenceIndex + "\t" + taggedSentence + "\t" + tree;
						 
						 
						 inputForClassifier.add(new Instance(sentenceIndex, input, twomentions));
						 
						 //keep mentionsFS
						 
						 if (sentenceMentions.size()>0 && (sentenceMentions.size()-1)==sentenceIndex) {
							 //update
							 List<AnnotationFS> sm = sentenceMentions.get(sentenceIndex);
							 sm.addAll(twomentions);
						 } else {
							 sentenceMentions.add(twomentions);
						 }
						 
						
						 
					 }
				 }
				 sentenceIndex++;
			 } 
			 
		}
		// now run classifier 
			try {
			 
			if (inputForClassifier.size()>0) {
				
				getContext().getLogger().log(Level.INFO,
						"Writing to classifier...");
				
					//for now run it for every sentence that contains at least 2 mentions
					Process process = new ProcessBuilder(command).start();
					
					PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream())),true);
				
					for (int i =0; i < inputForClassifier.size(); i++) {
						Instance instance = inputForClassifier.get(i);
						writer.println(instance.getClassifierInput());
						if (debug==true) 
							System.out.println(i + " Writing to classifier: " + instance.getClassifierInput() + " id: "+instance.instanceId);
					}
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
         
                    if (debug==true) {
                    	for(String out : outGobbler.getOuput()) 
                    		System.err.println(out.trim());
                    }
                    
                    List<String> output = outGobbler.getOuput();
                    List<String> outputErr = errGobbler.getOuput();
                    
                    if (debug ==true) {
                    for(String out : outputErr) 
                    	System.err.println(out.trim());
                    
                    	System.err.println("RETURNED:");
                    	for(String out : output) 
                    		System.err.println(out.trim());
                    }
                    sentenceIndex=0;
                    
                    for(String out : output) {
                    	//System.out.println(sentenceIndex + " from classifier: \n" +out);
                  
                    	Sentence s = Sentence.createSentenceFromRelationTaggedInput(sentenceIndex, out);
    					
    					Relations relations = s.getRelations();
    					for (IRelation ir : relations.getRelations()) {
    						Relation relation = (Relation)ir;
    						List<AnnotationFS> mentions = sentenceMentions.get(sentenceIndex);
    						
    						//for (AnnotationFS a : mentions)
    						//	System.out.println(a.getCoveredText());
    						
    						it.unitn.limosine.types.relation.Relation rel = new it.unitn.limosine.types.relation.Relation(jcas);
    					
    						EMD arg1 = (EMD)findMention(relation.getFirstMention(), mentions);
    						EMD arg2 = (EMD)findMention(relation.getSecondMention(), mentions);

    						
    						// PERSON, MALE, FEMALE, OBJECT, ORGANIZATION, LOCATION, DATE, TIME, MONEY, PERCENT, GPE, UNKNOWN, EVENT;
    						
    						if (arg1 == null || arg2 == null) {
    							System.err.println("Could not find mention of relation..");
    							continue;
    							
    						}
    							
    						//FSArray arguments = rel.getArguments();
    						//arguments.s
    						//rel.setArguments(0, arg1);
    						//rel.setArguments(1,arg2);
    						rel.setMention1(arg1);
    						rel.setMention2(arg2);
    						
    						rel.setRelationType(relation.getRelationType());
    					
    						//set begin/end of relation (otherwise not shown in document Analyzer!)
    						rel.setBegin(arg1.getBegin() < arg2.getBegin() ? arg1.getBegin() : arg2.getBegin());
    						rel.setEnd(arg1.getEnd() > arg2.getEnd() ? arg1.getEnd() : arg2.getEnd());
    						
//    						rel.setAnnotatorId(getClass().getCanonicalName());
	    						
       						rel.setAnnotatorId(getClass().getCanonicalName()+":"+model);
	    						
       					    rel.addToIndexes(jcas);
    						
    						
    						/* just tried with array...
    						   
    						RelationMention relmention = new RelationMention(jcas);
    						FSArray relationMentions = new FSArray(jcas, 2);
    						relationMentions.set(0, arg1);
    						relationMentions.set(1, arg2);
    						
    						relmention.setMentions(relationMentions);
    						relmention.addToIndexes(jcas);*/
    						
    						//System.out.println(relation);
    					}
    					
    					  sentenceIndex++;
					}
                  
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	
	//check if they overlap
	private boolean overlap(EMD mention1, EMD mention2) {
		int m1start = mention1.getStartTokenMIN().getBegin();
		int m1end = mention1.getEndTokenMIN().getEnd();
		int m2start = mention2.getStartTokenMIN().getBegin();
		int m2end = mention2.getEndTokenMIN().getEnd();
		
		if (m2start < m1start && m1start < m2end)
			return true;
		else if (m2start < m1end && m1end < m2end)
			return true;
		else if (m1start < m2start && m2start < m2end)
			return true;
		else if (m1start < m2end && m2end < m1start) 
			return true;
		return false;
	}


	//uses headtoken to find mention
	private EMD findMention(Mention relExMention, List<AnnotationFS> mentions) throws Exception {
		int[] findIds = relExMention.getTokenIds();
		for (AnnotationFS m: mentions) {
			EMD mention = (EMD) m;
			//System.out.println(mention);
			int mentionHeadStart = mention.getHeadToken().getTokenSentId();
			int mentionHeadEnd = mention.getHeadToken().getTokenSentId();
			if (mentionHeadStart == findIds[0] && mentionHeadEnd == findIds[findIds.length-1]) 
				return mention;
			//if (mention.getCoveredText().equals(relExMention.getHead()))
			//	return mention;
		}
		//throw new Exception("mention not found! " + relExMention.getHead());
		return null;
	}
	
	//private static final String SEPARATOR = ".";
	
	private String createEMDTaggedSentence(List<AnnotationFS> tokens, List<AnnotationFS> mentions, boolean useOnlyHead) {
		
		//assumes that mentions are in right order
		int i=0;
		EMD mention = null;
		if (mentions.size()>0)
			mention = (EMD)mentions.get(i); //get first
		
		StringBuilder sb = new StringBuilder();
		boolean foundEMD = false;
		
		for (AnnotationFS tokenFS : tokens) {
			Token token = (Token) tokenFS;
			
			if (mention == null) {
				sb.append(token.getStanfordNormalizedText() + "/O ");
				continue;
			}
			if (!useOnlyHead) {
				if (mention.getStartTokenMIN().equals(token) && mention.getEndTokenMIN().equals(token)) {
					sb.append(getAnnotation(token, mention,"B")); //begin token
					//get next mention
					i++;
					if (i < mentions.size())
						mention =  (EMD)mentions.get(i); //get next
					else
						mention = null;
					//reset
					foundEMD = false;
				}
				else if (mention.getStartTokenMIN().equals(token)) {
					foundEMD = true;
					sb.append(getAnnotation(token, mention,"B"));
				}  
				else if (mention.getEndTokenMIN().equals(token)) {
					sb.append(getAnnotation(token, mention,"I")); //inside
					foundEMD=false; //reset
					i++;
					if (i < mentions.size())
						mention =  (EMD)mentions.get(i); //get next
					else
						mention = null;
				} else {
					if (foundEMD)
						sb.append(getAnnotation(token, mention,"I"));
					else
						sb.append(token.getStanfordNormalizedText() + "/O ");
				}
			} //else if useOnlyHead 
			else {
				//TODO: extend to headTokenS
				if (mention.getHeadToken().getTokenSentId() == token.getTokenSentId()) { // for now assumes headToken is just 1 
					sb.append(getAnnotation(token, mention,"B"));
					i++;
					if (i < mentions.size())
						mention =  (EMD)mentions.get(i); //get next
					else
						mention = null;
				}
				else
					sb.append(token.getStanfordNormalizedText() + "/O ");
			}
		}
		
		return sb.toString().trim();
	}


	// use semanticEntityType
	private String getAnnotation(Token token, EMD mention, String prefix) {
		//String entityType = mention.getEntityType();
		String semanticEntityType = mention.getSemanticType();
		if (semanticTypeMapping.containsKey(semanticEntityType))
			semanticEntityType = semanticTypeMapping.get(semanticEntityType);
		//return token.getStanfordNormalizedText() + "/"+ semanticEntityType + SEPARATOR + mention.getACEtype() + " ";
		return token.getStanfordNormalizedText() + "/"+ prefix + "-"+ semanticEntityType + " ";
	}


	// private class to hold information
	private class Instance {
		int instanceId;
		String classifierInput;
		List<AnnotationFS> mentions;
		
		public Instance(int instanceIndex, String input,
			List<AnnotationFS> mentions) {
			this.instanceId = instanceIndex;
			this.classifierInput=input;
			this.mentions = mentions;
		}

		public List<AnnotationFS> getMentions() {
			return mentions;
		}

		public String getClassifierInput() {
			return this.classifierInput;
		}
		
		public int getInstanceId() {
			return this.instanceId;
		}
		
	}

}


