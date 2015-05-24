package qa.qcri.qf.semeval2015_3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
import org.maltparser.core.helper.HashSet;

import qa.qcri.qf.semeval2015_3.Question.Comment;
import qa.qcri.qf.semeval2015_3.wordspace.WordSpace;

public class FeatureExtractor {
	
	//Set to true if you want to consider the previous-comment classes
	private static final boolean INCLUDE_PAST = false; 
	private static final boolean INCLUDE_LSA_SIMILARITY = false;
	private static final boolean INCLUDE_LSA_VECTORS = false;
	private static final boolean INCLUDE_CONTEXT = true;
	private static final boolean INCLUDE_HEURISTICS = true;

	private static final Pattern EMAIL_REGEX = UrlPatterns.EMAIL_ADDRESS;
	private static final Pattern URL_REGEX = UrlPatterns.WEB_URL;
	
	private static final int PROPAGATION_WINDOW = 10;
	private static final double EPSILON = 0.0001;
	private static final int POSITION_SATURATION = 20;
	private static final int LENGTH_SATURATION = 400;
	private static final int MULT_REAL_SATURATION = 10;
	
	private static final int MIN_CHAR_REPETITION = 3;	
	private static final int LONG_WORD_THRES = 15;
	
	private static final String QUSER_COMMENT = "QUSER_COMMENT";
	private static final String QUSER_QUEST = "QUSER_QUEST";
	private static final String QUSER_ACK = "QUSER_ACK";
	private static final String QUSER_NOACK = "QUSER_NOACK";
	private static final String ACK = "ACK";
	private static final String NOACK = "NOACK";
	private static final String QUEST_PAST = "QUEST_PAST";
	private static final String QUEST_FUT = "QUEST_FUT";
	private static final String MULT_BOOL = "MULT_BOOL";
	private static final String MULT_REAL = "MULT_REAL";
	private static final String MULT_FIRST = "MULT_FIRST";
	private static final String MULT_MID = "MULT_MID";
	private static final String MULT_LAST = "MULT_LAST";
	private static final String DIAL_Uq_START = "DIAL_Uq_START";
	private static final String DIAL_Uq_IN = "DIAL_Uq_IN";
	private static final String DIAL_Uq_END = "DIAL_Uq_END";
	private static final String DIAL_U_START = "DIAL_U_START";
	private static final String DIAL_U_IN = "DIAL_U_IN";
	private static final String DIAL_U_END = "DIAL_U_END";
	private static final String EMAIL = "EMAIL";
	private static final String URL = "URL";
	private static final String POSITION = "POSITION";
	private static final String LENGTH = "LENGTH";
	
	//Noisy-based features
	private static final String REPEATED_CHARS = "REPEATED_CHARS";
	private static final String VERY_LONG_WORD = "VERY_LONG_WORD";
	
