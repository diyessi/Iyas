package qa.qcri.qf.cQAdemo;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import qa.qcri.qf.emnlp2015.SubjectBodyAggregator;
import qa.qcri.qf.semeval2015_3.FeatureExtractor;
import qa.qcri.qf.semeval2015_3.PairFeatureFactoryEnglish;
import qa.qcri.qf.semeval2015_3.Question;
import qa.qcri.qf.semeval2015_3.Question.Comment;
import qa.qcri.qf.semeval2015_3.textnormalization.JsoupUtils;
import qa.qcri.qf.semeval2015_3.textnormalization.TextNormalizer;
import qa.qcri.qf.semeval2015_3.textnormalization.UserProfile;
import qa.qcri.qf.trees.TreeSerializer;
import cc.mallet.types.AugmentableFeatureVector;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * THIS IS A BACKUP OF THE CLASS TO COMPUTE THE FEATURES. IT USES THE OLD 
 * IMPLEMENTATIONS OF THE QUESTIONS.
 *
 * @author albarron
 *
 */
public class CommentSelectionDatasetCreatorBACK 
    extends qa.qcri.qf.emnlp2015.CommentSelectionDatasetCreator{

  /** */
  public static final boolean WRITE_FEATURES_TO_FILE = true;

  /** Add the meaning */
  private boolean firstRow;
  
  //TODO ABC, SEP 9. Confirm why these variables are global (but not defined)
  //TODO ABC, SEP 10. Why do we need this global if it is locally defined in 
  //processEnglishFile?
  private JCas questionCas; //added by Giovanni
  private String suffix; //added by Giovanni
  private TreeSerializer ts; //added by Giovanni
  private String plainTextOutputPath;//added by Giovanni
  private String goodVSbadOutputPath;//added by Giovanni
  private String pairwiseOutputPath;//added by Giovanni
  private String kelpFilePath;//added by Giovanni

  public CommentSelectionDatasetCreatorBACK() {
    super();
    this.firstRow = true;
  }

  public static void main(String[] args) throws IOException, UIMAException, 
      SimilarityException {
    /**
     * Setup logger
     */
    org.apache.log4j.BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.INFO);
    
    /**
     * Run the code for the English tasks
     */
    new CommentSelectionDatasetCreatorBACK().runForEnglish();
  }

  //FIXME ABC, Sep 10. Gio: this function is used, but returns null as 
  //setFileSuffix is never used. Either remove these two methods and make
  //suffix a parameter in the necessary method or actually use the setter at 
  //construction time
  private String getFileSuffix() {
    return this.suffix;
  }
  
  //TODO ABC, Sep 9. Gio: this is never used and returns the parameter. 
  //Is there some reason to maintain it?
  private void setFileSuffix(String suffix) {
    this.suffix = suffix;
  }
  

  

  
  private void writeFeaturesToFile(AugmentableFeatureVector fv, JCas commentCas,  
      String qid, String cid, String cgold, String cgold_yn) {
    
    List<Double> features = this.serializeFv(fv);

    if (this.firstRow) {
      //header for Good vs Bad
      this.fm.write(this.goodVSbadOutputPath, "qid,cgold,cgold_yn");
      for(int i = 0; i < fv.numLocations(); i++) {
        int featureIndex = i + 1;
        this.fm.write(this.goodVSbadOutputPath, ",f" + featureIndex);
      }
      this.fm.writeLn(this.goodVSbadOutputPath, "");
      
      //header for pairwise
      this.fm.write(this.pairwiseOutputPath, "qid,cgold");
      int numFeatures = fv.numLocations();
      if (COMBINATION_CONCAT){
        numFeatures *=2;
      }
      if (INCLUDE_SIMILARITIES){
        numFeatures += PairFeatureFactoryEnglish.NUM_SIM_FEATURES;
      }
        
      for (int i = 0; i < numFeatures; i++) {
        int featureIndex = i + 1;
        this.fm.write(this.pairwiseOutputPath, ",f" + featureIndex);
      }
      this.fm.writeLn(this.pairwiseOutputPath, "");
      this.firstRow = false;
    }
    
    this.fm.writeLn(this.goodVSbadOutputPath, 
        cid + "," + cgold + "," + cgold_yn + "," 
        + Joiner.on(",").join(features)); 
    
    if (PRODUCE_SVMLIGHTTK_DATA) {
      produceSVMLightTKExample(this.questionCas, commentCas, this.getFileSuffix(), this.ts,
          qid, cid, cgold, cgold_yn, features);
    }
    if (PRODUCE_KELP_DATA){
      produceKelpExample(this.questionCas, commentCas, this.kelpFilePath, this.ts,
          qid, cid, cgold, cgold_yn, features);
    }
    
    //return features;
  }
  
  
  /**
   * TODO Gio: please document this 
   * @param thread
   * @return
   * @throws UIMAException
   * @throws IOException
   */
  public ArrayList<Double> getCommentFeatureRepresentation(Element thread) 
      throws UIMAException, IOException {
    return this.getCommentFeatureRepresentation(thread, 
        thread.getElementsByTag("QBody").get(0).text());
    
  }
  
  public ArrayList<Double> getCommentFeatureRepresentation(Element thread, String userQuestion) throws IOException, UIMAException {
    
    /*Comment-level features to be combined*/
    List<List<Double>>  listFeatures = new ArrayList<List<Double>>();
    
    Question q = new Question();
    /**
     * Parse question node
     */
    String qid = thread.attr("QID");
    String qcategory = thread.attr("QCATEGORY");
    String qdate = thread.attr("QDATE");
    String quserid = thread.attr("QUSERID");
    String qtype = thread.attr("QTYPE");
    String qgold_yn = thread.attr("QGOLD_YN");    
    String qsubject = JsoupUtils.recoverOriginalText(thread.getElementsByTag("QSubject").get(0).text());
    qsubject = TextNormalizer.normalize(qsubject);
    String qbody = thread.getElementsByTag("QBody").get(0).text();
    qbody = JsoupUtils.recoverOriginalText(UserProfile.removeSignature(qbody, userProfiles.get(quserid)));
    qbody = TextNormalizer.normalize(qbody);
    //      questionCategories.add(qcategory);
    
    q.setQid(qid);
    q.setQcategory(qcategory);
    q.setQdate(qdate);
    q.setQuserId(quserid);
    q.setQtype(qtype);
    q.setQgoldYN(qgold_yn);
    q.setQbody(qbody);
    
    Elements comments = thread.getElementsByTag("Comment");
    
    for (Element comment : comments) {
      
      String cid = comment.attr("CID");
      String cuserid = comment.attr("CUSERID");
      String cgold = comment.attr("CGOLD");
      //Replacing the labels for the "macro" ones: GOOD vs BAD
      if (ONLY_BAD_AND_GOOD_CLASSES){
        if (cgold.equalsIgnoreCase("good")){
          cgold = GOOD;
        } else {
          cgold = BAD;
        }
      }
      String cgold_yn = comment.attr("CGOLD_YN");
      String csubject = JsoupUtils.recoverOriginalText(comment.getElementsByTag("CSubject").get(0).text());
      csubject = TextNormalizer.normalize(csubject);
      String cbody = comment.getElementsByTag("CBody").get(0).text();
      cbody = JsoupUtils.recoverOriginalText(UserProfile.removeSignature(cbody, userProfiles.get(cuserid)));
      cbody = TextNormalizer.normalize(cbody);
      q.addComment(cid, cuserid, cgold, cgold_yn, csubject, cbody);
    }
    return this.getCommentFeatureRepresentation(q, userQuestion);
  }
  
  
  public ArrayList<Double> getCommentFeatureRepresentation(Question thread) throws IOException, UIMAException {
    //TODO confirm that getQbody is enough. Check how to get the question from a different place can be assigned
    return this.getCommentFeatureRepresentation(thread, thread.getQbody());
  }
  
  //FIXME ABC, Sep 10. Gio: I still don't understand the reasoning behind this invocation. Let's discuss 
  public ArrayList<Double> getCommentFeatureRepresentation(Question thread, String userQuestion) throws IOException, UIMAException {
    
    ArrayList<Double> featureMap = new ArrayList<Double>();

    /*Comment-level features to be combined*/
    List<List<Double>>  listFeatures = new ArrayList<List<Double>>();
    
    //Question q = new Question();
    /**
     * Parse question node
     */
    String qid = thread.getQid();
    String quserid = thread.getQuserid();
    String qsubject = TextNormalizer.normalize(thread.getQsubject());
    String qbody = thread.getQbody(); 
    qbody = JsoupUtils.recoverOriginalText(UserProfile.removeSignature(qbody, userProfiles.get(quserid)));
    qbody = TextNormalizer.normalize(qbody);
    //      questionCategories.add(qcategory);
    /**
     * Setup question CAS
     */
    this.questionCas.reset();
    this.questionCas.setDocumentLanguage("en");
    String questionText = SubjectBodyAggregator.getQuestionText(qsubject, qbody);
    //fm.writeLn(plainTextOutputPath, "---------------------------- QID: " + qid + " USER:" + quserid);
    //fm.writeLn(plainTextOutputPath, questionText);
    this.questionCas.setDocumentText(questionText);

    /** Run the UIMA pipeline */
    SimplePipeline.runPipeline(questionCas, this.analysisEngineList);

    //this.analyzer.analyze(questionCas, new SimpleContent("q-" + qid, qsubject + ". " + qbody));

    /** Parse comment nodes */
    //Elements comments = thread.getElementsByTag("Comment");

    /** Extracting context statistics for context Features */
    
    for (Comment comment : thread.getComments()) {
      if (ONLY_BAD_AND_GOOD_CLASSES){
        String cgold = comment.getCgold();
        if(cgold.equalsIgnoreCase("good")){
          cgold = GOOD;
        } else {
          cgold = BAD;
        }
        comment.setGold(cgold);
      }
      String csubject = TextNormalizer.normalize(comment.getCsubject());
      String cbody = TextNormalizer.normalize(comment.getCbody());
    }
    List<HashMap<String, Double>> albertoSimoneFeatures;
    if (GENERATE_ALBERTO_AND_SIMONE_FEATURES){
      albertoSimoneFeatures = FeatureExtractor.extractFeatures(thread);
    }

    int commentIndex = 0;
    List<JCas> allCommentsCas = new ArrayList<JCas>();
    
    for (Comment comment : thread.getComments()) {
      String cid = comment.getCid();
      String cuserid = comment.getCuserid();
      String cgold = comment.getCgold();
      String cgold_yn = comment.getCgold_yn();
      String csubject = comment.getCsubject();
      String cbody = comment.getCbody();
      /**
       * Setup comment CAS
       */
      JCas commentCas = JCasFactory.createJCas();
      commentCas.setDocumentLanguage("en");
      String commentText = SubjectBodyAggregator.getCommentText(csubject, cbody);
      commentCas.setDocumentText(commentText);
      /**
       * Run the UIMA pipeline
       */
      SimplePipeline.runPipeline(commentCas, this.analysisEngineList);
      //this.analyzer.analyze(commentCas, new SimpleContent("c-" + cid, csubject + ". " + cbody));

      AugmentableFeatureVector fv;
      if (GENERATE_MASSIMO_FEATURES){
        fv = (AugmentableFeatureVector) pfEnglish.getPairFeatures(questionCas, commentCas, PARAMETER_LIST);
      } else {
        fv = new AugmentableFeatureVector(this.alphabet);
      }

      if (GENERATE_ALBERTO_AND_SIMONE_FEATURES){
        HashMap<String, Double> featureVector = albertoSimoneFeatures.get(commentIndex);
        
        for (String featureName : FeatureExtractor.getAllFeatureNames()){
          Double value = featureVector.get(featureName);
          double featureValue =0;
          if (value!=null) {
            featureValue = value;
          }
          fv.add(featureName, featureValue);
        }

      }
      commentIndex++;

      /***************************************
       * * * * PLUG YOUR FEATURES HERE * * * *
       ***************************************/

      /**
       * fv is actually an AugmentableFeatureVector from the Mallet library
       * 
       * Internally the features are named so you must specify an unique identifier.
       * 
       * An example:
       * 
       * ((AugmentableFeatureVector) fv).add("your_super_feature_id", 42);
       * 
       * or:
       * 
       * AugmentableFeatureVector afv = (AugmentableFeatureVector) fv;
       * afv.add("your_super_feature_id", 42);
       * 
       */

      //((AugmentableFeatureVector) fv).add("quseridEqCuserid", quseridEqCuserid);

      /***************************************
       * * * * THANKS! * * * *
       ***************************************/

      /**
       * Produce output line
       */
  
      List<Double> features = this.serializeFv(fv);
      listFeatures.add(features);
      
      if (WRITE_FEATURES_TO_FILE) {
        this.writeFeaturesToFile(fv, commentCas, qid, cid, cgold, cgold_yn);
      }
      /**
       * Produce also the file needed to train structural models
       */     
      allCommentsCas.add(commentCas);
    }
    
    if (WRITE_FEATURES_TO_FILE) {
      this.fm.write(this.pairwiseOutputPath, computePairwiseFeatures(thread, listFeatures, allCommentsCas));
      //out.writeLn(computePairwiseFeatures(q, listFeatures);
    }
    return featureMap;
  }

}
