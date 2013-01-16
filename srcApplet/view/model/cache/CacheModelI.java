package view.model.cache;

import java.util.List;



public interface CacheModelI {

	public List<Entry> getCacheEntry(int cacheKey);
	public int putCacheEntry(int pid, short instrType, int ... keys);
	public void removeEntry(int index);
	public void removeAllEntries(int pid);

	
	
}
