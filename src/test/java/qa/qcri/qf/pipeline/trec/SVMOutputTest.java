package qa.qcri.qf.pipeline.trec;

import junit.framework.Assert;

import org.junit.Test;

import qa.qcri.qf.fileutil.ReadFile;

public class SVMOutputTest {

	@Test
	public void testRelevanceAndExampleLabelsCorrespondences() {
		ReadFile testCandidatesFile = new ReadFile("data/mengqui/test-less-than-40.num-recovered.txt");
		ReadFile testExamplesFile = new ReadFile("data/mengqui/test/svm.test");

		boolean error = false;

		while (testCandidatesFile.hasNextLine()) {
			boolean candidateRelevancy = testCandidatesFile.nextLine().trim()
					.split(" ")[4].equals("true");

			boolean exampleRelevancy = testExamplesFile.nextLine().trim()
					.startsWith("+1");

			if (candidateRelevancy != exampleRelevancy) {	
				error = true;
			}
		}
		
		testCandidatesFile.close();
		testExamplesFile.close();

		Assert.assertEquals(false, error);
	}

}
