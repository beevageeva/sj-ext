package view.model.cache;

public class EntryII extends EntryI{

	public int q;

	@Override
	public Object getValueAt(int col) {
		if(col==0){
			return p;
		}
		else if(col==1){
			return q;
		}
		return null;
	}
	
}
