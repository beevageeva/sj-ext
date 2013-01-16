package view.model.cache;

import java.util.Date;

public class NFUAddColumns implements AddColumns<EntryBL>{

	@Override
	public EntryBL createNewEntry() {
		return new EntryBL();
	}

	@Override
	public int getColCount() {
		return 2;
	}

	@Override
	public String getColname(int col) {
		switch(col){
		case 0:
			return "numberTimesUsed";
		case 1:
			return "referenced";
		}
		
		return null;
	}

	@Override
	public int getEvictionValue(EntryBL entry) {
		return (int)( Long.MAX_VALUE - entry.p -( entry.q?1:0));
	}

	@Override
	public boolean isEligible(int row, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public boolean isEligible(EntryBL entry, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public void setValues(EntryBL entry, int pid, short instrType, int... keys) {
		entry.q = true;
		entry.p = new Date().getTime();
	}

	
	
}
