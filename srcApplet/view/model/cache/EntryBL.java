package view.model.cache;

public class EntryBL extends EntryL{

	protected boolean q;
	
	@Override
	public Object getValueAt(int col) {
		if(col==0){
			return super.getValueAt(col);
		}
		else if (col==1){
			return q;
		}
		return null;
	}

	
	
	
	
}
