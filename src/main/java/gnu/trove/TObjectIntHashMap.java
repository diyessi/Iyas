package gnu.trove;

import gnu.trove.map.TObjectIntMap;

/**
 * Mallet uses a very old version of trove.  This maps the class Mallet uses to
 * its newer version.
 */
public class TObjectIntHashMap extends gnu.trove.map.hash.TObjectIntHashMap<Object> {

	public TObjectIntHashMap() {
		super();
		// TODO Auto-generated constructor stub
	}

	public TObjectIntHashMap(int initialCapacity, float loadFactor, int noEntryValue) {
		super(initialCapacity, loadFactor, noEntryValue);
		// TODO Auto-generated constructor stub
	}

	public TObjectIntHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		// TODO Auto-generated constructor stub
	}

	public TObjectIntHashMap(int initialCapacity) {
		super(initialCapacity);
		// TODO Auto-generated constructor stub
	}

	public TObjectIntHashMap(TObjectIntMap<? extends Object> map) {
		super(map);
		// TODO Auto-generated constructor stub
	}

}
