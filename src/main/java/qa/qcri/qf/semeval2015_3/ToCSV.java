package qa.qcri.qf.semeval2015_3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import util.Stopwords;
import au.com.bytecode.opencsv.CSVWriter;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;


public class ToCSV {
	
	public static final String LANG_ENGLISH = "ENGLISH";
	
	public static final String LANG_ARABIC = "ARABIC";
	
	public static final String CQA_QL_TRAIN_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-train.xml";
	
	public static final String CQA_QL_DEV_EN = "semeval2015-3/data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-devel.xml";
	
	public static final String CQA_QL_TRAIN_AR = "semeval2015-3/data/"
			+ "SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-train.xml";
	
	public static final String CQA_QL_DEV_AR = "semeval2015-3/data/"
			+ "SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-dev.xml";
	
	private Set<String> a_labels = new HashSet<>();
	
	private Set<String> b_labels = new HashSet<>();

	private Stopwords stopwords;
	
	public static void main(String[] args) throws IOException, UIMAException, SimilarityException {
		/**
		 * Setup logger
		 */
		org.apache.log4j.BasicConfigurator.configure();
		
		/**
		 * Run the code for the Arabic task
		 */
		//new ToCSV().runForArabic();

		new ToCSV().runForEnglish();
	}
	
	public ToCSV() {

	}
	
