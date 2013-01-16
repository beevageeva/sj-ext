package config.model;

import constants.InstructionType;

public class CacheConfigMem extends CacheConfigPT{

	
	
	protected int[] blockSizeNBits = new int[1];
	protected int busSize;
	public boolean isWriteAllocate;
	public boolean isWriteThrough;
	
	
	public int[] getBlockSizeNBits() {
		return blockSizeNBits;
	}
	public int getBlockSizeNBits(boolean isData) {
		if(blockSizeNBits.length==2 && !isData){
			return blockSizeNBits[1];
		}
		return blockSizeNBits[0];
	}
	
	public int getBusSize() {
		return busSize;
	}
	public void setBusSize(int busSize) {
		this.busSize = busSize;
	}
	@Override
	public void setDataInstrSeparated(boolean b) {
		super.setDataInstrSeparated(b);
		blockSizeNBits = new int[b?2:1];
	}
	
	
	
	
}
