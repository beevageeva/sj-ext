package view.model.cache;


public class SetsAddColumns implements AddColumns<EntryII>{

	private int numberSets;
	
	public SetsAddColumns(int numberSets){
		this.numberSets = numberSets;
	}
	
	public EntryII createNewEntry() {
		return new EntryII();
	}

	public int getColCount() {
		return 2;
	}

	public String getColname(int col) {
		if(col==0){
			return "tag";
		}
		else if(col==1){
			return "set";
		}
		return null;
	}

	public int getEvictionValue(EntryII entry) {
		return 0;
	}

	public boolean isEligible(int row,  int key, int pid, short instrType) {
		return row % numberSets == key%numberSets;
	}

	public boolean isEligible(EntryII entry, int key, int pid, short instrType) {
		return true;
	}

	public void setValues(EntryII entry, int pid, short instrType, int... keys) {
		entry.p = keys[0]/numberSets;
		entry.q = keys[0]%numberSets;
	}

}
