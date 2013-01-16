package view.model.cache;

import java.util.Date;

public class FIFOAddColumns implements AddColumns<EntryL>{

	@Override
	public EntryL createNewEntry() {
		EntryL e = new EntryL();
		e.p = new Date().getTime();
		return e;
		
	}

	@Override
	public int getColCount() {
		return 1;
	}

	@Override
	public String getColname(int col) {
		if(col==0){
			return "timestampCreated";
		}
		return null;
	}

	@Override
	public int getEvictionValue(EntryL entry) {
		return (int)(Long.MAX_VALUE-entry.p);
	}

	@Override
	public boolean isEligible(int row, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public boolean isEligible(EntryL entry, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public void setValues(EntryL entry, int pid, short instrType, int... keys) {
	}

}
