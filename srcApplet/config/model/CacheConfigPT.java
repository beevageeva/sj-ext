package config.model;

import constants.CacheEvictionPolicyType;
import constants.InstructionType;

/**
 * @author root
 *
 */
public  class CacheConfigPT extends CacheBlockCfg {
	
	
	protected short evictionPolicy = CacheEvictionPolicyType.RANDOM_POLICY;
	protected int numberSetsNBits = 0;
	public CacheBlockCfg victimCacheCfg = null;
	protected String name="";



	public short getEvictionPolicy() {
		return evictionPolicy;
	}
	public void setEvictionPolicy(short evictionPolicy) {
		this.evictionPolicy = evictionPolicy;
	}
	
	public int getNumberSetsNBits() {
		return numberSetsNBits;
	}
	public void setNumberSetsNBits(int numberSetsNBits) {
		this.numberSetsNBits = numberSetsNBits;
	}
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public boolean hasVictimCache(){
		return victimCacheCfg!=null;
	}
	

	@Override
	public String toString() {
		return name;
	}
	@Override
	public void setDataInstrSeparated(boolean b) {
		super.setDataInstrSeparated(b);
		if(victimCacheCfg!=null){
			victimCacheCfg.setDataInstrSeparated(b);
		}
	}

	

}
