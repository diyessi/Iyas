

/* First created by JCasGen Sun May 10 21:26:23 CEST 2015 */
package qa.qcri.qf.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Sun May 10 21:26:23 CEST 2015
 * XML source: /home/noname/workspace/Iyas-new/desc/Iyas/UrlDescriptor.xml
 * @generated */
public class Url extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Url.class);
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
  protected Url() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Url(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Url(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Url(JCas jcas, int begin, int end) {
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
  //* Feature: uriType

  /** getter for uriType - gets Whether the URI belongs to a "url" or an "email".
   * @generated
   * @return value of the feature 
   */
  public String getUriType() {
    if (Url_Type.featOkTst && ((Url_Type)jcasType).casFeat_uriType == null)
      jcasType.jcas.throwFeatMissing("uriType", "qa.qcri.qf.type.Url");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Url_Type)jcasType).casFeatCode_uriType);}
    
  /** setter for uriType - sets Whether the URI belongs to a "url" or an "email". 
   * @generated
   * @param v value to set into the feature 
   */
  public void setUriType(String v) {
    if (Url_Type.featOkTst && ((Url_Type)jcasType).casFeat_uriType == null)
      jcasType.jcas.throwFeatMissing("uriType", "qa.qcri.qf.type.Url");
    jcasType.ll_cas.ll_setStringValue(addr, ((Url_Type)jcasType).casFeatCode_uriType, v);}    
  }

    