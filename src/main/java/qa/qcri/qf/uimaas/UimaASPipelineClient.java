package qa.qcri.qf.uimaas;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.aae.client.UimaAsynchronousEngine;
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.pipeline.retrieval.Analyzable;
import qa.qcri.qf.pipeline.trec.TrecQuestionsReader;

public class UimaASPipelineClient {
	
	public static void main(String[] args) {
		
		UimaAsynchronousEngine uimaAsEngine = new BaseUIMAAsynchronousEngine_impl();

		uimaAsEngine.addStatusCallbackListener(new MyStatusCallbackListener());

		Map<String,Object> appCtx = new HashMap<String,Object>();
		appCtx.put(UimaAsynchronousEngine.ServerUri, "tcp://localhost:61616");
		appCtx.put(UimaAsynchronousEngine.ENDPOINT, "iyasSamplePipeline");
		appCtx.put(UimaAsynchronousEngine.CasPoolSize, 2);
		try {
			uimaAsEngine.initialize(appCtx);

			ReadFile in = new ReadFile("data/trec-en/questions.txt");
			
			long startTime = System.currentTimeMillis();
			
			while(in.hasNextLine()) {
				String question = Lists.newArrayList(Splitter.on(" ")
						.limit(2).split(in.nextLine())).get(1);
				
				CAS cas = uimaAsEngine.getCAS();
				cas.setDocumentText(question);
				cas.setDocumentLanguage("en");
				
				// Send Cas to service for processing
				uimaAsEngine.sendCAS(cas);
			}
			
			uimaAsEngine.collectionProcessingComplete();
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println(elapsedTime);
			
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
