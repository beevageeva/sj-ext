package view.model.cache;

public class LFUAddColumns implements AddColumns<EntryI> {

	@Override
	public EntryI createNewEntry() {
		return new EntryI();
	}

	@Override
	public int getColCount() {
		return 2;
	}

	@Override
	public String getColname(int col) {
		if(col==0){
			return "numberTimesUsed";
		}
		return null;
	}

	@Override
	public int getEvictionValue(EntryI entry) {
		return -entry.p;
	}

	@Override
	public boolean isEligible(int row, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public boolean isEligible(EntryI entry, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public void setValues(EntryI entry, int pid, short instrType, int... keys) {
		entry.p++;
	}

}
