package qa.qf.qcri.cqa;

import java.io.Serializable;
import java.util.Map;

import qa.qcri.qf.semeval2015_3.textnormalization.JsoupUtils;
import qa.qcri.qf.semeval2015_3.textnormalization.TextNormalizer;
import qa.qcri.qf.semeval2015_3.textnormalization.UserProfile;

/**
 * 
 * Abstract class with common functions for handling comments and questions 
 * (generally referred to as posts) in a thread. 
 * 
 * @author albarron
 *
 */

public abstract class CQAabstractElement implements Serializable{

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
  
  /* Setters */
  
  public void setId(String id){
    this.id = id;   
  }
  
  public void setDate(String date){
    this.date = date;
  }
  
  public void setUserId(String user){
    userid = user;
  }
  

  
  public void setSubject(String subject){
    this.subject = subject;
  }

  public void setBody(String body){
    this.body = body;
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
    return concatSubjectAndBody(subject,  body);
  }
  
  /**
   * Text is normalised and potential signatures removed
   * TODO potentually a uima annotator
   * @param userProfiles
   * @return
   */
  public String getWholeTextNormalized(Map<String, UserProfile> userProfiles) {
    String qsubject = TextNormalizer.normalize(subject);
    String qbody = JsoupUtils.recoverOriginalText(
        UserProfile.removeSignature(body, userProfiles.get(userid)));
    qbody = TextNormalizer.normalize(qbody);
    //      questionCategories.add(qcategory);
    return concatSubjectAndBody(qsubject,  qbody);
  }
  
  private String concatSubjectAndBody(String s, String b) {
    if (b.toLowerCase().startsWith(s.toLowerCase())) {
      return b;
    }
    if (b.length() > 0) {
      if (Character.isUpperCase(b.charAt(0))) {
        return s+ ". " + b; 
      }
    }
    return s+ " " + b;
  }
  
  
}