	private static final String HISTORY_1_Start = "HISTORY_1_Start";
	private static final String HISTORY_1_Bad = "HISTORY_1_Bad";
	private static final String HISTORY_1_Potential = "HISTORY_1_Potential";
	private static final String HISTORY_1_Good = "HISTORY_1_Good";
	private static final String HISTORY_2_Start_Bad = "HISTORY_2_Start_Bad";
	private static final String HISTORY_2_Start_Potential = "HISTORY_2_Start_Potential";
	private static final String HISTORY_2_Start_Good = "HISTORY_2_Start_Good";
	private static final String HISTORY_2_Bad_Potential = "HISTORY_2_Bad_Potential";
	private static final String HISTORY_2_Bad_Good = "HISTORY_2_Bad_Good";
	private static final String HISTORY_2_Potential_Bad = "HISTORY_2_Potential_Bad";
	private static final String HISTORY_2_Potential_Good = "HISTORY_2_Potential_Good";
	private static final String HISTORY_2_Good_Bad = "HISTORY_2_Good_Bad";
	private static final String HISTORY_2_Good_Potential = "HISTORY_2_Good_Potential";
	private static final String CATEGORY_Salary_and_Allowances  = "CATEGORY_Salary and Allowances";
	private static final String CATEGORY_Health_and_Fitness  = "CATEGORY_Health and Fitness";
	private static final String CATEGORY_Sightseeing_and_Tourist_attractions = "CATEGORY_Sightseeing and Tourist attractions";
	private static final String CATEGORY_Electronics = "CATEGORY_Electronics";
	private static final String CATEGORY_Computers_and_Internet = "CATEGORY_Computers and Internet";
	private static final String CATEGORY_Visas_and_Permits = "CATEGORY_Visas and Permits";
	private static final String CATEGORY_Life_in_Qatar = "CATEGORY_Life in Qatar";
	private static final String CATEGORY_Advice_and_Help = "CATEGORY_Advice and Help";
	private static final String CATEGORY_Sports_in_Qatar = "CATEGORY_Sports in Qatar";
	private static final String CATEGORY_Funnies = "CATEGORY_Funnies";
	private static final String CATEGORY_Language = "CATEGORY_Language";
	private static final String CATEGORY_Education = "CATEGORY_Education";
	private static final String CATEGORY_Qatar_Living_Lounge = "CATEGORY_Qatar Living Lounge";
	private static final String CATEGORY_Beauty_and_Style = "CATEGORY_Beauty and Style";
	private static final String CATEGORY_Socialising = "CATEGORY_Socialising";
	private static final String CATEGORY_Cars = "CATEGORY_Cars";
	private static final String CATEGORY_Family_Life_in_Qatar = "CATEGORY_Family Life in Qatar";
	private static final String CATEGORY_Moving_to_Qatar = "CATEGORY_Moving to Qatar";
	private static final String CATEGORY_Working_in_Qatar = "CATEGORY_Working in Qatar";
	private static final String CATEGORY_Opportunities = "CATEGORY_Opportunities";
	private static final String CATEGORY_Investment_and_Finance = "CATEGORY_Investment and Finance";
	private static final String CATEGORY_Pets_and_Animals = "CATEGORY_Pets and Animals";
	private static final String CATEGORY_Doha_Shopping = "CATEGORY_Doha Shopping";
	private static final String CATEGORY_Qatari_Culture = "CATEGORY_Qatari Culture";
	private static final String CATEGORY_Environment = "CATEGORY_Environment";
	private static final String CATEGORY_Politics = "CATEGORY_Politics";
	
	private static final String PREF_CONTAIN = "CONTAIN_";
	private static final String PREF_CONTAINS_SYMB = "CONTAINSYMB_";
	private static final String PREF_STARTS = "STARTS_WITH_";
	
	private static final String[] KEY_WORDS = {"yes", "sure", "no", "can", "neither", "okay", "sorry"};
	private static final String[] KEY_SYMBOLS = {"?", "@"};
	private static final String[] START_WORDS = {"yes"};
	
	
	
	private static final String N_LSA_SIMILARITY = "N_LSA_SIMILARITY";
	private static final String LSA_SIMILARITY = "LSA_SIMILARITY";
	
	private static final String LSA_Q_PREFIX = "Q_LSA_";
	private static final String LSA_C_PREFIX = "C_LSA_";
	
	private static final String wordspacePath = "semeval2015-3/LSAWord.txt.gz";
	private static final WordSpace wordspace;
	private static final Set<Character> posOfInterest;
	
	
	
	private static List<String> contextFeatureNames = new ArrayList<String>();
	private static List<String> heuristFeatureNames = new ArrayList<String>();
	private static List<String> pastFeatureNames = new ArrayList<String>();
	private static List<String> lsaQuestionNames = new ArrayList<String>();
	private static List<String> lsaCommentNames = new ArrayList<String>();
	private static List<String> completeFeatureSet = new ArrayList<String>();
	
