
/* First created by JCasGen Sun May 10 21:21:31 CEST 2015 */
package qa.qcri.qf.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Sun May 10 21:21:31 CEST 2015
 * @generated */
public class Acknowledgment_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Acknowledgment_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Acknowledgment_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Acknowledgment(addr, Acknowledgment_Type.this);
  			   Acknowledgment_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Acknowledgment(addr, Acknowledgment_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Acknowledgment.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("qa.qcri.qf.type.Acknowledgment");
 
  /** @generated */
  final Feature casFeat_ack;
  /** @generated */
  final int     casFeatCode_ack;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getAck(int addr) {
        if (featOkTst && casFeat_ack == null)
      jcas.throwFeatMissing("ack", "qa.qcri.qf.type.Acknowledgment");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_ack);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAck(int addr, boolean v) {
        if (featOkTst && casFeat_ack == null)
      jcas.throwFeatMissing("ack", "qa.qcri.qf.type.Acknowledgment");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_ack, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Acknowledgment_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_ack = jcas.getRequiredFeatureDE(casType, "ack", "uima.cas.Boolean", featOkTst);
    casFeatCode_ack  = (null == casFeat_ack) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_ack).getCode();

  }
}



    