
/* First created by JCasGen Wed Dec 31 15:35:45 AST 2014 */
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
 * Updated by JCasGen Sun Mar 22 10:04:46 AST 2015
 * @generated */
public class QuestionFocus_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (QuestionFocus_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = QuestionFocus_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new QuestionFocus(addr, QuestionFocus_Type.this);
  			   QuestionFocus_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new QuestionFocus(addr, QuestionFocus_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = QuestionFocus.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("qa.qcri.qf.type.QuestionFocus");
 
  /** @generated */
  final Feature casFeat_Focus;
  /** @generated */
  final int     casFeatCode_Focus;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getFocus(int addr) {
        if (featOkTst && casFeat_Focus == null)
      jcas.throwFeatMissing("Focus", "qa.qcri.qf.type.QuestionFocus");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Focus);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFocus(int addr, int v) {
        if (featOkTst && casFeat_Focus == null)
      jcas.throwFeatMissing("Focus", "qa.qcri.qf.type.QuestionFocus");
    ll_cas.ll_setRefValue(addr, casFeatCode_Focus, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public QuestionFocus_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Focus = jcas.getRequiredFeatureDE(casType, "Focus", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token", featOkTst);
    casFeatCode_Focus  = (null == casFeat_Focus) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Focus).getCode();

  }
}



    