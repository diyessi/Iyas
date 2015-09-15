package qa.qf.qcri.cqa;

import java.io.Serializable;

/**
 * Container for the values associated to a comment as in the Semevel 2015
 * community question answer task.
 * 
 * NOTE: in this case, the id includes the id of the question
 * @author albarron
 *
 */
public class CQAcomment extends CQAabstractElement implements Serializable, Comparable {

  private static final long serialVersionUID = 6131037150458266024L;

  /** Not English, good, bad... */
  private String gold;
  
  /** Whether it is a Y/N answer */
  private String gold_yn;
  
  private String prediction = "null";
  
  private double predictionScore = 0;
  
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

  public void setPrediction(String pred, double score){
    prediction = pred;
    predictionScore = score;
  }
  
  public String getPredictedClass(){
    return prediction;
  }
  
  public double getScore(){
    return predictionScore;
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

  @Override
  public int compareTo(Object anotherComment) {
    if (!(anotherComment instanceof CQAcomment))
      throw new ClassCastException("A CQAcomment expected.");
    double anotherCommentScore = ((CQAcomment) anotherComment).getScore();  
    //return this.predictionScore - anotherCommentScore;
    //TODO Check if the proper sorting is carried out
    if (this.predictionScore > anotherCommentScore) {
      return -1;
    } else if (this.predictionScore < anotherCommentScore) {
          return 1;
    } else {
      return 0;
    } 
    
  }

}
