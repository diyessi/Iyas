package qa.qcri.qf.emnlp2015;

public class SubjectBodyAggregator {
	
	public static String getQuestionText(String questionSubject, String questionBody){
		
		if(questionBody.toLowerCase().startsWith(questionSubject.toLowerCase())){
			return questionBody;
		}
		return questionSubject+ ". " + questionBody; 
		
	}
	
	public static String getCommentText(String commentSubject, String commentBody){
		if(commentSubject.toLowerCase().startsWith("re:")){
			return commentBody;
		}
		if(commentBody.toLowerCase().startsWith(commentSubject.toLowerCase())){
			return commentBody;
		}
		return commentSubject + ". " + commentBody;
	}

}
