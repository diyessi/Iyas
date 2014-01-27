package qa.qcri.qf.datagen;

import java.util.ArrayList;
import java.util.List;

import util.Pair;

public class Pairer {
	
	public static List<Pair<DataPair, DataPair>> pair(List<DataPair> dataPairs) {
		List<Pair<DataPair, DataPair>> pairs = new ArrayList<>();
		
		List<DataPair> relevantPairs = new ArrayList<>();
		List<DataPair> irrelevantPairs = new ArrayList<>();
		
		for(DataPair dataPair : dataPairs) {
			if(dataPair.isPositive()) {
				relevantPairs.add(dataPair);
			} else {
				irrelevantPairs.add(dataPair);
			}
		}
		
		boolean flip = false;
		
		for(DataPair relevantPair : relevantPairs) {
			for(DataPair irrelevantPair : irrelevantPairs) {
				DataPair left = relevantPair;
				DataPair right = irrelevantPair;
				
				if(flip) {
					left = irrelevantPair;
					right = relevantPair;
				}
				
				Pair<DataPair, DataPair> pair = new Pair<>(left, right);
				pairs.add(pair);
				
				flip = !flip;
			}		
		}
		
		return pairs;
	}
}
