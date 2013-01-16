package view.model.cache;

import java.util.List;

public interface CacheListener {

	
	public void entriesToBeEvicted(int index,CacheModel cm);
	public void entriesAccessed(int index,CacheModel cm);
	public void entriesPut(int index,CacheModel cm);
	
}
