package view.model.cache;

public class MemCacheAddColumns implements AddColumns<EntryI>{

	public EntryI createNewEntry() {
		return new EntryI() ;
	}

	public int getColCount() {
		return 1;
	}

	public String getColname(int col) {
		if(col==0){
			return "key";
		}
		return null;
	}

	public int getEvictionValue(EntryI entry) {
		return 0;
	}

	public boolean isEligible(int row, int key, int pid, short instrType) {
		return true;
	}

	public boolean isEligible(EntryI entry, int key, int pid, short instrType) {
		return true;
	}

	public void setValues(EntryI entry, int pid, short instrType, int... keys) {
		entry.p = keys[0];
	}

}