	public void runForArabic() throws UIMAException {		
		/**
		 * Specify the task label
		 * For Arabic there is just one task
		 */
		this.a_labels.add("direct");
		this.a_labels.add("related");
		this.a_labels.add("irrelevant");
		
		try {
			processArabicFile(CQA_QL_TRAIN_AR, "train");
			processArabicFile(CQA_QL_DEV_AR, "dev");
		} catch (SimilarityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void processArabicFile(String dataFile, String suffix) throws SimilarityException, UIMAException, IOException {	
		FileOutputStream fos = new FileOutputStream(dataFile + ".full.csv"); 
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		CSVWriter writer = new CSVWriter(osw);
		
		Document doc = Jsoup.parse(new File(dataFile), "UTF-8");
		
		boolean firstRow = true;
		
		/**
		 * Consume data
		 */
		Elements questions = doc.getElementsByTag("Question");
		
		int numberOfQuestions = questions.size();
		int questionNumber = 1;
		
		List<String> header = new ArrayList<>();
		
		header.add("QID");
		header.add("QCATEGORY");
		header.add("QDATE");
		header.add("QSUBJECT");
		header.add("QBODY");
		header.add("CID");
		header.add("CGOLD");
		header.add("CBODY");
		
		for(Element question : questions) {
			System.out.println("[INFO]: Processing " + questionNumber++ + " out of " + numberOfQuestions);
			/**
			 * Parse question node
			 */
			String qid = question.attr("QID");
			String qcategory = question.attr("QCATEGORY");
			String qdate = question.attr("QDATE");		
			String qsubject = question.getElementsByTag("QSubject").get(0).text()
					.replaceAll("/", "")
					.replaceAll("~", "");
			String qbody = question.getElementsByTag("QBody").get(0).text()
					.replaceAll("/", "")
					.replaceAll("~", "");
			
			/**
			 * Parse answer nodes
			 */
			Elements comments = question.getElementsByTag("Answer");
			for(Element comment : comments) {
				String cid = comment.attr("CID");
				String cgold = comment.attr("CGOLD");
				String cbody = comment.text()
						.replaceAll("/", "")
						.replaceAll("~", "");
				
				if(firstRow) {
					String[] csvHeader = {
							"QID",
							"QCATEGORY",
							"QDATE",
							"QSUBJECT",
							"QBODY",
							"CID",
							"CGOLD",
							"CBODY",
					};
					writer.writeNext(csvHeader);
					firstRow = false;
				}
				
				String[] row = {
						qid,
						qcategory,
						qdate,
						StringEscapeUtils.escapeCsv(qsubject),
						StringEscapeUtils.escapeCsv(qbody),
						cid,
						cgold,
						StringEscapeUtils.escapeCsv(cbody)
				};
				
				writer.writeNext(row);
			}
		}
		
		writer.close();
		osw.close();
		fos.close();
	}
	
	public void runForEnglish() throws UIMAException, IOException {
		/**
		 * Specify A and B subtask labels 
		 */
		this.a_labels.add("Not English");
		this.a_labels.add("Good");
		this.a_labels.add("Potential");
		this.a_labels.add("Dialogue");
		this.a_labels.add("Bad");
		
		this.b_labels.add("No");
		this.b_labels.add("Yes");
		this.b_labels.add("Unsure");

		this.processEnglishFile(CQA_QL_TRAIN_EN, "train");
		this.processEnglishFile(CQA_QL_DEV_EN, "dev");
	}

	/**
	 * Process the xml file and output a csv file with the results in the same directory
	 * @param dataFile the xml file to process
	 * @suffix suffix for identifying the data file 
	 * 
	 * @param suffix
	 * @throws ResourceInitializationException
	 * @throws UIMAException
	 * @throws IOException
	 * @throws AnalysisEngineProcessException
	 * @throws SimilarityException
	 */
	private void processEnglishFile(String dataFile, String suffix)
			throws IOException {
		
		FileOutputStream fos = new FileOutputStream(dataFile + ".full.csv"); 
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		CSVWriter writer = new CSVWriter(osw);
		
		Document doc = Jsoup.parse(new File(dataFile), "UTF-8");
		
		boolean firstRow = true;
		
		/**
		 * Consume data
		 */
		Elements questions = doc.getElementsByTag("Question");		
		int numberOfQuestions = questions.size();
		int questionNumber = 1;
		
		List<String> header = new ArrayList<>();
		
		header.add("QID");
		header.add("QCATEGORY");
		header.add("QDATE");
		header.add("QUSERID");
		header.add("QTYPE");
		header.add("QGOLD_YN");
		header.add("QSUBJECT");
		header.add("QBODY");	
		header.add("CID");
		header.add("CUSERID");
		header.add("CGOLD");
		header.add("CGOLD_YN");
		header.add("CSUBJECT");
		header.add("CBODY");
		
		for(Element question : questions) {
			System.out.println("[INFO]: Processing " + questionNumber++ + " out of " + numberOfQuestions);
			/**
			 * Parse question node
			 */
			String qid = question.attr("QID");
			String qcategory = question.attr("QCATEGORY");
			String qdate = question.attr("QDATE");
			String quserid = question.attr("QUSERID");
			String qtype = question.attr("QTYPE");
			String qgold_yn = question.attr("QGOLD_YN");		
			String qsubject = question.getElementsByTag("QSubject").get(0).text();
			String qbody = question.getElementsByTag("QBody").get(0).text();	

			/**
			 * Parse comment nodes
			 */
			Elements comments = question.getElementsByTag("Comment");
			for(Element comment : comments) {
				String cid = comment.attr("CID");
				String cuserid = comment.attr("CUSERID");
				String cgold = comment.attr("CGOLD");
				String cgold_yn = comment.attr("CGOLD_YN");
				String csubject = comment.getElementsByTag("CSubject").get(0).text();
				String cbody = comment.getElementsByTag("CBody").get(0).text();

				
				/**
				 * Produce output line
				 */
				
				if(firstRow) {
					String[] csvHeader = {
							"QID",
							"QCATEGORY",
							"QDATE",
							"QUSERID",
							"QTYPE",
							"QGOLD_YN",
							"QSUBJECT",
							"QBODY",
							"CID",
							"CUSERID",
							"CGOLD",
							"CGOLD_YN",
							"CSUBJECT",
							"CBODY",
					};	
					writer.writeNext(csvHeader);
					firstRow = false;
				}
				
				String[] row = {
						qid,
						qcategory,
						qdate,
						quserid,
						qtype,
						qgold_yn,
						StringEscapeUtils.escapeCsv(qsubject),
						StringEscapeUtils.escapeCsv(qbody),
						cid,
						cuserid,
						cgold,
						cgold_yn,
						StringEscapeUtils.escapeCsv(csubject),
						StringEscapeUtils.escapeCsv(cbody)
				};
				
				writer.writeNext(row);
			}
		}

		writer.close();
		osw.close();
		fos.close();
	}

}
