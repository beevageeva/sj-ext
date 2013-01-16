package config.model;

import global.Helper;

import java.util.ArrayList;
import java.util.List;

import runtime.model.P2;

import view.components.Graph;

import config.model.PageTableConfig.DirectMappedPageTableConfig;
import config.model.PageTableConfig.InverseMappedPageTableConfig;
import constants.CacheCoherencePolicyType;

public class Configuration {

	public int virtualAddrNBits;
	
	public PageTableConfig pageTableConfig = new PageTableConfig();
	public int pageSizeNBits;

	public List<SMPNodeConfig> smpNodeConfigs;
	
	


	public short cacheMemCacheCoherencePolicy;

	protected Configuration() {
		smpNodeConfigs = new ArrayList<SMPNodeConfig>();
		virtualAddrNBits = 32;
		pageSizeNBits = 12;
		cacheMemCacheCoherencePolicy = CacheCoherencePolicyType.MSI;
		
	}

	private static Configuration instance = null;

	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}

	public static void resetConfiguration() {
		instance = null;
	}
	

	public int getTotalNumberOfProcs() {
		int n = 0;
		for (int i = 0; i < smpNodeConfigs.size(); i++) {
			n += smpNodeConfigs.get(i).cpuCachesToMem.size();
		}
		return n;
	}
	

	public int getMemTotalNumberOfPages(){
		int n = 0;
		for (int i = 0; i < smpNodeConfigs.size(); i++) {
			n += Math.pow(2,smpNodeConfigs.get(i).mainMemoryConfig.numberEntriesNBits);
		}
		return n;
	}
	
	//physical memory must look like an entire, even if each smpnode has its own mem
	//return the smp node index that have this memory 
	public int getRVPageNumber(int smpNodeIndex,int rpn){
		int r = 0;
		for(int i =0;i<smpNodeIndex;i++){
			r += Math.pow(2,smpNodeConfigs.get(i).mainMemoryConfig.numberEntriesNBits);
		}
		r+=rpn;
		return r;
	}

	public P2 getRLPageNumber(int rpn){
		int r = 0,i=0;
		while(i<smpNodeConfigs.size() && rpn>=0){
			r = rpn;
			rpn -= Math.pow(2,smpNodeConfigs.get(i).mainMemoryConfig.numberEntriesNBits);
			i++;
		}
		P2 p2 = new P2();
		p2.x = r;
		if(i==smpNodeConfigs.size() && rpn>0){
			p2.y = -1;
			System.out.println("rl not found "+r);
		}
		else{
			p2.y = i - 1;
		}
		return p2;
	}

	

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("virtualaddressNBits = ");
		sb.append(virtualAddrNBits);
		sb.append("\ncachesToMemCoherencePolicy = ");
		sb.append(cacheMemCacheCoherencePolicy);
		sb.append("\number of smp nodes = ");
		sb.append(smpNodeConfigs.size());
		sb.append("   PageTableCfg: ");
		sb.append(" type = ");
		if(pageTableConfig.getMappingType()==PageTableConfig.DIRECT_MAPPED_TYPE){
			sb.append("Direct");
		}
		else if(pageTableConfig.getMappingType()==PageTableConfig.INVERSE_MAPPED_TYPE){
			sb.append("Inverse");
		}
		else if(pageTableConfig.getMappingType() ==PageTableConfig.NO_MAPPING){
			sb.append("None(mapping obj cfg should be null)");
		}
		else{
			sb.append("Undefined(?)");
		}
		if(pageTableConfig.getMappingType() == PageTableConfig.DIRECT_MAPPED_TYPE){
			DirectMappedPageTableConfig dpageTableConfig = (DirectMappedPageTableConfig) pageTableConfig.getMappingConfig();
			sb.append(" ,offsets :");
			for(int j = 0;j<dpageTableConfig.getOffsetsLength().length;j++){
				sb.append(dpageTableConfig.getOffsetsLength()[j]);
				sb.append(" ");
			}
			sb.append(", searchMethod = ");
			if(dpageTableConfig.isSearchMethodTopDown()){
				sb.append("topDown");
			}
			else{
				sb.append("bottomUp");
			}
		}
		else if(pageTableConfig.getMappingType()==PageTableConfig.INVERSE_MAPPED_TYPE){
			InverseMappedPageTableConfig ipageTableConfig = (InverseMappedPageTableConfig) pageTableConfig.getMappingConfig();
			sb.append(" ,hashAnchorSizeNBits = ");
			sb.append(ipageTableConfig.getHashAnchorSizeNBits());
		}
		else{
			sb.append(", mappingObectCfg = ");
			sb.append(pageTableConfig.getMappingConfig());
		}
		SMPNodeConfig smpNodeCfg;
		MainMemCacheConfig mmcfg;
		CacheConfigMem ccmem;
		CacheConfigPT ccpt;
		for(int i = 0 ; i<smpNodeConfigs.size();i++){
			smpNodeCfg = smpNodeConfigs.get(i);
			sb.append("\nSMP_NODE ");
			sb.append(i);
			sb.append(" : \n");
			sb.append("   MainMemCfg: ");
			mmcfg = smpNodeCfg.mainMemoryConfig;
			sb.append(" accessTimeUnits = ");
			sb.append(mmcfg.accessTimeUnits);
			sb.append(" ,numberEntriesNBits = ");
			sb.append(mmcfg.numberEntriesNBits);
			sb.append(" numberSetsNBits(1 or numberEntriesNBits if inverse page table) = ");
			sb.append(mmcfg.numberSetsNBits);
			sb.append(" , pageSizeNBits = ");
			sb.append(pageSizeNBits);
			sb.append(" , busSize = ");
			sb.append(mmcfg.busSize);
			sb.append("\n  Available cache Mem : ");
			for(int j = 0 ; j<smpNodeCfg.cacheConfigsMem.size();j++){
				ccmem = smpNodeCfg.cacheConfigsMem.get(i);
				sb.append("\n      ");
				sb.append(j);
				sb.append(" ( ");
				sb.append(ccmem.name);
				sb.append(") : accessTimeUnits = ");
				sb.append(ccmem.accessTimeUnits);
				sb.append(", isDataInstrSep = ");
				sb.append(ccmem.isDataInstrSeparated());
				sb.append(", numberEntriesNBits = ");
				sb.append(ccmem.numberEntriesNBits[0]);
				if(ccmem.isDataInstrSeparated()){
					sb.append(", numberEntriesNBitsInstr = ");
					sb.append(ccmem.numberEntriesNBits[1]);
				}
				sb.append(", numberSetsNBitsInstr = ");
				sb.append(ccmem.numberSetsNBits);
				sb.append(", blockSizeNBits = ");
				sb.append(ccmem.blockSizeNBits[0]);
				if(ccmem.isDataInstrSeparated()){
					sb.append(", blockSizeNBitsInstr = ");
					sb.append(ccmem.blockSizeNBits[1]);
				}
				sb.append(" , victimCacheNumberEntriesNBits = ");
				sb.append(ccmem.victimCacheCfg==null?"None":ccmem.victimCacheCfg.getTotalNumberEntriesNBits() );
				sb.append(" , busSize = ");
				sb.append(ccmem.busSize);
				
			}
		}
		return sb.toString();
	}
	

	
	
	
}

  
