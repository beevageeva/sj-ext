package config.model;

import constants.InstructionType;

public class CacheBlockCfg {
	
	protected int[] numberEntriesNBits = new int[1];
	protected int accessTimeUnits;
	public CacheBlockCfg(){}
	
	public int[] getNumberEntriesNBits() {
		return numberEntriesNBits;
	}
	
	public int getNumberEntriesNBits(boolean isData){
		if(numberEntriesNBits.length==2 && !isData){
			return numberEntriesNBits[1];
		}
		return numberEntriesNBits[0];
	}

	public boolean  isDataInstrSeparated(){
		return numberEntriesNBits.length==2; 
	}
	
	public void  setDataInstrSeparated(boolean b){
		numberEntriesNBits = new int[b?2:1];
	}

	public int getTotalNumberEntriesNBits(){
		int s = numberEntriesNBits[0];
		if(numberEntriesNBits.length ==2){
			s+=numberEntriesNBits[1];
		}
		return s;
	}


	public int getAccessTimeUnits() {
		return accessTimeUnits;
	}
	public void setAccessTimeUnits(int accessTimeUnits) {
		this.accessTimeUnits = accessTimeUnits;
	}


}
