package qa.qcri.qf.pipeline;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.Iterator;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import qa.qcri.qf.annotators.IllinoisChunker;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.SampleFileReader;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.treemarker.MarkTreesOnRepresentation;
import qa.qcri.qf.treemarker.MarkTwoAncestors;
import qa.qcri.qf.trees.RichNode;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerChunkerTT4J;

public class Pipeline {
        
        public static final String SAMPLE_CONTENT_PATH = "data/sample.txt";

        @Test
        public void runPipeline() throws UIMAException {
                
                String parameterList = Joiner.on(",").join(new String[] {
                                RichNode.OUTPUT_PAR_LEMMA, RichNode.OUTPUT_PAR_TOKEN_LOWERCASE});
                
                TreeSerializer ts = new TreeSerializer().enableRelationalTags();
                
                Analyzer ae = new Analyzer(new UIMAFilePersistence("CASes/"));
                
                ae.addAEDesc(createEngineDescription(BreakIteratorSegmenter.class))
                        .addAEDesc(createEngineDescription(StanfordParser.class))
                        .addAEDesc(createEngineDescription(IllinoisChunker.class));
                        //.addAEDesc(createEngineDescription(TreeTaggerChunkerTT4J.class));
                
                Iterator<Analyzable> content = new SampleFileReader(SAMPLE_CONTENT_PATH)
                                .iterator();

                JCas cas = JCasFactory.createJCas();
                
                while (content.hasNext()) {
                        Analyzable analyzable = content.next();
                        ae.analyze(cas, analyzable);

                        RichNode posChunkTree = RichTree.getPosChunkTree(cas);
                        String tree = ts.serializeTree(posChunkTree, parameterList);
                        System.out.println(tree);
                }
                
                System.out.println("\n+++++++++++++++++++++\n");
                
                JCas cas_a = JCasFactory.createJCas();
                JCas cas_b = JCasFactory.createJCas();
                ae.analyze(cas_a, new SimpleContent("1", ""));
                ae.analyze(cas_b, new SimpleContent("2", ""));
                
                TokenTree aTree = RichTree.getPosChunkTree(cas_a);
                TokenTree bTree = RichTree.getPosChunkTree(cas_b);
                
                MarkTreesOnRepresentation marker = new MarkTreesOnRepresentation(new MarkTwoAncestors());
                marker.markTrees(aTree, bTree, parameterList);
                
                System.out.println(ts.serializeTree(aTree, parameterList));
                System.out.println(ts.serializeTree(bTree, parameterList));                
                
        }
        
}