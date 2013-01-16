package view.model;

import view.model.interfaces.MovingIndexAbstractTableModel;

public interface DirectPageTableListener {

	public void tableAdded(MovingIndexAbstractTableModel model, int level);
	public void tablesCleared();
	
}
