package qa.qcri.qf.semeval2015_3;

import java.io.Serializable;

/**
 * Container for the values associated to a comment as in the Semevel 2015
 * community question answer task.
 * 
 * NOTE: in this case, the id includes the id of the question
 * @author albarron
 *
 */
public class CQAcomment extends CQAabstractElement implements Serializable {

  private static final long serialVersionUID = 6131037150458266024L;

  /** Not English, good, bad... */
  private String gold;
  
  /** Whether it is a Y/N answer */
  private String gold_yn;
  
  public CQAcomment(String cid, String cuserid, String cGold, String cGoldYN, 
          String cSubject, String cBody){
    this.id = cid;
    this.userid = cuserid;
    this.gold = cGold;
    this.gold_yn = cGoldYN;
    this.subject = cSubject;
    this.body = cBody;
  }

  //TODO the date is missing in this case!!!
  
  public String getGold() {
    return gold;
  }

  public String getGold_yn() {
    return gold_yn;
  }
  
  public void setGold(String cGold){
    this.gold = cGold;
  }

  /*   /**
   * Previous to the operations in the abstract class, the following two rules are 
   * (potentially) applied...
   * 
   * <ul>
   * <li> if the subject starts with "re:", the subject is discarded
   * <li> if the body is empty, it is discarded. 
   * </ul>
   * @return The sum of the subject and body of an element
   * (non-Javadoc)
   * @see qa.qcri.qf.semeval2015_3.CQAabstractElement#getWholeText()
   */
  @Override
  public String getWholeText() {
    if (subject.toLowerCase().startsWith("re:")) {
      return body;
    }    
    if (body.length()==0){
      return subject;
    }
    return super.getWholeText();
  }

}
