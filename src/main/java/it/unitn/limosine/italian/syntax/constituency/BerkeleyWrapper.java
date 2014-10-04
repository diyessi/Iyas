package it.unitn.limosine.italian.syntax.constituency;

import it.unitn.limosine.types.pos.Pos;
import it.unitn.limosine.types.segmentation.Sentence;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.ConstituencyTree;
import it.unitn.limosine.util.JCasUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.Corpus;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Lexicon;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.PCFGLA.TreeAnnotations;
import edu.berkeley.nlp.io.PTBLineLexer;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.util.Numberer;

public class BerkeleyWrapper extends JCasAnnotator_ImplBase {

	public static final String PARAM_GRAMMARFILE = "gr";
	@ConfigurationParameter(name=PARAM_GRAMMARFILE, description="Grammarfile (Required)")
	private String grFileName;

	public static final String PARAM_TOKENIZE = "tokenize";
	@ConfigurationParameter(name=PARAM_TOKENIZE, defaultValue="false", description="Tokenize input first. "
			+ "(Default: false=text is already tokenized)")
	private boolean tokenize;

	public static final String PARAM_VITERBI = "viterbi";
	@ConfigurationParameter(name=PARAM_VITERBI, defaultValue="false", description="Copute viterbi derivation instead of max-rule tree (Default: max-rule")
	public boolean viterbi;

	public static final String PARAM_BINARIZE = "binarize";
	@ConfigurationParameter(name=PARAM_BINARIZE, defaultValue="false", description="Output binarized trees. (Default: false")
	public boolean binarize;

	public static final String PARAM_SCORES = "scores";
	@ConfigurationParameter(name=PARAM_SCORES, defaultValue="false", description="Output inside scores (only for binarized viterbi trees)")
	public boolean scores;

	public static final String PARAM_SUBSTATES = "substates";
	@ConfigurationParameter(name=PARAM_SUBSTATES, defaultValue="false", description="Output subcategories (only for binarized viterbi trees). (Default: false)")
	public boolean substates;

	public static final String PARAM_ACCURATE = "accurate";
	@ConfigurationParameter(name=PARAM_ACCURATE, defaultValue="false", description="Set thresholds for accuracy. (Default: set thersholds for efficiency)")
	public boolean accurate;

	public static final String PARAM_CONFIDENCE = "confidence";
	@ConfigurationParameter(name=PARAM_CONFIDENCE, defaultValue="false", description="Output confidence measure, i.e. tree likelihood: P(T|w) (Default: false)")
	public boolean confidence;

	public static final String PARAM_CHINESE = "chinese";
	@ConfigurationParameter(name=PARAM_CHINESE, defaultValue="false", description="Enable some Chinese specific features in the lexicon")
	public boolean chinese;

	public static final String PARAM_MAXLENGTH = "maxLength";
	@ConfigurationParameter(name=PARAM_MAXLENGTH, defaultValue="200", description="Maximum sentence length (Default = 200)")
	public int maxLength;	

	public static final String PARAM_USEGOLDPOS = "useGoldPos";	
	@ConfigurationParameter(name=PARAM_USEGOLDPOS, defaultValue="false", description="Read data in CoNLL format, including gold part of speech tags.")
	public boolean goldPos;

	private CoarseToFineMaxRuleParser parser;	
	private PTBLineLexer tokenizer = null;


	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		double threshold = 1.0;

		ParserData pData = ParserData.Load(grFileName);
		if (pData == null) {
			throw new ResourceInitializationException(
					new IllegalArgumentException("Failed to load grammar from file " + grFileName + "."));
		}
		Grammar grammar = pData.getGrammar();
		Lexicon lexicon = pData.getLexicon();
		Numberer.setNumberers(pData.getNumbs());

		if (tokenize) tokenizer = new PTBLineLexer();

		if (chinese) Corpus.myTreebank = Corpus.TreeBankType.CHINESE;

		parser = new CoarseToFineMaxRuleParser(grammar, lexicon, threshold, -1, viterbi, substates, scores, accurate, false, true, true);	
	}


	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		// needs input from tokenizer
		FSIndex<Annotation> sentencesIndex = cas.getAnnotationIndex(Sentence.type);

		// iterate over all sentences
		Iterator<Annotation> sentencesIterator = sentencesIndex.iterator();
		while (sentencesIterator.hasNext()) { 
			Sentence aSentence = (Sentence) sentencesIterator.next();

			List<String> sentence = null;
			List<String> posTags = null;

			if (goldPos) {
				sentence = new ArrayList<>();
				posTags = new ArrayList<>();
				List<AnnotationFS> myPos = JCasUtility.selectCovered(cas, Pos.class, aSentence);
				for (AnnotationFS p : myPos) { 
					Pos pos = (Pos)p;
					Token tok = pos.getToken();
					sentence.add(tok.getNormalizedText());
					posTags.add(pos.getPostag());
				}
			} else {
				try {
					String line = aSentence.getCoveredText();
					if (!tokenize) sentence = Arrays.asList(line.split(" "));	
					else {
						sentence = tokenizer.tokenizeLine(line);
					}
				} catch (IOException ex) { 
					ex.printStackTrace();
				}
			}

			if (sentence.size() >= maxLength) {
				System.err.println("Skipping sentence with " + sentence.size() + " words since it is too long.");
				continue;
			}

			Tree<String> parsedTree = parser.getBestConstrainedParse(sentence, posTags, null);
			if (goldPos && parsedTree.getChildren().isEmpty()) { // parse error when using goldPos, try without
				parsedTree = parser.getBestConstrainedParse(sentence, null, null);
			}

			if (!binarize) parsedTree = TreeAnnotations.unAnnotateTree(parsedTree);
			ConstituencyTree pennTree = new ConstituencyTree(cas);
			pennTree.setBegin(aSentence.getBegin());
			pennTree.setEnd(aSentence.getEnd());
			pennTree.setRawParse(parsedTree.toString().trim());
			pennTree.setAnnotatorId(getClass().getCanonicalName());
			pennTree.setSentence(aSentence);
			pennTree.addToIndexes();
		}			 
	} 
}