	static{
		
		posOfInterest = new HashSet<Character>();
		//posOfInterest.add('j');
		//posOfInterest.add('v');
		posOfInterest.add('n');
		
		try {
			//wordspace = new WordSpace(wordspacePath);
			wordspace = null;
		} catch (final Exception e) {
			throw new RuntimeException("Failed to create WordSpace instance in static block.",e);
		}
		
		if (INCLUDE_HEURISTICS) {			
			for(String name : new String[]{
					QUSER_COMMENT, QUSER_QUEST, QUSER_ACK, QUSER_NOACK,  				
					EMAIL, URL, LENGTH, REPEATED_CHARS, VERY_LONG_WORD,					
					CATEGORY_Salary_and_Allowances, 
					CATEGORY_Health_and_Fitness, 
					CATEGORY_Sightseeing_and_Tourist_attractions,
					CATEGORY_Electronics,
					CATEGORY_Computers_and_Internet,
					CATEGORY_Visas_and_Permits,
					CATEGORY_Life_in_Qatar,
					CATEGORY_Advice_and_Help,
					CATEGORY_Sports_in_Qatar,
					CATEGORY_Funnies,
					CATEGORY_Language,
					CATEGORY_Education,
					CATEGORY_Qatar_Living_Lounge,
					CATEGORY_Beauty_and_Style,
					CATEGORY_Socialising,
					CATEGORY_Cars,
					CATEGORY_Family_Life_in_Qatar,
					CATEGORY_Moving_to_Qatar,
					CATEGORY_Working_in_Qatar,
					CATEGORY_Opportunities,
					CATEGORY_Investment_and_Finance,
					CATEGORY_Pets_and_Animals,
					CATEGORY_Doha_Shopping,
					CATEGORY_Qatari_Culture,
					CATEGORY_Environment,
					CATEGORY_Politics,					
			}){
				heuristFeatureNames.add(name);
			}
			for (String name : KEY_WORDS){
				heuristFeatureNames.add(PREF_CONTAIN+name);
			}
			for (String name : KEY_SYMBOLS){
				heuristFeatureNames.add(PREF_CONTAINS_SYMB+name);
			}			
			
			for (String name : START_WORDS){
				heuristFeatureNames.add(PREF_STARTS+name);
			}
			
			
			
		}	

		if (INCLUDE_CONTEXT){
			for (String context : new String[]{
				ACK, NOACK, QUEST_PAST, QUEST_FUT,
				MULT_BOOL, MULT_REAL, MULT_FIRST,	
				MULT_MID, MULT_LAST, 
				DIAL_Uq_START, DIAL_Uq_IN, DIAL_Uq_END, 
				DIAL_U_START, DIAL_U_IN, DIAL_U_END,
				POSITION, 
							
			}) {					
				contextFeatureNames.add(context);				
			}
		}
		
		
		//We add the past-related features in case we prefer to discard them
		if (INCLUDE_PAST){
			for (String hBased : new String[]{
				HISTORY_1_Start, HISTORY_1_Bad, HISTORY_1_Potential, HISTORY_1_Good,
				HISTORY_2_Start_Bad, HISTORY_2_Start_Potential, HISTORY_2_Start_Good,
				HISTORY_2_Bad_Potential, HISTORY_2_Bad_Good,
				HISTORY_2_Potential_Bad, HISTORY_2_Potential_Good,
				HISTORY_2_Good_Bad, HISTORY_2_Good_Potential}) {
				
				pastFeatureNames.add(hBased);
			}
		}
		
		if(INCLUDE_LSA_SIMILARITY){
			contextFeatureNames.add(N_LSA_SIMILARITY);
			contextFeatureNames.add(LSA_SIMILARITY);
		}
		
		if(INCLUDE_LSA_VECTORS){
			for(int i=0; i<wordspace.getSpaceDimensionality(); i++){
				lsaQuestionNames.add(LSA_Q_PREFIX + (i+1));
				lsaCommentNames.add(LSA_C_PREFIX + (i+1));
			}
		}
		
		completeFeatureSet.addAll(heuristFeatureNames);
		completeFeatureSet.addAll(contextFeatureNames);
		completeFeatureSet.addAll(pastFeatureNames);
		completeFeatureSet.addAll(lsaQuestionNames);
		completeFeatureSet.addAll(lsaCommentNames);
		
	}

	private class CommentInfo{
		Boolean containsAckFromQuestionAuthor=false;
		Boolean containsQuestionFromQuestionAuthor=false;
		Boolean isQuestionAuthor = false;
		
		public CommentInfo(){

		}
	}

