package qa.qcri.qf.cQAdemo;

import java.util.ArrayList;
import java.util.List;

import qa.qcri.qf.semeval2015_3.CQAinstance;
import qa.qcri.qf.semeval2015_3.Question;

/**
 * A class to create Question objects from QatarLiving.com urls. 
 * 
 *
 */

public class LinkToQuestionObjectMapper {
	
	public LinkToQuestionObjectMapper() {
		// TODO by Hamdy
	}

	/**
	 * The function, for each url of the QatarLiving website, create a Question object from the corresponding thread  
	 * @param urls, a list of String representing urls 
	 * @return a list of Question objects, each one related to the corresponding url 
	 */
	public List<CQAinstance> getQuestions(List<String> urls) {
		return null;
	}
	
	/**
	 * Function for testing purposes 
	 * @return a list of Question objects 
	 */
	public List<Question> getQuestions() {
		List<Question> qList = new ArrayList<Question>();
		String qid = "1234";
		String qcategory = "";
		String qdate = "";
		String quserid = "";
		String qtype = ""; 	// Type of question (GENERAL, YES_NO)
		String qgold_yn = ""; //{Yes, No}  
		String qsubject = ""; //question subject
		String qbody = "";    //question body
		Question q = new Question(qid, qcategory, qdate, quserid, qtype, qgold_yn, qsubject, qbody);
		qList.add(q);
		return qList;
	}
	
}
