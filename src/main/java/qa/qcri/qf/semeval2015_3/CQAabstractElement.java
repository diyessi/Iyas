package qa.qcri.qf.semeval2015_3;

public abstract class CQAabstractElement {

  /** Unique, global, id of the question */
  protected String id;
  
  /** Date when the question was generated */
  protected String date;
  
  /** User ID for the "questioner" Q */
  protected String userid;
  
  /** Subject of the question, as set by Q */
  protected String subject;
  
  /** Body of the question */
  protected String body;
  
  
  public CQAabstractElement() {
    
  }
  
  public String getId(){
    return id;
  }
  
  public String getDate() {
    return date;
  }
  
  public String getUserId() {
    return userid;
  }

  public String getSubject() {
    return subject;
  }
  
  public String getBody() {
    return body;
  }
  
  /**
   * Combines subject and body of the element considering two exclusive conditions:
   * 
   * <ul>
   * <li> if the subject and beginning of the comment are identical, the subject 
   * is discarded.
   * <li> if the body starts with upper case, subject and body are concatenated 
   * with a period. 
   * </ul>
   * @return The sum of the subject and body of an element
   */
  public String getWholeText() {
    if (body.toLowerCase().startsWith(subject.toLowerCase())) {
      return body;
    }
    if (Character.isUpperCase(body.charAt(0))) {
      return subject+ ". " + body; 
    }
    return subject+ " " + body;
  }
}