	public static List<HashMap<String, Double>> extractFeatures(Question question){
		String quserid = question.getQuserid();
		List<CommentInfo> commentInfos = new ArrayList<CommentInfo>();
		HashMap<String, List<Integer>> userCommentIndices = new HashMap<String, List<Integer>>();
		
		List<String> commentThread = new ArrayList<String>();

		int commentCount = 0;
		for(Comment comment : question.getComments()){
			String cuserid = comment.getCuserid();
			List<Integer> indices = userCommentIndices.get(cuserid);
			if(indices==null){
				indices = new ArrayList<Integer>();
				indices.add(commentCount);
				userCommentIndices.put(cuserid, indices);
			}else{
				indices.add(commentCount);
			}
			CommentInfo info = new FeatureExtractor().new CommentInfo();

			if(cuserid.equals(quserid)){
				info.isQuestionAuthor = true;
				info.containsAckFromQuestionAuthor = containsAcknowledge(comment);
				info.containsQuestionFromQuestionAuthor = containsQuestion(comment);
			}
			
			
			commentThread.add(comment.getCgold());
			commentInfos.add(info);
			commentCount++;
		}

		/**
		 * INITIALIZING THE OUTPUT
		 */

		List<HashMap<String, Double>> features = new ArrayList<HashMap<String,Double>>();
		for(int i=0; i<question.getComments().size(); i++ ){
			features.add(new HashMap<String, Double>());
		}
		/**
		 * Extracting ACK, QUEST_PAST & NO_ACK
		 */
		double ackValue = 0;
		double questValue = 0;
		double noAckValue = 0;
		for(int i = commentInfos.size() -1; i>=0; i--){
			CommentInfo currentInfo = commentInfos.get(i);
			if(currentInfo.isQuestionAuthor){
				features.get(i).put(QUSER_COMMENT, 1.0);
				if(currentInfo.containsAckFromQuestionAuthor && currentInfo.containsQuestionFromQuestionAuthor){
					ackValue = 1;					
					questValue = 1;
					noAckValue = 0;
					features.get(i).put(QUSER_ACK, 1.0);
					features.get(i).put(QUSER_QUEST, 1.0);

				}
				else if(currentInfo.containsAckFromQuestionAuthor){
					ackValue = 1;
					questValue = 0;
					noAckValue = 0;
					features.get(i).put(QUSER_ACK, 1.0);
				}
				else if(currentInfo.containsQuestionFromQuestionAuthor){
					ackValue = 0;
					questValue = 1;
					noAckValue = 0;
					features.get(i).put(QUSER_QUEST, 1.0);
				}else{
					ackValue = 0;
					questValue = 0;
					noAckValue = 1;
					features.get(i).put(QUSER_NOACK, 1.0);
				}

			}else{

				if(ackValue>EPSILON){
					features.get(i).put(ACK, ackValue);
					ackValue -= 1.0/PROPAGATION_WINDOW;
				}
				if(questValue>EPSILON){
					features.get(i).put(QUEST_PAST, questValue);
					questValue -= 1.0/PROPAGATION_WINDOW;
				}
				if(noAckValue>EPSILON){
					features.get(i).put(NOACK, noAckValue);
					noAckValue -= 1.0/PROPAGATION_WINDOW;
				}
			}

		}
		/**
		 * Extracting QUEST_FUT
		 */
		questValue=0;
		for(int i=0; i<commentInfos.size(); i++){
			CommentInfo currentInfo = commentInfos.get(i);
			if(currentInfo.isQuestionAuthor){
				if(currentInfo.containsQuestionFromQuestionAuthor){
					questValue = 1;
				}else{
					questValue = 0;
				}
			}else{
				if(questValue>EPSILON){
					features.get(i).put(QUEST_FUT, questValue);
					questValue -= 1.0/PROPAGATION_WINDOW;
				}
			}
		}

		/**
		 * EXTRACTING MULT features
		 */
		for(List<Integer> indices : userCommentIndices.values()){
			if(indices.size()>1){
				for(int i=0; i<indices.size(); i++){
					int index = indices.get(i);
					HashMap<String, Double> commentFeatureVector = features.get(index);
					commentFeatureVector.put(MULT_BOOL, 1.0);

					if(i==0){
						commentFeatureVector.put(MULT_FIRST, 1.0);
					} else {
						if(i==indices.size()-1){					
							commentFeatureVector.put(MULT_LAST, 1.0);							
						}else{
							commentFeatureVector.put(MULT_MID, 1.0);							
						}
						commentFeatureVector.put(MULT_REAL, 
								Math.min(MULT_REAL_SATURATION, (double)i) /
								MULT_REAL_SATURATION);
					}
				}
			}

		}

		/**
		 * EXTRACTING DIAL_Uq features
		 */
		//		List<Integer> uqIndices = userCommentIndices.get(quserid);
		//		if(uqIndices!=null){//qUser wrote at least a comment so he could have had a dialog
		//			for(Entry<String, List<Integer>> entry : userCommentIndices.entrySet()){
		//				if(entry.getKey().equals(quserid)){
		//					continue;//skipping qUser
		//				}
		//				List<Integer> uIndices = entry.getValue();
		//				if(uIndices.size() > 1){//user u had at least two comments, so he could have started a dialog with qUser
		//					int u=0;
		//					int q=0;
		//					boolean uTalked = false;
		//			
		//					while(u<uIndices.size() && q<uqIndices.size()){
		//						if(uIndices.get(u) < uqIndices.get(q)){
		//							uTalked = true;
		//							u++;
		//						}else{
		//							if(uTalked){//the dialog starts
		//								features.get(uIndices.get(u-1)).put("DIAL_Uq_START", 1.0);
		//								for(int i = u; i<uIndices.size()-1; i++){
		//									features.get(uIndices.get(i)).put("DIAL_Uq_IN", 1.0);
		//								}
		//								int uLastCommentIndex = uIndices.get(uIndices.size()-1);
		//								int uqLastCommentIndex = uqIndices.get(uqIndices.size()-1);
		//								if(uLastCommentIndex > uqLastCommentIndex){
		//									features.get(uLastCommentIndex).put("DIAL_Uq_END", 1.0);
		//								}else{
		//									features.get(uLastCommentIndex).put("DIAL_Uq_IN", 1.0);
		//								}
		//								break;
		//							}
		//							q++;
		//						}
		//					}
		//				}
		//			}
		//			
		//		}

		List<Integer> uqIndices = userCommentIndices.get(quserid);
		if(uqIndices!=null){//qUser wrote at least a comment so he could have had a dialog
			for(Entry<String, List<Integer>> entry : userCommentIndices.entrySet()){
				if(entry.getKey().equals(quserid)){
					continue;//skipping qUser
				}
				List<Integer> uIndices = entry.getValue();
				if(uIndices.size() > 1){//user u had at least two comments, so he could have started a dialog with qUser
					int u=0;
					int q=0;
					int mergedIndex = 0;
					int [] mergedIndices = new int[uIndices.size()+uqIndices.size()];
					char [] userSequence = new char [uIndices.size()+uqIndices.size()];

					while(u<uIndices.size() && q<uqIndices.size()){
						if(uIndices.get(u) < uqIndices.get(q)){
							mergedIndices[mergedIndex] = uIndices.get(u);
							userSequence[mergedIndex] = 'U';
							u++;
						}else{
							mergedIndices[mergedIndex] = uqIndices.get(q);
							userSequence[mergedIndex] = 'Q';
							q++;
						}
						mergedIndex++;
					}
					
					while(u<uIndices.size()){//adding remaining comments from u
						
						mergedIndices[mergedIndex] = uIndices.get(u);
						userSequence[mergedIndex] = 'U';
						u++;
						mergedIndex++;
					}
					while(q<uqIndices.size()){//adding remaining comments from q
						mergedIndices[mergedIndex] = uqIndices.get(q);
						userSequence[mergedIndex] = 'Q';
						q++;
						mergedIndex++;
					}

					String sequenceString = String.copyValueOf(userSequence);
					String regex = "UQ+U";

					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(sequenceString);

					if(matcher.find()){
						int startIndex = matcher.start();
						features.get(mergedIndices[startIndex]).put(DIAL_Uq_START, 1.0);
						for(int i = startIndex + 1; i<mergedIndices.length-1; i++){
							features.get(mergedIndices[i]).put(DIAL_Uq_IN, 1.0);
						}
						features.get(mergedIndices[mergedIndices.length-1]).put(DIAL_Uq_END, 1.0);
					}

				}
			}

		}

		/**
		 * EXTRACTING DIAL_U features
		 */
		ArrayList<Entry<String, List<Integer>>> indicesPerUser = new ArrayList<Entry<String, List<Integer>>>();
		indicesPerUser.addAll(userCommentIndices.entrySet());
		//looping on all pair of users to check whether they had a dialog
		for(int u1=0; u1<indicesPerUser.size()-1; u1++){
			if(indicesPerUser.get(u1).getKey().equals(quserid)){
				continue;//skipping the quser
			}
			List<Integer> u1Indices = indicesPerUser.get(u1).getValue();
			if(u1Indices.size()>1){//user u1 has multiple comments, so he could have had a conversation
				for(int u2=u1+1; u2<indicesPerUser.size(); u2++){
					if(indicesPerUser.get(u2).getKey().equals(quserid)){
						continue;//skipping the quser
					}
					List<Integer> u2Indices = indicesPerUser.get(u2).getValue();

					if(u2Indices.size()>1){//u2 has multiple comments, so he could have had a conversation with u1
						int u1Index=0;
						int u2Index=0;
						int mergedIndex = 0;
						int [] mergedIndices = new int[u1Indices.size()+u2Indices.size()];
						char [] userSequence = new char [u1Indices.size()+u2Indices.size()];
						while(u1Index<u1Indices.size() && u2Index<u2Indices.size()){//creating an interlaced sequence between u1 and u2
							if(u1Indices.get(u1Index) < u2Indices.get(u2Index)){//current comment is from u1
								mergedIndices[mergedIndex] = u1Indices.get(u1Index);
								userSequence[mergedIndex] = '1';
								u1Index++;
							}else{//current comment is from u2
								mergedIndices[mergedIndex] = u2Indices.get(u2Index);
								userSequence[mergedIndex] = '2';
								u2Index++;
							}
							mergedIndex++;
						}
						while(u1Index<u1Indices.size()){//adding remaining comments from u1
							mergedIndices[mergedIndex] = u1Indices.get(u1Index);
							userSequence[mergedIndex] = '1';
							u1Index++;
							mergedIndex++;
						}
						while(u2Index<u2Indices.size()){//adding remaining comments from u2
							mergedIndices[mergedIndex] = u2Indices.get(u2Index);
							userSequence[mergedIndex] = '2';
							u2Index++;
							mergedIndex++;
						}

						String sequenceString = String.copyValueOf(userSequence);

						String regex1 = "12+1+2";
						String regex2 = "21+2+1";
						Pattern pattern = Pattern.compile(regex1);
						Matcher matcher = pattern.matcher(sequenceString);
						int startIndex = Integer.MAX_VALUE;
						boolean conversationStarted = false;
						if(matcher.find()){
							startIndex = matcher.start();
							conversationStarted = true;
						}
						pattern = Pattern.compile(regex2);
						matcher = pattern.matcher(sequenceString);
						if(matcher.find()){
							int startIndex2 = matcher.start();
							conversationStarted = true;
							if(startIndex2<startIndex){
								startIndex = startIndex2;
							}
						}

						if(conversationStarted){
							features.get(mergedIndices[startIndex]).put(DIAL_U_START, 1.0);
							for(int i = startIndex + 1; i<mergedIndices.length-1; i++){
								features.get(mergedIndices[i]).put(DIAL_U_IN, 1.0);
							}
							features.get(mergedIndices[mergedIndices.length-1]).put(DIAL_U_END, 1.0);
						}


					}


				}
			}
		}
		
		
		/**
		 * EXTRACTING HISTORY-BASED FEATURES 
		 */
		if (INCLUDE_PAST) {
			
			/**
			 * 1-GRAM: whether the previous comment in the stream is 
			 * good/bad/potential (or it is the first one) 
			*/
			features.get(0).put(HISTORY_1_Start, 1.0);		
			
			for(int i=1; i<commentInfos.size(); i++){
				features.get(i).put(
						String.format("HISTORY_1_%s", commentThread.get(i-1)), 
						1.0);
			}
	
			/**
			 * 2-gram: whether the two previous comment in the stream are a 
			 * combination of [start]/good/bad/potential (non-applicable for the 
			 * first one)
			 */				
			if (commentInfos.size() > 1){
				features.get(1).put("HISTORY_2_Start_"+commentThread.get(0), 1.0);
				for (int i=2; i < commentInfos.size(); i++){
					features.get(i).put(
							String.format("HISTORY_2_%s_%s", 
									commentThread.get(i-2), 
									commentThread.get(i-1)),  
									1.0);			
				}			
			}
		}
				
		/**
		 * EXTRACTING CATEGORY FEATURES
		 */
		String categoryFeature = "CATEGORY_" + question.getQcategory();
		for(HashMap<String, Double> featureVector : features){
			featureVector.put(categoryFeature, 1.0);
		}

		/**
		 * EXTRACTING EMAIL, URL, LENGTH and POSITION FEATURE, 
		 */
		for(int i=0; i<question.getComments().size(); i++){

			//As the text is modified in this section, we copy it into a temporal
			//string before doing any operation.
			String commentBody = question.getComments().get(i).getCbody()
										.replaceAll("\\s+"," ");
			if (containsEmail(commentBody)){
				features.get(i).put(EMAIL, 1.0);
				commentBody = removePattern(EMAIL_REGEX, commentBody);
			}
						
			//ATT: note that an email has to be looked up before a URL because
			//the URL pattern also recognizes mails as URLs
			if (hasUrl(commentBody)){
				features.get(i).put(URL, 1.0);
				commentBody = removePattern(URL_REGEX, commentBody);
			}

			for (String word : KEY_WORDS){
				if (containsWord(commentBody, word)){
					features.get(i).put(PREF_CONTAIN+word, 1.0);
				}
			}
			
			for (String seq : KEY_SYMBOLS){
				if (containsSequence(commentBody, seq)){
					features.get(i).put(PREF_CONTAINS_SYMB+seq, 1.0);
				}
			}		

			for (String word : START_WORDS){
				if (startsWithWord(commentBody, word)){
					features.get(i).put(PREF_STARTS+word, 1.0);
				}
			}
			
			
			
			double lengthFeature = (double)commentBody.length();
			if(lengthFeature>LENGTH_SATURATION){
				lengthFeature = (double)LENGTH_SATURATION;
			}
			features.get(i).put(LENGTH, lengthFeature/LENGTH_SATURATION);
			
			
			features.get(i).put(REPEATED_CHARS, 
					(containsRepeatedChars(commentBody)) ? 1.0 : 0.0
				);
						
			features.get(i).put(VERY_LONG_WORD, 
					(containsLongWord(commentBody)) ? 1.0 : 0.0
				);
						
			double positionFeature = (double)i;
			if(positionFeature>POSITION_SATURATION){
				positionFeature = (double)POSITION_SATURATION;
			}
			features.get(i).put(POSITION, positionFeature/POSITION_SATURATION);

		}
		
		/**
		 * EXTRACTING LSA FEATURES
		 */
		if(INCLUDE_LSA_SIMILARITY || INCLUDE_LSA_VECTORS){
			DenseMatrix64F qVector = wordspace.sentence2vector(question.getQsubject());
			CommonOps.addEquals(qVector, wordspace.sentence2vector(question.getQbody()));	
			
			DenseMatrix64F qPosFilteredVector = wordspace.sentence2vector(question.getQsubject(), posOfInterest);
			CommonOps.addEquals(qPosFilteredVector, wordspace.sentence2vector(question.getQbody(), posOfInterest));	
			
			
			
			for(int i=0; i<question.getComments().size(); i++){
				DenseMatrix64F cVector = wordspace.sentence2vector(question.getComments().get(i).getCbody());
				DenseMatrix64F cPosFilteredVector = wordspace.sentence2vector(question.getComments().get(i).getCbody(), posOfInterest);
				if(INCLUDE_LSA_SIMILARITY){
					double similarity = cosineSimilarity(qVector, cVector);
					double filteredSimilarity = cosineSimilarity(qPosFilteredVector, cPosFilteredVector);
					features.get(i).put(LSA_SIMILARITY, similarity);
					features.get(i).put(N_LSA_SIMILARITY, filteredSimilarity);
					
				}
				if(INCLUDE_LSA_VECTORS){
					for(int f=0; f<wordspace.getSpaceDimensionality(); f++){
						features.get(i).put(LSA_Q_PREFIX + (f+1),  qPosFilteredVector.get(0,f));
						features.get(i).put(LSA_C_PREFIX + (f+1),  cPosFilteredVector.get(0,f));
					}
				}
			}
		}
		return features;
	}	

