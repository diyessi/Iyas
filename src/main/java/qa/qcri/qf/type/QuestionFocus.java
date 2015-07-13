

/* First created by JCasGen Wed Dec 31 15:35:45 AST 2014 */
package qa.qcri.qf.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;


/** 
 * Updated by JCasGen Sun Mar 22 10:04:46 AST 2015
 * XML source: /Users/albarron/Iyas/desc/Iyas/QuestionFocus.xml
 * @generated */
public class QuestionFocus extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(QuestionFocus.class);
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
  protected QuestionFocus() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public QuestionFocus(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public QuestionFocus(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public QuestionFocus(JCas jcas, int begin, int end) {
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
  //* Feature: Focus

  /** getter for Focus - gets 
   * @generated
   * @return value of the feature 
   */
  public Token getFocus() {
    if (QuestionFocus_Type.featOkTst && ((QuestionFocus_Type)jcasType).casFeat_Focus == null)
      jcasType.jcas.throwFeatMissing("Focus", "qa.qcri.qf.type.QuestionFocus");
    return (Token)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((QuestionFocus_Type)jcasType).casFeatCode_Focus)));}
    
  /** setter for Focus - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFocus(Token v) {
    if (QuestionFocus_Type.featOkTst && ((QuestionFocus_Type)jcasType).casFeat_Focus == null)
      jcasType.jcas.throwFeatMissing("Focus", "qa.qcri.qf.type.QuestionFocus");
    jcasType.ll_cas.ll_setRefValue(addr, ((QuestionFocus_Type)jcasType).casFeatCode_Focus, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    