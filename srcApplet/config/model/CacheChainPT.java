package config.model;

import java.util.ArrayList;
import java.util.List;


public class CacheChainPT<CC extends CacheConfigPT> {

	public List<CC> caches;

	//if null means that has no exclusive cache
	public IndexCacheBlockCfg exclusiveCacheCfg = null;


	public CacheChainPT() {
		caches = new ArrayList<CC>();
	}
	
	public boolean hasExclusiveCache(){
		return exclusiveCacheCfg!=null;
	}


}



 

