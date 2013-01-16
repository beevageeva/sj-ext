package view.model.cache;

public class EntryI implements Entry{

	public int p;
	
	public EntryI(){
		p = 0;
	}

	public Object getValueAt(int col) {
		if(col==0){
			return p;
			
		}
		return null;
	}
	
	
	
}
