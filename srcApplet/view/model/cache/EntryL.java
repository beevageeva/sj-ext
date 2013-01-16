package view.model.cache;

public class EntryL implements Entry{

	public EntryL(){
		p = 0;
	}
	
	public long p;
	public Object getValueAt(int col) {
		if(col==0){
			return p;
			
		}
		return null;
	}
		
		
		
}
