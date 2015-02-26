
/* First created by JCasGen Wed Feb 25 15:52:25 AST 2015 */
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
 * Updated by JCasGen Wed Feb 25 15:52:25 AST 2015
 * @generated */
public class Email_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Email_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Email_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Email(addr, Email_Type.this);
  			   Email_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Email(addr, Email_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Email.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("qa.qcri.qf.type.Email");
 
  /** @generated */
  final Feature casFeat_email;
  /** @generated */
  final int     casFeatCode_email;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getEmail(int addr) {
        if (featOkTst && casFeat_email == null)
      jcas.throwFeatMissing("email", "qa.qcri.qf.type.Email");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_email);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEmail(int addr, boolean v) {
        if (featOkTst && casFeat_email == null)
      jcas.throwFeatMissing("email", "qa.qcri.qf.type.Email");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_email, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Email_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_email = jcas.getRequiredFeatureDE(casType, "email", "uima.cas.Boolean", featOkTst);
    casFeatCode_email  = (null == casFeat_email) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_email).getCode();

  }
}



    