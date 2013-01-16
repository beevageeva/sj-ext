package view.model.interfaces;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import runtime.view.DefaultTablePanel;

public abstract class MovingIndexAbstractTableModel extends AbstractTableModel{

	public int lastIndex = -1;
	
	public void fireIndexMoved(int index) {
		fireTableRowsUpdated(index, index);
		lastIndex = index;
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TableModelListener.class) {
				if (listeners[i + 1] instanceof JTable) {
					DefaultTablePanel.moveTo((JTable) listeners[i + 1], index);
				}
			}
		}
	}

	
}
