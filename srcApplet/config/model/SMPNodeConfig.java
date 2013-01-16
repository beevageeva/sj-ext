package config.model;

import java.rmi.server.RemoteCall;
import java.util.ArrayList;
import java.util.List;

import runtime.model.P2;

public class SMPNodeConfig {

	public List<CacheConfigMem> cacheConfigsMem;

	public List<CacheConfigPT> cacheConfigsPT;

	public List<CacheChainMem> cpuCachesToMem;

	public List<CacheChainPT<CacheConfigPT>> cpuCachesToPT;

	public MainMemCacheConfig mainMemoryConfig = new MainMemCacheConfig();

	public int diskAccessTime;
	
	public String name="";
	
	public CacheBlockCfg remoteDataCache = null;

	public SMPNodeConfig() {
		cacheConfigsMem = new ArrayList<CacheConfigMem>();
		cacheConfigsPT = new ArrayList<CacheConfigPT>();
		cpuCachesToMem = new ArrayList<CacheChainMem>();
		cpuCachesToPT = new ArrayList<CacheChainPT<CacheConfigPT>>();
		mainMemoryConfig = new MainMemCacheConfig();
		diskAccessTime = 10;
	}
	
	public int getIndexInPTCacheCfgs(int numCPU, int index){
		return cacheConfigsPT.indexOf(cpuCachesToPT.get(numCPU).caches.get(index));
	}

	
	public int getIndexInMemCacheCfgs(int numCPU, int index){
		return cacheConfigsMem.indexOf(cpuCachesToMem.get(numCPU).caches.get(index));
	}
	
	public List<P2> getCPUsFromIndexInMemArray(int cacheIndex){
		List<P2> res = new ArrayList<P2>();
		CacheConfigMem cfgmem = cacheConfigsMem.get(cacheIndex);
		int index;
		for(int i = 0;i<cpuCachesToMem.size();i++){
			index = cpuCachesToMem.get(i).caches.indexOf(cfgmem); 
			if(index!=-1){
				res.add(new P2(i, index));
			}
		}
		return res;
	}
	
	public P2 getRandomCPUFromIndexInMemArray(int cacheIndex){
		List<P2> matchingCPUS = getCPUsFromIndexInMemArray(cacheIndex);
		if(matchingCPUS.size()>0){
			return matchingCPUS.get((int) (Math.random() * matchingCPUS.size()));
		}
		return new P2(-1,-1);
	}
	
	public boolean hasRemoteCache(){
		return remoteDataCache!=null;	
	}

}


