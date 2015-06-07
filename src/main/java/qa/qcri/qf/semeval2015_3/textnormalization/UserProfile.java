package qa.qcri.qf.semeval2015_3.textnormalization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Currently UserProfile contains only the user signatures
 * 
 * @author Simone Filice
 *
 */
public class UserProfile {

	private static final int MINIMUM_SIGNATURE_LENGTH = 10;
	private static int removedSignatures = 0;

	private List<String> signatures;

	private List<String> messageBodies;

	public UserProfile(){
		this.signatures = new ArrayList<String>();
		this.messageBodies = new LinkedList<String>();
	}

	public void addMessageBody(String body){
		
		String messageBody = JsoupUtils.specialTrim(body);
		for(String signature : signatures){
			if(messageBody.endsWith(signature)){
				return;
			}
		}

		int index = 0;
		for(String previousBody : messageBodies){
			
			if(previousBody.equals(messageBody)){//if two messages are identical it is probably due to a double posting
				return;
			}

			String commonSuffix = JsoupUtils.specialTrim(getCommonSuffix(previousBody, messageBody));

			if(JsoupUtils.recoverOriginalText(commonSuffix).trim().length()>=MINIMUM_SIGNATURE_LENGTH && !commonSuffix.toLowerCase().contains("thank")){

				if(messageBody.length() != commonSuffix.length() ){//To avoid empty messages due to double posting of the final part of a comment (like in Q510_C3)
					this.signatures.add(commonSuffix);
					this.messageBodies.remove(index);
					return;
				}
				
			}

			index++;
		}
		this.messageBodies.add(messageBody);
	}

	public List<String> getSignatures(){
		return this.signatures;
	}

	public static String getCommonSuffix(String stringA, String stringB){

		String [] partsA = stringA.split(JsoupUtils.LINE_START);
		String [] partsB = stringB.split(JsoupUtils.LINE_START);


		int shortestLenght = Math.min(partsA.length, partsB.length);
		String signature = "";

		for(int i=0; i<shortestLenght; i++){
			if(!partsA[partsA.length-i-1].equals(partsB[partsB.length-i-1])){
				break;
			}
			signature = partsA[partsA.length-i-1] + JsoupUtils.LINE_START + signature;
		}

		return signature;
	}

	public static Map<String, UserProfile> createUserProfiles(Document ... docs) throws IOException {
		HashMap<String, UserProfile> profiles = new HashMap<String, UserProfile>();
		for(Document doc : docs){

			/**
			 * Consume data
			 */
			Elements questions = doc.getElementsByTag("Question");		

			for(Element question : questions) {


				String quserid = question.attr("QUSERID");

				String qbody = question.getElementsByTag("QBody").get(0).text();

				UserProfile questionUserProfile = profiles.get(quserid);
				if(questionUserProfile==null){
					questionUserProfile = new UserProfile();
					profiles.put(quserid, questionUserProfile);
				}
				questionUserProfile.addMessageBody(qbody);

				/**
				 * Parse comment nodes
				 */
				Elements comments = question.getElementsByTag("Comment");


				for(Element comment : comments) {

					String cuserid = comment.attr("CUSERID");

					String cbody = comment.getElementsByTag("CBody").get(0).text();

					UserProfile commentUserProfile = profiles.get(cuserid);
					if(commentUserProfile==null){
						commentUserProfile = new UserProfile();
						profiles.put(cuserid, commentUserProfile);
					}
					
					commentUserProfile.addMessageBody(cbody);

				}

			}

		}

		return profiles;
	}
	
	public static String removeSignature(String body, UserProfile userProfile){
		String messageBody = JsoupUtils.specialTrim(body);
		for(String signature : userProfile.getSignatures()){
			if(messageBody.endsWith(signature)){
				removedSignatures++;
				return JsoupUtils.specialTrim(messageBody.substring(0, messageBody.length()-signature.length()));
			}
		}
		
		int index = messageBody.indexOf("___________");
		if(index>0){
			removedSignatures++;
			messageBody = JsoupUtils.specialTrim(messageBody.substring(0, index));
		}
		return messageBody;
	}

	/**
	 * @return the removedSignatures
	 */
	public static int getRemovedSignatures() {
		return removedSignatures;
	}


}
