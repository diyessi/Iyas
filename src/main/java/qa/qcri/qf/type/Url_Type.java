
/* First created by JCasGen Wed Feb 25 14:38:58 AST 2015 */
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
 * Updated by JCasGen Thu Feb 26 11:59:08 AST 2015
 * @generated */
public class Url_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Url_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Url_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Url(addr, Url_Type.this);
  			   Url_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Url(addr, Url_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Url.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("qa.qcri.qf.type.Url");
 
  /** @generated */
  final Feature casFeat_uriType;
  /** @generated */
  final int     casFeatCode_uriType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getUriType(int addr) {
        if (featOkTst && casFeat_uriType == null)
      jcas.throwFeatMissing("uriType", "qa.qcri.qf.type.Url");
    return ll_cas.ll_getStringValue(addr, casFeatCode_uriType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setUriType(int addr, String v) {
        if (featOkTst && casFeat_uriType == null)
      jcas.throwFeatMissing("uriType", "qa.qcri.qf.type.Url");
    ll_cas.ll_setStringValue(addr, casFeatCode_uriType, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Url_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_uriType = jcas.getRequiredFeatureDE(casType, "uriType", "uima.cas.String", featOkTst);
    casFeatCode_uriType  = (null == casFeat_uriType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_uriType).getCode();

  }
}



    