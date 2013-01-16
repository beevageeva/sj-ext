package runtime.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import view.model.cache.EntryI;
import view.model.interfaces.MovingIndexAbstractTableModel;

public class DE extends MovingIndexAbstractTableModel{

	
	protected int numberEntries;
	
	
	public DE(int numberEntries){
		this.numberEntries = numberEntries;
	}
	
	public EntryI getEntry(int row){
		return entries.get(row);
	}

	public void newEntry(int row){
		if(row>numberEntries){
			System.out.println("row > number Entries");
		}
		entries.put(row, new EntryI());
	}
	
	private Map<Integer, EntryI> entries = new HashMap<Integer, EntryI>();
	
	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return numberEntries;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		EntryI e = entries.get(rowIndex); 
		if(e==null){
			return null;
		}
		if(columnIndex==0){
			return rowIndex;
		}
		return e.getValueAt(columnIndex - 1); 
	}
	
	public List<Integer> getAllEntriesValues(){
		List<Integer> res = new ArrayList<Integer>();
		Iterator<Integer> keysit = entries.keySet().iterator();
		int key;
		while(keysit.hasNext()){
			key = keysit.next();
			res.add(entries.get(key).p);
		}
		return res;
	}
	
}
