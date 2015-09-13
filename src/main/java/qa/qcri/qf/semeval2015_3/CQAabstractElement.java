package qa.qcri.qf.semeval2015_3;

/**
 * 
 * Abstract class with common functions for handling comments and questions 
 * (generally referred to as posts) in a thread. 
 * 
 * @author albarron
 *
 */

public abstract class CQAabstractElement {

  /** Unique, global, id of the post */
  protected String id;
  
  /** Date when the post was generated */
  protected String date;
  
  /** User ID for the post */
  protected String userid;
  
  /** Subject of the post */
  protected String subject;
  
  /** Body of the post */
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
