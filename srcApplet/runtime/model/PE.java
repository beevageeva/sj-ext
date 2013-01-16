package runtime.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import view.model.cache.EntryIB;
import view.model.interfaces.MovingIndexAbstractTableModel;

public class PE extends MovingIndexAbstractTableModel{

	protected int numberEntries;
	
	
	public PE(int numberEntries){
		this.numberEntries = numberEntries;
	}
	
	private Map<Integer, EntryIB> entries = new HashMap<Integer, EntryIB>();
	
	public int getColumnCount() {
		return 3;
	}

	public int getRowCount() {
		return numberEntries;
	}
	
	

	@Override
	public String getColumnName(int column) {
		switch(column){
		case 0: return "i";
		case 1: return "p";
		case 2: return "inMem";
		}
		return null;
			
	}

	public EntryIB getEntry(int row){
		return entries.get(row);
	}

	public void newEntry(int row){
		entries.put(row, new EntryIB());
	}

	
	public Object getValueAt(int rowIndex, int columnIndex) {
		EntryIB e = entries.get(rowIndex); 
		if(e==null){
			return null;
		}
		if(columnIndex == 0){
			return rowIndex;
		}
		return e.getValueAt(columnIndex - 1); 
	}
	
	public boolean  swapMemEntry(int rpn){
		Iterator<Integer> it = entries.keySet().iterator();
		int i;
		while(it.hasNext()){
			i = it.next();
			if(entries.get(i).p==rpn){
				entries.get(i).b = false;
				fireIndexMoved(i);
				return true;
			}
		}
		return false;
	}
	
	
}
