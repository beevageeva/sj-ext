package config.model;

import constants.CacheEvictionPolicyType;

public class MainMemCacheConfig {

	public int numberEntriesNBits = 7;
	public int numberSetsNBits = 0;
	protected short evictionPolicy = CacheEvictionPolicyType.RANDOM_POLICY;
	protected int accessTimeUnits = 5;
	protected int busSize = 1;
	public MainMemoryAllocConfig mainMemoryAllocConfig = null;
	public PageAgingConfig pageAgingConfig = null;
	
	
	public int getAccessTimeUnits() {
		return accessTimeUnits;
	}
	public void setAccessTimeUnits(int accessTimeUnits) {
		this.accessTimeUnits = accessTimeUnits;
	}
	public short getEvictionPolicy() {
		return evictionPolicy;
	}
	public void setEvictionPolicy(short evictionPolicy) {
		this.evictionPolicy = evictionPolicy;
	}
	public int getNumberEntriesNBits() {
		return numberEntriesNBits;
	}
	public void setNumberEntriesNBits(int numberEntriesNBits) {
		this.numberEntriesNBits = numberEntriesNBits;
	}
	
	public int getBusSize() {
		return busSize;
	}
	public void setBusSize(int busSize) {
		this.busSize = busSize;
	}
	
	public boolean hasMainMemoryAllocConfig(){
		return mainMemoryAllocConfig!=null;
	}
	
	public boolean hasPageAgingConfig(){
		return pageAgingConfig!=null;
	}

	
}



