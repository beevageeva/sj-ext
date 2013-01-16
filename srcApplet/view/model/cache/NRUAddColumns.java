package view.model.cache;

import trace.Instr;

/**
 * keeps for each entry a bit of reference( 1 if the page was ref , 0 otherwise )
 * in eviction algorithm verifies the modified bit and the reference bit and thus comparts
 * the entries in 4 classes (r=0 , m=0; r=0,m=1 ; r=1,m=0; r=1 , m=1 ) then selects randomly
 * an entry from the first class that appears in this order
 *
 */


public class NRUAddColumns implements AddColumns<EntryBB>{

	@Override
	public EntryBB createNewEntry() {
		return new EntryBB();
	}

	@Override
	public int getColCount() {
		return 2;
	}

	@Override
	public String getColname(int col) {
		switch(col){
			case 0:
				return "referenced";
			case 1:
				return "modified";
		}
		return null;
	}

	@Override
	public int getEvictionValue(EntryBB entry) {
		return (entry.b?0:1) + (entry.q?0:2);
	}

	@Override
	public boolean isEligible(int row, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public boolean isEligible(EntryBB entry, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public void setValues(EntryBB entry, int pid, short instrType, int... keys) {
		if(instrType==Instr.MODIFDATA){
			entry.q = true;
		}
		entry.b = true;
	}

	
	
}