	private static String removePattern(Pattern regex, String s){		
		Matcher matcher = regex.matcher(s);
		if (matcher.find()){
			//we substitute the mail or URL by MoU			
			return matcher.replaceAll("MoU");
		}
		return s;		
	}
	
	private static double cosineSimilarity(DenseMatrix64F vectorA, DenseMatrix64F vectorB){
		double normA = NormOps.fastNormP2(vectorA);
		double normB = NormOps.fastNormP2(vectorB);
		if(normA<=0 || normB<=0){
			return 0;
		}
		return VectorVectorMult.innerProd(vectorA, vectorB)/(normA*normB);
	}

	
	private static boolean hasUrl(String s) {
        try {
            Matcher matcher = URL_REGEX.matcher(s);
            return matcher.find();
        } catch (RuntimeException e) {
        return false;
        }
    }
	
	private static boolean containsEmail(String s) {
        try {
            Matcher matcher = EMAIL_REGEX.matcher(s);
            return matcher.find();
        } catch (RuntimeException e) {
        return false;
        }
    } 
	
	public static boolean containsURL(Comment comment) {
		String body = comment.getCbody();
		if(body.toLowerCase().contains("www.") || body.toLowerCase().contains("http://")){
			return true;
		}
		return false;
	}

	private static boolean containsRepeatedChars(String s){
		//Another alternative was a regular expression, but this way we don't need it		
		if (s.length() < MIN_CHAR_REPETITION){	return false; }
		
		int count = 1;
		char previous = s.charAt(0);
		for (int i = 1 ; i < s.length() ; i++ ){
			if (previous == s.charAt(i)){
				if (++count >= MIN_CHAR_REPETITION){
					return true;
				}
			} else {
				previous = s.charAt(i);
				count = 1;
			}			
		}
		return false;
	}
	
