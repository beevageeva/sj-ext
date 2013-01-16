package view.model.cache;


public class PTCacheAddColumn implements AddColumns<EntryII>{

	public EntryII createNewEntry() {
		return new EntryII();
	}

	public int getColCount() {
		return 2;
	}

	public String getColname(int col) {
		if(col==0){
			return "vpn";
		}
		else if(col==1){
			return "rpn";
		}
		return null;
	}

	public int getEvictionValue(EntryII entry) {
		return 0;
	}

	public boolean isEligible(int row,int key, int pid, short instrType) {
		return true;
	}

	public boolean isEligible(EntryII entry,int key, int pid, short instrType) {
		return true;
	}

	public void setValues(EntryII entry, int pid, short instrType, int... keys) {
		assert keys.length==2;
		entry.p = keys[0];
		entry.q = keys[1];
	}

}
