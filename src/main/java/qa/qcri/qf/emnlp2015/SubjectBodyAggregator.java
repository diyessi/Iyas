package qa.qcri.qf.emnlp2015;

@Deprecated
public class SubjectBodyAggregator {
	
	/**
	 * This class is deprecated and the method to extract the entire text from a 
	 * question is now in CQAquestion
	 * 
	 * @see CQAquestion
	 * @param questionSubject
	 * @param questionBody
	 * @return
	 */
  @Deprecated
	public static String getQuestionText(String questionSubject, String questionBody){
		
		if(questionBody.toLowerCase().startsWith(questionSubject.toLowerCase())){
			return questionBody;
		}
		if(Character.isUpperCase(questionBody.charAt(0))){
			return questionSubject+ ". " + questionBody; 
		}
		return questionSubject+ " " + questionBody; 
		
	}
	
  /**
   * This class is deprecated and the method to extract the entire text from a 
   * comment is now in CQAcomment
   * 
   * @see CQAquestion
   * @param commentSubject
   * @param commentBody
   * @return
   */
  @Deprecated
	public static String getCommentText(String commentSubject, String commentBody){
		if(commentSubject.toLowerCase().startsWith("re:")){
			return commentBody;
		}
		if(commentBody.toLowerCase().startsWith(commentSubject.toLowerCase())){
			return commentBody;
		}
		if(commentBody.length()==0){
			return commentSubject;
		}
		if(Character.isUpperCase(commentBody.charAt(0))){
			return commentSubject+ ". " + commentBody; 
		}
		return commentSubject+ " " + commentBody; 
	}

	//TODO merge these two functions??? OR MOVE TO THE CORRESPONDING CLASS
	
}
