package qa.qcri.qf.datagen.rr;

import java.util.List;

import qa.qcri.qf.datagen.DataObject;

public interface Reranking {

	public void generateData(DataObject questionObject,
			List<DataObject> candidateObjects);
}
