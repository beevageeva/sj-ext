package view.model.cache;

import constants.InstructionType;

public class DIAddColumns implements AddColumns<EntryB>{

	@Override
	public EntryB createNewEntry() {
		return new EntryB();
	}

	@Override
	public int getColCount() {
		return 1;
	}

	@Override
	public String getColname(int col) {
		if(col==0){
			return "isData";
		}
		return null;
	}

	@Override
	public int getEvictionValue(EntryB entry) {
		return 0;
	}

	@Override
	public boolean isEligible(int row, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public boolean isEligible(EntryB entry, int key, int pid, short instrType) {
		return true;
	}

	@Override
	public void setValues(EntryB entry, int pid, short instrType, int... keys) {
		entry.b = instrType !=InstructionType.FETCH_INSTR_TYPE;
		
	}

}
