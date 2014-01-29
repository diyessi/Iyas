package qa.qcri.qf.pipeline;

import java.util.List;

import org.apache.uima.UIMAException;
import org.junit.Assert;
import org.junit.Test;

import util.MinMax;
import util.Pair;

public class MinMaxTest {

	@Test
	public void testMinMax() throws UIMAException {
		MinMax<String> minMax = new MinMax<>();
		
		minMax.look(.5, ",");
		minMax.look(.1, "Hello");
		minMax.look(.3, "my");
		minMax.look(.2, "name");
		minMax.look(.9, "Iyas");
		minMax.look(.4, "is");
		
		Assert.assertSame("Hello", minMax.getMin());
		Assert.assertSame("Iyas", minMax.getMax());
	}
	
	@Test
	public void testObservedValues() throws UIMAException {
		MinMax<String> minMax = new MinMax<String>().keepSortedList();
		
		minMax.look(.5, ",");
		minMax.look(.1, "Hello");
		minMax.look(.3, "my");
		minMax.look(.2, "name");
		minMax.look(.9, "Iyas");
		minMax.look(.4, "is");
		
		List<Pair<Double, String>> pairs = minMax.getSortedList();
		
		Assert.assertSame("Hello", pairs.get(0).getB());
		Assert.assertSame("name", pairs.get(1).getB());
		Assert.assertSame("my", pairs.get(2).getB());
		Assert.assertSame("is", pairs.get(3).getB());
		Assert.assertSame(",", pairs.get(4).getB());
		Assert.assertSame("Iyas", pairs.get(5).getB());
	}

}