package qa.qcri.qf.semeval2015_3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A simple class that includes all the information related to Community 
 * Question Answering question and its associated thread of comments as in the
 * Semeval 2015 task 3 on answer selection.
 * 
 * <p>
 * The class implements comparable in order to allow for the creation of a 
 * TreeSet of cQA instances. The instances can then be sorted by QID.
 * </p>
 * 
 * TODO check that the sorting is actually working. 
 * TODO Junit
 * 
 * @author albarron
 *
 */
public class CQAinstance implements Comparable<CQAinstance>,  Serializable {
	
	private static final long serialVersionUID = 7155250601309427542L;
	
	/** Category (Advice and help, Computers and Internet, etc.) */
  private String category;
  
	
	public static final String THANKS = "thanks";
	public static final String NOTHANKS = "nothanks";
	public static final String HASQ = "hasq";
	public static final String NOHASQ = "nohasq";
	
	public static final String NOT_ENGLISH = "Not English"; 
	public static final String GOOD = "Good"; 
	public static final String POTENTIAL = "Potential"; 
	public static final String DIALOGUE = "Dialogue"; 
	public static final String BAD = "Bad";
	
	private CQAquestion question; 
		
	/** Ordered list of the comments; potential answers to the question */
	private List<CQAcomment> comments;
	
	/**
	 * Invoke the class without setting any value. Comments list and question are 
	 * initialized.
	 */
	public CQAinstance(CQAquestion question, String cat){
	  this.question = question;
		comments = new ArrayList<CQAcomment>();
		
		//TODO maybe both question and comment should have their own methods to be produced?
		setQcategory(cat);
	}

	/**
	 * Add a new comment
	 * @param cid comment id: (includes QID)
	 * @param cuserid	user id
	 * @param cGold	Not English, good, ...
	 * @param cGoldYN Whether it is a Y/N answer
	 * @param cSubject	Subject of the comment
	 * @param cBody	Comment
	 */
	public void addComment(String cid, String cuserid, String cGold, String cGoldYN, 
							String cSubject, String cBody){
		comments.add(new CQAcomment(cid, cuserid, cGold, cGoldYN, 
							cSubject, cBody));		
	}	

	public boolean hasGoodComments(){
		for (CQAcomment comment : comments){
			if (comment.getGold().equals(GOOD))
				return true;
		}
		return false;
	}
	
	public boolean hasGoodCommentsByQ(){
		for (CQAcomment comment : comments) {
			if (comment.getGold().equals(GOOD) &&
			    comment.getUserId().equals(question.getUserId()))
					return true;			
		}
		return false;		
	}	
	
	public List<CQAcomment> getComments(){
		return comments;
	}
	
	/** @return number of comments associated to this question */
	public int size(){
		return comments.size();
	}
	
	public Map<Integer, CQAcomment> getCommentsOfType(String type){		
		Map<Integer, CQAcomment> mComments = new TreeMap<Integer, CQAcomment>();		
		List<CQAcomment> lComments = getComments();
		//Iterator<cQAcomment> it = lComments.iterator();
		int i=1;
	    for (CQAcomment comment : lComments) {	        	        
	        if (comment.getGold().equals(type)) {
	        	mComments.put(i, comment);
	        }
	        i++;        
	    }
		return mComments;		
	}
	

	
	public Map<Integer, CQAcomment> getCommentsByQ(String type){
		boolean remove = false;
		Map<Integer, CQAcomment> qComments = getCommentsByQ();
		
		Iterator<Entry<Integer, CQAcomment>> it = qComments.entrySet().iterator();
		while (it.hasNext()) {
		  remove = false;
		  Map.Entry<Integer, CQAcomment> pairs = (Entry<Integer, CQAcomment>) it.next();
		  switch(type){
  		  case THANKS:
  		    //TODO maybe move to body+header?
  		    remove = (! FeatureExtractor.containsAcknowledge(
  		        pairs.getValue().getBody())) ? true : false;
  		    break;
  		  case NOTHANKS:
  		    remove = (FeatureExtractor.containsAcknowledge(
  		        pairs.getValue().getBody())) ? true : false;
  		        //((cQAcomment) pairs.getValue()).getCbody())) ? true : false;
  		    break;
  		  case HASQ:
  		    remove = (! FeatureExtractor.containsQuestion(
  		        pairs.getValue().getBody())) ? true : false;
  		    break;
  		  case NOHASQ: 	
  		    remove = (FeatureExtractor.containsQuestion(
  		        pairs.getValue().getBody())) ? true : false;
  		    break;				
		  }	        
		  if (remove) {
		    it.remove();
		  }	        
		}
		return qComments;
	}
		
	public Map<Integer, CQAcomment> getCommentsByQThanks(){
		Map<Integer, CQAcomment> qComments = getCommentsByQ();
		
		Iterator<Entry<Integer, CQAcomment>> it = qComments.entrySet().iterator();
	    while (it.hasNext()) {	    	
	        Map.Entry<Integer, CQAcomment> pairs = (Entry<Integer, CQAcomment>) it.next();
	        //if it does not have aks, remove it from the map to be returned
	        if (! FeatureExtractor.containsAcknowledge(
	        		pairs.getValue().getBody())){
	        	it.remove();
	        }	        
	    }
	    return qComments;		 
	}
	
	public Map<Integer, CQAcomment> getCommentsByQNoThanks(){
		Map<Integer, CQAcomment> qComments = getCommentsByQ();
		
		Iterator<Entry<Integer, CQAcomment>> it = qComments.entrySet().iterator();
	    while (it.hasNext()) {	    	
	        Map.Entry<Integer, CQAcomment> pairs = (Entry<Integer, CQAcomment>) it.next();
	        if (FeatureExtractor.containsAcknowledge(
	        		pairs.getValue().getBody())){
	        	it.remove();
	        }	        
	    }
	    return qComments;		 
	}

	/**
	 * @return all the comments in the thread authored by u_q, indexed by
   * position in the thread. Empty if none.
	 */
	public Map<Integer, CQAcomment> getCommentsByQ(){
	  return getCommentsByUser(question.getUserId());
	}

	/**
	 * @param userid identifier of the user of interest
	 * @return all the comments in the thread authored by the given user, indexed
	 * by position in the thread. Empty if none.
	 * 
	 */
	//TODO this could be moved to just a list of int, where int is the position
  //of the the comment in the thred
	public Map<Integer, CQAcomment> getCommentsByUser(String userid) {
	  Map<Integer, CQAcomment> comms = new TreeMap<Integer, CQAcomment>();
    int i =1;
    for (CQAcomment comment : comments) {      
      if (comment.getUserId().equals(userid)) {
        comms.put(i, comment);
      }
      i++;      
    }
    return comms;
	}
	
	public int getNumberOfComments(){
		return comments.size();
	}
	
	public int getNumberOfCommentsType(String type){
		int i = 0;
		for (CQAcomment comment : comments){
			if (comment.getGold().equals(type)){
				i++;
			}			
		}
		return i;
	}
  
	public String getCategory() {
    return category;
  }
	
	public void setQcategory(String category) {
	  this.category = category;
	}

	private int getMethodToSort(){
		return Integer.valueOf(question.getId().replaceAll("[a-zA-Z]", ""));
	}

	@Override
	public int compareTo(CQAinstance o) {
		if (getMethodToSort() > o.getMethodToSort()) {
		      return 1;
		    } else if (getMethodToSort() < o.getMethodToSort()) {
		      return -1;
		    }  
		    return 0;
	}

	
}
