package qa.qcri.qf.uimaas;

import java.util.List;

import org.apache.uima.aae.client.UimaAsBaseCallbackListener;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class MyStatusCallbackListener extends UimaAsBaseCallbackListener {

	public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
		if (aStatus != null) {
			if (aStatus.isException()) {
				System.err.println("Error on process CAS call to remote service:");
				List<Exception> exceptions = aStatus.getExceptions();
				for (int i = 0; i < exceptions.size(); i++) {
					((Throwable) exceptions.get(i)).printStackTrace();
				}
			}
			
			try {
				JCas cas = aCas.getJCas();

				for(Token token : JCasUtil.select(cas, Token.class)) {
					System.out.println(token.getCoveredText() + " " + token.getPos().getPosValue());
				}

			} catch (CASException e) {
				e.printStackTrace();
			}
		}
	}
}
