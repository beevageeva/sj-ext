package runtime.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import view.model.cache.EntryIList;
import view.model.interfaces.MovingIndexAbstractTableModel;

public class IPE extends MovingIndexAbstractTableModel{

	protected int numberEntries;
	
	public IPE(int numberEntries) {
		this.numberEntries = numberEntries;
	}

	private Map<Integer, EntryIList> entries = new HashMap<Integer, EntryIList>();
	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public int getRowCount() {
		return numberEntries;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		EntryIList e = entries.get(rowIndex); 
		if(e==null){
			return null;
		}
		if(columnIndex==0){
			return rowIndex;
		}
		return e.getValueAt(columnIndex - 1); 
	}

	@Override
	public String getColumnName(int column) {
		switch(column){
		case 0:
			return "i";
		case 1:
			return "PPN";
		case 2:
			return"VPNs";
		}
		return null;
	
	}
	
	public EntryIList getEntry(int index){
		return entries.get(index);
	}
	
	public void newEntry(int index){
		entries.put(index, new EntryIList());
	}
	
	public void removeProcess(int pid){
		Iterator<Integer> keys = entries.keySet().iterator();
		int key;
		while(keys.hasNext()){
			key = keys.next();
			entries.get(key).removeEntriesByPid(pid);
		}
	}
	

	
	
}
