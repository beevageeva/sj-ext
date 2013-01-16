package runtime.model;

import java.util.ArrayList;
import java.util.List;

import view.model.cache.CacheModel;


public class CacheChainModel {

	public List<CacheModel> cacheModels = new ArrayList<CacheModel>();
	
	//when passing from victimBuffer, he entry must be deleted here
	public CacheModel victimBufferModel = null;
	
	
}
