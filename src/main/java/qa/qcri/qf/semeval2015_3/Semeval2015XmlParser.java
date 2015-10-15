package qa.qcri.qf.semeval2015_3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import qa.qcri.qf.semeval2015_3.textnormalization.JsoupUtils;
import qa.qcri.qf.semeval2015_3.textnormalization.TextNormalizer;
import qa.qf.qcri.check.CHK;
import qa.qf.qcri.cqa.CQAinstance;
import qa.qf.qcri.cqa.CQAquestion;


/**
 * This class reads the SemEval 2015 task 3 on cQA XML file for English and 
 * makes an object available with the questions and threads.
 * 
 * The main method shows how to use it.
 * 
 * @author albarron
 */
public class Semeval2015XmlParser {

  /** The Jsoup doc to load the XML file in*/
  protected Document doc;
  
  private static final String XML_FILE = "semeval2015-3/data/"
      + "SemEval2015-Task3-English-data/datasets/emnlp15/CQA-QL-devel.xml";
  
  /**
   * At invocation time the source XML file has to be provided
   * @param xmlFile 
   */
  public Semeval2015XmlParser(String xmlFile) {
    CHK.CAN_READ(new File(xmlFile), "");
    try {
      doc = JsoupUtils.getDoc(xmlFile);
    } catch (IOException e) {
      System.out.println("I cannot read the input xml file");
      e.printStackTrace();
    }
  }

  public List<CQAinstance> readXml() {
    /** Consume data */
    Elements questions = doc.getElementsByTag("Question");    
    int numberOfQuestions = questions.size();
    int qNumber = 0;

    List<CQAinstance> instances = new ArrayList<CQAinstance>();
    for (Element question : questions) {
      qNumber++;
      if (qNumber % 10 == 0) {
        System.out.println(String.format("[INFO]: Processing %d out of %d", 
            qNumber, numberOfQuestions));
      }
      instances.add(qElementToObject(question));
    }
    return instances;
  }

  /**
   * Loads the contents of a question-thread pair from an XML element. 
   * 
   * TODO decide whether this should be public and the iteration should be 
   * somewhere else
   * 
   * @param qelement XML element with the question-thread information
   * @return object CQAinstance with the question-thread
   */
  private CQAinstance qElementToObject (Element qelement) {
    String id = qelement.attr("QID");
    String category = qelement.attr("QCATEGORY");
    String date = qelement.attr("QDATE");
    String userid = qelement.attr("QUSERID");
    String type = qelement.attr("QTYPE");
    String goldYN = qelement.attr("QGOLD_YN");
    String subject = TextNormalizer.normalize( 
            JsoupUtils.recoverOriginalText(
                    qelement.getElementsByTag("QSubject").get(0).text()) 
              );
    //TODO we don't normalise the subject?
    String body = qelement.getElementsByTag("QBody").get(0).text();
    //FIXME make it use useprofiles as below
    //body = JsoupUtils.recoverOriginalText(
    //            UserProfile.removeSignature(body, 
    //                          userProfiles.get(userid)));
    body = TextNormalizer.normalize(body);
    CQAquestion q = new CQAquestion(id,  date, userid, type, goldYN, subject, body);
    CQAinstance cqa = new CQAinstance(q, category);
    
    /** Parse comment nodes */
    for (Element comment : qelement.getElementsByTag("Comment")) {      
      String cid = comment.attr("CID");
      String cuserid = comment.attr("CUSERID");
      String cgold = comment.attr("CGOLD");
      
      //TODO whether we still hard code this here 
//      if (ONLY_BAD_AND_GOOD_CLASSES) {
//        cgold = (cgold.equalsIgnoreCase("good")) ? GOOD : BAD;
//      }
      String cgold_yn = comment.attr("CGOLD_YN");
      String csubject = JsoupUtils.recoverOriginalText(
                    comment.getElementsByTag("CSubject").get(0).text());
      csubject = TextNormalizer.normalize(csubject);
      String cbody = comment.getElementsByTag("CBody").get(0).text();
      //FIXME make the following line work
      //cbody = JsoupUtils.recoverOriginalText(
      //          UserProfile.removeSignature(cbody, userProfiles.get(cuserid)));
      cbody = TextNormalizer.normalize(cbody);
      cqa.addComment(cid, cuserid, cgold, cgold_yn, csubject, cbody);
    }
    return cqa;
  }

  public static void main (String[] args) {
    System.out.println(String.format("[INFO]: Parsing file %s", XML_FILE));
    
    Semeval2015XmlParser xmlParser = new Semeval2015XmlParser(XML_FILE);
    List<CQAinstance> cqaInstances = xmlParser.readXml();
    
    System.out.println(String.format("[INFO]: %d instances loaded", 
        cqaInstances.size()));
   
    for (CQAinstance cqaInstance : cqaInstances) {
      System.out.println(String.format("[INFO]: Instance %s contains %d comments", 
          cqaInstance.getQuestion().getId(), cqaInstance.getNumberOfComments()));
    }
  }
  
}
