package qa.qcri.qf.annotators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.uimafit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.descriptor.ConfigurationParameter;

import cc.mallet.topics.ParallelTopicModel;
import qa.qcri.qf.lda.LdaModelResource;
import qa.qcri.qf.lda.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import qa.qcri.qf.type.LdaTopic;
import qa.qcri.qf.type.LdaTopicDistribution;

@TypeCapability(
		inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" },
		outputs = { "qa.qcri.qf.type:LdaTopicDistribution", 
					"qa.qcri.qf.type:LdaTopic" 
		}
)
public class LdaAnnotator extends JCasAnnotator_ImplBase {
	
	public static final String MODEL_KEY = "uima.ldaModel";
	@ExternalResource(key = MODEL_KEY)
	LdaModelResource resource;	
	
	private ParallelTopicModel model = null;
	
	public static final String PARAM_NUM_ITERATIONS = "numIterations";
	@ConfigurationParameter(name = PARAM_NUM_ITERATIONS, mandatory = false)
	private int numIterations = 100;
	
	public static final String PARAM_THINNING = "thinning";
	@ConfigurationParameter(name = PARAM_THINNING, mandatory = false)
	private int thinning = 1;
	
	public static final String PARAM_BURNIN = "burnIn";
	@ConfigurationParameter(name = PARAM_BURNIN, mandatory = false)
	private int burnIn = 5;
	
	public static final String PARAM_SEED = "randomSeed";
	@ConfigurationParameter(name = PARAM_SEED, mandatory = false, description = "random seed for teh gibbs sampler")
	private int randomSeed = 123;
	
	public static final String PARAM_LIST = "paramList";
	private String paramList = RichNode.OUTPUT_PAR_TOKEN;
	
	private Alphabet alphabet;
	private TopicInferencer inferencer;

	private final Logger logger = LoggerFactory.getLogger(LdaAnnotator.class);

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
	}

	private void init() {
		model = resource.getModel();
		alphabet = model.getAlphabet();
		inferencer = new TopicInferencer(model);
		inferencer.setRandomSeed(randomSeed);
	}
	
	@SuppressWarnings("unused")
	private void printTypeTopicCounts(int type) {
		int[] topicCounts = model.typeTopicCounts[type];

		int index = 0;
		logger.debug("WordTopic counts");
		while (index < topicCounts.length &&
			   topicCounts[index] > 0) {

			int topic = topicCounts[index] & model.topicMask;
			int count = topicCounts[index] >> model.topicBits;
			
			logger.debug("{} {}:{}", type, topic, count);
			index++;
		}
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		if (model == null)
			init();

		Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
		
		// Create Mallet feature sequence 
		FeatureSequence ret = new FeatureSequence(alphabet, tokens.size());
		List<Token> tokensWithTopic = new ArrayList<>();
		for (Token token : tokens) {
			// Lookup the token in the alphabet without expanding the alphabet
			int featIndex = alphabet.lookupIndex(new RichTokenNode(token).getRepresentation(paramList), false);
			if (featIndex >= 0) {
				ret.add(featIndex);
				tokensWithTopic.add(token);
			}
		}			
		Instance inst = new Instance(ret, null, null, null);
		
		logger.debug("Number of features: {}", ret.size());
		logger.debug("Features: {}", ret.toString());
		logger.debug("Running LDA topic inferencer.");
		double[] testProbabilities = inferencer.getSampledDistribution(inst,
				numIterations, thinning, burnIn);

		LdaTopicDistribution topicDist = new LdaTopicDistribution(jCas);
		DoubleArray d = new DoubleArray(jCas, testProbabilities.length);
		d.copyFromArray(testProbabilities, 0, 0, testProbabilities.length);

		topicDist.setTopicDistribution(d);
		topicDist.addToIndexes();

		int[] topics = inferencer.getTopicPerWordAssignment();
		
		// Add topic assignments to the CAS.
		int pos = 0;
		for (Token token : tokensWithTopic) {
			LdaTopic ldaTopic = new LdaTopic(jCas, token.getBegin(), token.getEnd());
			String topic = String.valueOf(topics[pos++]);
			ldaTopic.setTopic(topic);
			ldaTopic.addToIndexes();
		}
	}
	
}
