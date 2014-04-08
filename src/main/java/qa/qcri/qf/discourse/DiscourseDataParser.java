package qa.qcri.qf.discourse;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class DiscourseDataParser {
	
	public static final String TREC_DATA = "data/trec-en/terrier.BM25b0.75_0";
	
	public static final String DISCOURSE_DATA = "data/discourse/QA_output_2nd_format.txt";
	
	public static final Pattern ENTITY_PATTERN = Pattern.compile("& ([A-Z]+) ;");
	
	public static void main(String[] args) throws UIMAException {
		
		Analyzer ae = instantiateAnalyzer();
		JCas cas = JCasFactory.createJCas();
		
		ReadFile trecIn = new ReadFile(TREC_DATA);
		ReadFile discourseIn = new ReadFile(DISCOURSE_DATA);
		
		while(trecIn.hasNextLine()) {
			
			List<String> trecLine = Lists.newArrayList(Splitter.on(" ").limit(6)
					.split(trecIn.nextLine().trim()));
			List<String> discourseLine = Lists.newArrayList(Splitter.on(" ").limit(3)
					.split(discourseIn.nextLine().trim()));
			
			String passageId = trecLine.get(1);
			String passageText = trecLine.get(5);
			Analyzable passage = new SimpleContent(passageId, passageText);
			
			ae.analyze(cas, passage);
			
			String discourseText = discourseLine.get(2);
			
			discourseText = discourseText
					.replaceAll("\\[[A-Z_-]+", "")
					.replaceAll("\\]", "")
					.replaceAll(" +", " ")
					.trim();
			
			Matcher matcher = ENTITY_PATTERN.matcher(discourseText);
			while (matcher.find()) {
				discourseText = discourseText.replace("& " + matcher.group(1) + " ;",
						"&" + matcher.group(1) + ";");
			}
				
			List<String> tokens = new ArrayList<>();		
			for(Token token : JCasUtil.select(cas, Token.class)) {
				tokens.add(token.getCoveredText());
			}
			
			String tokenizedText = Joiner.on(" ").join(tokens);
			
			if(!tokenizedText.equals(discourseText)) {
				System.out.println(passageId);
				System.out.println("D- " + discourseText);
				System.out.println("T- " + tokenizedText);
				System.out.println("");
			}
			
			cas.reset();
			
			//break;
		}
	
		trecIn.close();
		discourseIn.close();
	}
	
	private static Analyzer instantiateAnalyzer() throws UIMAException {
		Analyzer ae = new Analyzer(new UIMAFilePersistence("CASes/trec-en/"));

		ae.addAEDesc(createEngineDescription(StanfordSegmenter.class))
				.addAEDesc(createEngineDescription(StanfordPosTagger.class))
				.addAEDesc(createEngineDescription(StanfordLemmatizer.class))
				.addAEDesc(createEngineDescription(IllinoisChunker.class));

		return ae;
	}
	
}
