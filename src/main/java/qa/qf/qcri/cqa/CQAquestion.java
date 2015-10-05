package qa.qf.qcri.cqa;

/**
 * A container for all the fields of a question in the Semeval
 * 2015 community question answering setting. This question
 * is intended to be part of a cQAinstance.
 * 
 * @see CQAinstance
 * 
 * @author albarron
 * @since 0.1
 * @version 0.1
 */
public class CQAquestion extends CQAabstractElement {
  
  //TODO candidates to be moved to the instance from here
  /** Type of question (GENERAL, YES_NO) */
  private String type;  
  /** Whether the expected answer to the question is of type YES/NO */
  private String gold_yn;
  //TODO candidates to be moved to the instance up to here
  

  
  /**
   * Invoke the class setting all the values for the question (but not the 
   * comments).
   *  
   * @param qid
   * @param qdate
   * @param quserid
   * @param qtype
   * @param qgold_yn
   * @param qsubject
   * @param qbody
   */
  public CQAquestion(String qid,  String qdate, String quserid,
      String qtype, String qgold_yn, String qsubject, String qbody) {
    setQid(qid);    
    setQdate(qdate);
    setQuserId(quserid);
    setQtype(qtype);
    setQgoldYN(qgold_yn);   
    setQsubject(qsubject);
    setQbody(qbody); 
  }
 
  /* Getters */

  public String getType() {
    return type;
  }
  
  public String getGold_yn() {
    return gold_yn;    
  }

  public String getQuestionText(String questionSubject, String questionBody){
    
    if(questionBody.toLowerCase().startsWith(questionSubject.toLowerCase())){
      return questionBody;
    }
    if(Character.isUpperCase(questionBody.charAt(0))){
      return questionSubject+ ". " + questionBody; 
    }
    return questionSubject+ " " + questionBody; 
    
  }
  
  
  /* Setters */
  
  private void setQid(String id) {
    this.id = id;   
  }
  
 
  private void setQdate(String date) {
    this.date = date;
  }
  
  private void setQuserId(String user) {
    this.userid = user;
  }
  
  //XXX public settes temprarilly added for compatibility with Hamdy's software
  public void setQtype(String type) {
    this.type = type;
  }

  public void setQgoldYN(String yn) {
    this.gold_yn = yn;    
  }
  
  private void setQsubject(String subject) {
    this.subject = subject;
  }

  private void setQbody(String body) {
    this.body = body;
  }
  
//  public void setType(String type){
//    this.type = type;
//    
//  }

//  public void setGoldYN(String yn){
//    gold_yn = yn;    
//  }
  
}