	private static boolean containsLongWord(String s){		
		for (String w : s.split("\\s")){
			if (w.length() >= LONG_WORD_THRES)
				return true;
		}
		return false;
	}

	public static boolean containsAcknowledge(Comment comment){
		String body = comment.getCbody().toLowerCase();
		if(body.contains("thank") || body.contains("appreciat")){
			return true;
		}
		return false;
	}
	
	public static boolean containsWord(String comment, String word){
		for (String w : comment.toLowerCase().split("\\s")){
			if (w.equals(word))
				return true;
		}		
		return false;
	}
	
	public static boolean containsSequence(String comment, String seq){
		return comment.toLowerCase().contains(seq);		
	}
	
	
	public static boolean startsWithWord(String comment, String word){
		return comment.toLowerCase().startsWith(word);
	}
	
	public static boolean containsQuestion(Comment comment){
		String body = comment.getCbody();
		if(body.contains("?")){
			return true;
		}
		return false;
	}

	public static List<String> getAllFeatureNames(){
		
		return completeFeatureSet; 
	}
	
	public static List<String> getPastFeatureNames(){
		
		return pastFeatureNames; 
	}
	
	public static List<String> getHeuristicFeatureNames(){
		return heuristFeatureNames;
	}
	
	public static List<String> getContextFeatureNames(){
		return contextFeatureNames;
	}
	
	public static List<String> getLsaCommentFeatureNames(){
		return lsaCommentNames; 
	}
	
	public static List<String> getLsaQuestionFeatureNames(){
		return lsaQuestionNames; 
	}

}
