package view.model.cache;

public class EntryIB extends EntryI {

	
	public boolean b;

	@Override
	public Object getValueAt(int col) {
		switch(col){
		case 0:
			return p;
		case 1:
			return b;
		}
		return null;
	}
	
}
