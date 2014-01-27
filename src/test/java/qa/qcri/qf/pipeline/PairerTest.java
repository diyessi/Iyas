package qa.qcri.qf.pipeline;

import java.util.ArrayList;
import java.util.List;

import qa.qcri.qf.datagen.DataObject;
import qa.qcri.qf.datagen.DataPair;
import qa.qcri.qf.datagen.Pairer;
import util.Pair;

public class PairerTest {
	
	public static void main(String[] args) {
		
		List<DataPair> dataPairs = new ArrayList<>();
		
		DataPair
		dataPair = new DataPair(1.0, "p-1", null, null,
				new DataObject(1.0, "q-1", null, null),
				new DataObject(1.0, "c-1", null, null));
		
		dataPairs.add(dataPair);
		
		dataPair = new DataPair(1.0, "p-2", null, null,
				new DataObject(1.0, "q-2", null, null),
				new DataObject(0.0, "c-2", null, null));
		
		dataPairs.add(dataPair);
		
		dataPair = new DataPair(1.0, "p-3", null, null,
				new DataObject(1.0, "q-3", null, null),
				new DataObject(1.0, "c-3", null, null));
		
		dataPairs.add(dataPair);
		
		dataPair = new DataPair(1.0, "p-4", null, null,
				new DataObject(1.0, "q-4", null, null),
				new DataObject(0.0, "c-4", null, null));
		
		dataPairs.add(dataPair);
		
		dataPair = new DataPair(1.0, "p-5", null, null,
				new DataObject(1.0, "q-5", null, null),
				new DataObject(1.0, "c-5", null, null));
		
		dataPairs.add(dataPair);
		
		List<Pair<DataPair, DataPair>> pairs = Pairer.pair(dataPairs);
		
		for(Pair<DataPair, DataPair> pair : pairs) {
			DataPair left = pair.getA();
			DataPair right = pair.getB();
			System.out.println(left.getId() + ": " + "<" + left.getA().getId() + " "
					+ left.getB().getId() + "> "
					+ right.getId() + ": " + "<" + right.getA().getId() + " "
					+ right.getB().getId() + ">"
					+ " left rel: " + left.isPositive()
					+ ", right rel: " + right.isPositive());
		}
	}
}
