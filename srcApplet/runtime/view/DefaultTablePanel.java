package runtime.view;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import view.model.cache.CacheModel;
import view.model.interfaces.MovingIndexAbstractTableModel;

public class DefaultTablePanel extends JPanel {

	private JTable table;

	 public DefaultTablePanel(JComponent label, MovingIndexAbstractTableModel m){
			super(new BorderLayout());
			add(label, BorderLayout.NORTH);
			table = new JTable(m);
			JScrollPane scrollPane = new JScrollPane(table);
			add(scrollPane, BorderLayout.CENTER);
			if (m.lastIndex != -1) {
				moveTo(m.lastIndex);
			}
		 
	 }

	public DefaultTablePanel(String label, MovingIndexAbstractTableModel m) {
		this(new JLabel(label), m);
	}

	public void moveTo(int index) {
		moveTo(table, index);
	}

	public static void moveTo(JTable table, int index) {
		JViewport viewPort = (JViewport) table.getParent();
		if (viewPort != null) {
			JScrollBar vsb = ((JScrollPane) viewPort.getParent())
					.getVerticalScrollBar();
			vsb.setValue((8 * index * vsb.getBlockIncrement()) / 5);
			// TODO 8, 5
			// vsb.setValue(index * vsb.getBlockIncrement());

		}
		table.setSelectionBackground(new Color(184, 207, 229));
		table.setRowSelectionInterval(index, index);

	}

}
