package it.unitn.limosine.opinion;

import it.unitn.limosine.syntax.dependency.LTHParserFast;
import it.unitn.limosine.types.opinion.OpinionExpression;
import it.unitn.limosine.types.segmentation.Token;
import it.unitn.limosine.types.syntax.CoNLL2008DependencyTree;
import it.unitn.limosine.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import se.lth.cs.nlp.depsrl.format.CoNLL2008Format;
import se.lth.cs.nlp.depsrl.format.PAStructure;
import se.lth.cs.nlp.nlputils.core.Ax;
import se.lth.cs.nlp.nlputils.core.Triple;
import se.lth.cs.nlp.nlputils.depgraph.DepGraph;
import se.lth.cs.nlp.nlputils.depgraph.DepNode;
import se.lth.cs.nlp.nlputils.ml_long.Classifier;
import se.lth.cs.nlp.nlputils.ml_long.SparseVector;
import se.lth.cs.nlp.opinions.TaggingModel;

import mpqa_structlearn.fullsystem.Server;
import mpqa_structlearn.io.MPQAExpression;
import mpqa_structlearn.io.MPQASentence;
import mpqa_structlearn.sequences.SentenceData;
import mpqareader.SubjectivityLexicon;

/***
 * Interface to the opinion miner tool
 * Assumes that the LTHparser has been called before
 * 
 * Output to CAS: opinion expressions
 * 
 * @author bplank
 *
 */
public class OpinionMiner extends JCasAnnotator_ImplBase {

	private final static int k = 1;
	//private final static boolean extractHolders = false; //not yet done
	
	private SubjectivityLexicon subjLex;
	private TaggingModel baseSeqModel;
	private Classifier<SparseVector, Integer> polCl;
	private Classifier<SparseVector, Integer> intCl;
	
	@SuppressWarnings("unchecked")
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			
			SharedModel sharedModel = (SharedModel) getContext().getResourceObject(
				"OpinionMiningModel");
			String packageName = sharedModel.getPath();		
		
		
			ObjectInputStream ois = new ObjectInputStream(
					Ax.openFileStream(packageName));

			System.err.println("Loading subjectivity lexicon...");
			subjLex = (SubjectivityLexicon) ois.readObject();

			System.err.println("Loading sequence model...");
			baseSeqModel = (TaggingModel) ois.readObject();

			//for now not implemented
			/*LocalLinkClassifier llc = null;
			if (extractHolders) {
				System.err.println("Loading holder link model...");
				llc = (LocalLinkClassifier) ois.readObject();
			} else */
				ois.readObject();

			System.err.println("Loading polarity classifier...");
			polCl = (Classifier<SparseVector, Integer>) ois.readObject();
			System.err.println("Loading intensity classifier...");
			intCl = (Classifier<SparseVector, Integer>) ois.readObject();

			ois.close();
			ois = null;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		// needs input from LTH parser (CoNLL 2008 trees)

		FSIndex<Annotation> depTreeIndex = cas
				.getAnnotationIndex(CoNLL2008DependencyTree.type);

		// iterate over all sentences

		Iterator<Annotation> treeIterator = depTreeIndex.iterator();

		while (treeIterator.hasNext()) {
			CoNLL2008DependencyTree tree = (CoNLL2008DependencyTree) treeIterator.next();
			
			Triple<DepGraph, DepGraph, List<PAStructure>> parse;
			try {
				parse = CoNLL2008Format
						.readNextGraph(new BufferedReader(new StringReader(tree.getRawParse())));
		
			if (parse == null) {
				getContext().getLogger().log(
						Level.SEVERE,
						"Could not get parse for sentence: "
								+ tree.getCoveredText());
				throw new RuntimeException("could not read parse!");
			} else {

				DepGraph dg = parse.first;
				ArrayList<PAStructure> pas = (ArrayList<PAStructure>) parse.third;
				
				// get tokens of sentence
				List<AnnotationFS> myTokens = JCasUtility.selectCovered(cas,
						Token.class, tree);
				
				if ((dg.nodes.length-1) != myTokens.size()) {
					throw new AnalysisEngineProcessException(LTHParserFast.MESSAGE_DIGEST, "tokens_tags_error", 
							new Object[]{tree.getCoveredText()});
				}

				SentenceData cands = Server.kbest(baseSeqModel, subjLex, dg,
						pas, k);

				MPQASentence msen;

				// k=1
				msen = cands.candidates.get(0).mpqaSentence;
				Server.addPolInt(msen, polCl, intCl, subjLex);

		
				for (MPQAExpression expr : msen.exprs) {
					if (expr != null && !expr.type.equals("os")) { 
						// ignore objective speech for now
						
						Token startToken = null;
						Token endToken = null;
						//for (DepNode n : expr.span) {
						for (int i=0; i < expr.span.size(); i++) {
							DepNode n = expr.span.get(i);
							Token token = (Token) myTokens.get(n.position-1); //position 0 is root, thus ignore
							if (i==0)
								startToken = token;
							if (i>0 && i==(expr.span.size()-1))
								endToken = token;
							
						}
						OpinionExpression opinionExpr = new OpinionExpression(cas);
						opinionExpr.setBegin(startToken.getBegin());
						opinionExpr.setEnd(endToken != null ? endToken.getEnd() : startToken.getEnd()); 
						opinionExpr.setPolarity(expr.polarity);
						opinionExpr.setIntensity(expr.intensity);
						opinionExpr.setAnnotatorId(OpinionMiner.class.getCanonicalName());
						opinionExpr.setSentence(tree.getSentence());
						opinionExpr.addToIndexes();
						
					}
				}
				
			}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	


}
