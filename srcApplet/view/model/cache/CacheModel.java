package view.model.cache;

import global.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import config.model.Configuration;

import view.model.interfaces.MovingIndexAbstractTableModel;

public class CacheModel extends MovingIndexAbstractTableModel implements
		CacheModelI {

	protected List<AddColumns> addColumns = new ArrayList<AddColumns>();

	protected Map<Integer, List<Entry>> entries = new HashMap<Integer, List<Entry>>();

	private List<CacheListener> l = new ArrayList<CacheListener>();

	protected int maxNumberEntries;

	private int lastIndex = -1;
	
	private int numEvictedEntries = 0;
	private int numPutEntries = 0;
	private int numAccessedEntries = 0;
	

	public int getNumEvictedEntries() {
		return numEvictedEntries;
	}

	public int getNumPutEntries() {
		return numPutEntries;
	}

	public int getNumAccessedEntries() {
		return numAccessedEntries;
	}

	public CacheModel(int maxNumberEntries) {
		this.maxNumberEntries = maxNumberEntries;

	}

	public void addAddColumns(AddColumns addCol) {
		addColumns.add(addCol);
	}
	
	private int getPidColumnIndex(){
		int indexAddColumn = -1;
		for (int i = 0; i < addColumns.size() && indexAddColumn == -1; i++) {
			if (addColumns.get(i) instanceof PidAddColumns) {
				indexAddColumn = i;
			}
		}
		return indexAddColumn;
	}

	private int getDataColumnIndex(){
		int indexAddColumn = -1;
		for (int i = 0; i < addColumns.size() && indexAddColumn == -1; i++) {
			if (addColumns.get(i) instanceof DIAddColumns) {
				indexAddColumn = i;
			}
		}
		return indexAddColumn;
	}

	
	public int getColumnCount() {
		int n = 1;
		for (int i = 0; i < addColumns.size(); i++) {
			n += addColumns.get(i).getColCount();
		}
		return n;
	}

	public int getRowCount() {
		return maxNumberEntries;
	}

	@Override
	public String getColumnName(int column) {
		if (column == 0) {
			return "i";
		}
		int n = 1;
		for (int i = 0; i < addColumns.size(); i++) {
			if (column - n < addColumns.get(i).getColCount()) {
				return addColumns.get(i).getColname(column - n);
			}
			n += addColumns.get(i).getColCount();
		}
		return null;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		List<Entry> le = entries.get(rowIndex);
		if (le == null) {
			return null;
		}
		if (columnIndex == 0) {
			return rowIndex;
		}
		int n = 1;
		for (int i = 0; i < addColumns.size(); i++) {
			if (columnIndex - n < addColumns.get(i).getColCount()) {
				return le.get(i).getValueAt(columnIndex - n);
			}
			n += addColumns.get(i).getColCount();
		}
		return null;

	}

	public List<Entry> getCacheEntry(int cacheKey) {
		return findEntriesByKey(cacheKey, new CacheLineFound() {

			@Override
			public void cacheLineFound(List<Entry> le, int index) {
				fireEntriesAccessed(index);
			}
		});
	}

	public int putCacheEntry(int pid, short instrType, int... keys) {
		boolean eligible;
		for (int i = 0; i < maxNumberEntries; i++) {
			if (entries.get(i) == null) {
				eligible = true;
				for (int k = 0; k < addColumns.size() && eligible; k++) {
					eligible = eligible
							&& addColumns.get(k).isEligible(i, keys[0], pid,
									instrType);
				}
				if (eligible) {
					entries.put(i, new ArrayList<Entry>());
					for (int k = 0; k < addColumns.size(); k++) {
						entries.get(i).add(addColumns.get(k).createNewEntry());
						addColumns.get(k).setValues(entries.get(i).get(k), pid,
								instrType, keys);
					}
					fireEntriesPut(i);
					fireIndexMoved(i);

					return i;
				}
			}
		}
		// evict a node
		int ind = findIndexToEvict(keys[0], pid, instrType);
		if (entries.get(ind) != null) {
			entries.get(ind).clear();
		} else {
			// TODO this should not occur
			System.out.println("entries null when evict");
		}
		for (int k = 0; k < addColumns.size(); k++) {
			entries.get(ind).add(addColumns.get(k).createNewEntry());
			addColumns.get(k).setValues(entries.get(ind).get(k), pid,
					instrType, keys);
		}
		fireEntriesToEvict(ind);
		fireEntriesPut(ind);
		fireIndexMoved(ind);
		return ind;
	}

	//for mm returns -2 , that is that the index of the column must be used
	public int getKeyAddColumnIndex(){
		int indexAddColumn = -1;
		for (int i = 0; i < addColumns.size() && indexAddColumn == -1; i++) {
			if (addColumns.get(i) instanceof PTCacheAddColumn
					|| addColumns.get(i) instanceof MemCacheAddColumns ) {
				indexAddColumn = i;
			}
			if(addColumns.get(i) instanceof MainMemAddColumn){
				indexAddColumn=-2;
			}
		}
		return indexAddColumn;
	}
	
	public List<Entry> findEntriesByKey(int key, CacheLineFound l) {
		int indexAddColumn = getKeyAddColumnIndex();
		if(indexAddColumn==-1){
			return null;
		}
		Iterator<Integer> it = entries.keySet().iterator();
		List<Entry> le;
		Integer ind;
		while (it.hasNext()) {
			ind = it.next();
			le = entries.get(ind);
			if (le != null) {
				assert le.size() == addColumns.size();
				if (indexAddColumn==-2 &&ind==key || indexAddColumn!=-2 && ((EntryI) le.get(indexAddColumn)).p == key) {
					fireIndexMoved(ind);
					l.cacheLineFound(le, ind);
					return le;
				}

			}
		}
		return null;

	}

	public void findEntriesByPid(int pid, CacheLineFound l) {
		int indexAddColumn = getPidColumnIndex();
		Iterator<Integer> it = entries.keySet().iterator();
		List<Entry> le;
		int index;
		while (it.hasNext()) {
			index = it.next();
			le = entries.get(index);
			for (int i = 0; i < le.size(); i++) {
				if (((EntryI) le.get(indexAddColumn)).p == pid) {
					l.cacheLineFound(le, index);
				}
			}
		}

	}

	private int findIndexToEvict(int cacheKey, int pid, short instrType) {

		// find the nodes with the max value , then choose a random index
		int maxValue = -1;
		boolean elig = true;
		int value;

		Iterator<Integer> it = entries.keySet().iterator();
		Entry e;
		int i;
		while (it.hasNext()) {
			value = 0;
			i = it.next();
			elig = true;
			for (int k = 0; k < addColumns.size(); k++) {
				e = entries.get(i).get(k);
				value += addColumns.get(k).getEvictionValue(e);
				elig = elig
						&& addColumns.get(k).isEligible(e, cacheKey, pid,
								instrType);
			}
			if (elig && value > maxValue) {
				maxValue = value;
			}
		}
		List<Integer> selectableIndexes = new ArrayList<Integer>();
		it = entries.keySet().iterator();

		while (it.hasNext()) {
			value = 0;
			elig = true;
			i = it.next();
			for (int k = 0; k < addColumns.size(); k++) {
				e = entries.get(i).get(k);
				value += addColumns.get(k).getEvictionValue(e);
				elig = elig
						&& addColumns.get(k).isEligible(e, cacheKey, pid,
								instrType);
			}
			if (elig && value == maxValue) {
				selectableIndexes.add(i);
			}
		}
		if (selectableIndexes.size() > 0) {
			int k = (int) (Math.random() * selectableIndexes.size());
			return selectableIndexes.get(k);
		} else {
			System.out.println("no index to evict ");
			return -1;
		}
	}

	public void removeEntry(int index) {
		entries.remove(index);
		fireEntriesToEvict(index);
	}
	
	public void removeEntryByKey(int cacheKey){
		findEntriesByKey(cacheKey, new CacheLineFound() {

			@Override
			public void cacheLineFound(List<Entry> le, int index) {
				removeEntry(index);
			}
		});

	}

	public void removeAllEntries(int pid) {
		final List<Integer> torem = new ArrayList<Integer>();	
		findEntriesByPid(pid, new CacheLineFound() {
			@Override
			public void cacheLineFound(List<Entry> le, int index) {
				torem.add(index);
				
			}
		});
		for(int i = 0;i<torem.size();i++){
			removeEntry(i);
		}
	}

	public AddColumns getAddColumn(int i) {
		return addColumns.get(i);
	}

	public int getLastIndex() {
		return lastIndex;
	}

	public void addCacheListener(CacheListener list) {
		l.add(list);
	}
	
	public void removeCacheListener(CacheListener list) {
		l.remove(list);
	}
	

	private void fireEntriesToEvict(int index) {
		numEvictedEntries++;
		for (int i = 0; i < l.size(); i++) {
			l.get(i).entriesToBeEvicted(index, this);
		}
	}

	private void fireEntriesPut(int index) {
		numPutEntries++;
		for (int i = 0; i < l.size(); i++) {
			l.get(i).entriesPut(index, this);
		}
	}

	private void fireEntriesAccessed(int index) {
		numAccessedEntries++;
		for (int i = 0; i < l.size(); i++) {
			l.get(i).entriesAccessed(index, this);
		}
	}

	public List<Entry> getEntriesAtIndex(int index) {
		return entries.get(index);
	}

	interface CacheLineFound {
		public void cacheLineFound(List<Entry> le, int index);
	}
	
	public int getKeyAtIndex(int index){
		List<Entry> le = getEntriesAtIndex(index);
		if(le!=null){
			int keyIndex = getKeyAddColumnIndex();
			return ((EntryI)le.get(keyIndex)).p; 
		}
		return -1;
	}

	public int getPidAtIndex(int index){
		if(existsEntry(index)){
			int pidIndex = getPidColumnIndex();
			return ((EntryI)getEntriesAtIndex(index).get(pidIndex)).p;
		}
		Logger.log("ENTRY at Index "+index + " in cacheModel addr "+this + " empty getpid returns -1");	
		return -1;
	}
	
	public boolean isDataAtIndex(int index){
		if(existsEntry(index)){
			int isDataIndex = getDataColumnIndex();
			return ((EntryB)getEntriesAtIndex(index).get(isDataIndex)).b;
		}
		Logger.log("ENTRY at Index "+index + " in cacheModel addr "+this + " empty isData returns false");	
		return false;
	}
	
	public boolean existsEntry(int index){
		return getEntriesAtIndex(index)==null?false:true;
	}

	
}
