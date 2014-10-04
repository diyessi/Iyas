package qa.qcri.qf.italian.syntax.constituency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import it.unitn.limosine.util.JCasUtility;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

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
			
			List<Token> tokens = Lists.newArrayList(JCasUtil.selectCovered(cas, Token.class, aSentence));
			List<String> sentence = null;
			List<String> posTags = null;
			
			if (goldPos) {
				sentence = new ArrayList<>();
				posTags = new ArrayList<>();
				List<AnnotationFS> myToks = JCasUtility.selectCovered(cas, Token.class, aSentence);
				for (AnnotationFS t : myToks) { 
					Token tok = (Token)t;
					POS pos = tok.getPos();
					sentence.add(tok.getCoveredText());
					posTags.add(pos.getPosValue());
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
					
			createConstituentAnnotationFromTree(cas, parsedTree, null, tokens, new MutableInt(0));
		}
	}
	
	private Annotation createConstituentAnnotationFromTree(JCas aJCas, Tree<String> aNode,
			Annotation aParentFS, List<Token> aTokens, MutableInt aIndex) {
		// If the node is a word-level constituent node (== POS):
		// create parent link on token and (if not turned off) create POS tag
		if (aNode.isPreTerminal()) { 
			Token token = aTokens.get(aIndex.intValue());
			
			// link token to its parent
			if (aParentFS  != null) { 
				token.setParent(aParentFS);
			}
			
			aIndex.add(1);
			
			return token;
		} else {		
			
			// Check if node is a constituent node on sentence or phrase-level
			String typeName = aNode.getLabel();
			
			// create the necessary obejcts and methods
			String constituentTypeName =  typeName;
			
			Constituent constAnno = new Constituent(aJCas, 0, 0);
			constAnno.setConstituentType(constituentTypeName);
			
			// link to parent
			if (aParentFS != null) { 
				constAnno.setParent(aParentFS);
			}
			
			// Do we have any children?
			List<Annotation> childAnnotations = new ArrayList<Annotation>();
			for (Tree<String> child : aNode.getChildren()) { 
				Annotation childAnnotation = createConstituentAnnotationFromTree(aJCas, child, 
							constAnno, aTokens, aIndex);
				if (childAnnotation != null) {
					childAnnotations.add(childAnnotation);
				}
			}
			
			constAnno.setBegin(childAnnotations.get(0).getBegin());
			constAnno.setEnd(childAnnotations.get(childAnnotations.size()-1).getEnd());
			
			// Now that we know how many children we havw, link annotation of
			// current not with its children
			FSArray childArray = (FSArray) FSCollectionFactory.createFSArray(aJCas,
					childAnnotations);
			constAnno.setChildren(childArray);
			
			// write annotation for current node to index
			aJCas.addFsToIndexes(constAnno);
			
			return constAnno;			
		}
		
	}
}
