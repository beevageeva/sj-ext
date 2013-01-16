package view.model.cache;

import constants.InstructionType;


public class DISepAddColumns extends DIAddColumns {

	protected int numDataEntries;
	
	public DISepAddColumns(int numDataEntries){
		this.numDataEntries = numDataEntries;
	}
	
	public boolean isEligible(int row,int key, int pid, short instrType) {
		return instrType!=InstructionType.FETCH_INSTR_TYPE?row<numDataEntries:row>=numDataEntries;
	}

	public boolean isEligible(EntryB entry,int key, int pid, short instrType) {
		return entry.b == (instrType!=InstructionType.FETCH_INSTR_TYPE);
	}

	

	
	
}
