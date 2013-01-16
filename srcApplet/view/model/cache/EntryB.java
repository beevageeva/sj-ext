package view.model.cache;

public class EntryB implements Entry{

	public boolean b;
	
	public Object getValueAt(int col) {
		if(col==0){
			return b?1:0;
		}
		return null;
	}

}
