

/* First created by JCasGen Wed Feb 25 15:52:25 AST 2015 */
package qa.qcri.qf.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Feb 25 15:52:25 AST 2015
 * XML source: /home/alberto/Iyas/desc/Iyas/HeuristicsDescriptor.xml
 * @generated */
public class Email extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Email.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Email() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Email(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Email(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Email(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: email

  /** getter for email - gets whether the text contains an email
   * @generated
   * @return value of the feature 
   */
  public boolean getEmail() {
    if (Email_Type.featOkTst && ((Email_Type)jcasType).casFeat_email == null)
      jcasType.jcas.throwFeatMissing("email", "qa.qcri.qf.type.Email");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((Email_Type)jcasType).casFeatCode_email);}
    
  /** setter for email - sets whether the text contains an email 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEmail(boolean v) {
    if (Email_Type.featOkTst && ((Email_Type)jcasType).casFeat_email == null)
      jcasType.jcas.throwFeatMissing("email", "qa.qcri.qf.type.Email");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((Email_Type)jcasType).casFeatCode_email, v);}    
  }

    