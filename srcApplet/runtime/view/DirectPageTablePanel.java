package runtime.view;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import runtime.model.DirectMappedPageTableModel;
import runtime.model.RuntimeModel;
import view.model.DirectPageTableListener;
import view.model.interfaces.MovingIndexAbstractTableModel;
import config.model.Configuration;
import config.model.PageTableConfig.DirectMappedPageTableConfig;

public class DirectPageTablePanel extends JPanel implements DirectPageTableListener{


	private JPanel[] levelPanels;

	

	public DirectPageTablePanel() {
		// init tables
		DirectMappedPageTableModel model = (DirectMappedPageTableModel) RuntimeModel.getInstance().pageTableModel;
		int nOffsets = ((DirectMappedPageTableConfig)Configuration.getInstance().pageTableConfig.getMappingConfig()).getOffsetsLength().length;
		levelPanels = new JPanel[nOffsets ];
		JPanel panel = new JPanel(new GridLayout(nOffsets + 1, 1));
		JScrollPane sp = new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setPreferredSize(new Dimension(800, 800));
		add(sp);
		panel.add(new DefaultTablePanel("Root", model.getPidToTableTable()));
		for (int i = 0; i < nOffsets; i++) {
			levelPanels[i] = new JPanel();
			levelPanels[i].setSize(new Dimension(500,
					550 / nOffsets));
			panel.add(levelPanels[i]);
		}
		//add tables from model
		for(int i = 0;i<nOffsets-1;i++){
			for(int j = 0 ; j<model.getDirectoryTables()[i].size();j++){
				tableAdded(model.getDirectoryTables()[i].get(j) , i);
			}
		}
		for(int j = 0 ; j<model.getPeTables().size();j++){
			tableAdded(model.getPeTables().get(j) ,nOffsets - 1 );
		}
		model.addDirectPageTableListener(this);
	}
	
	public void tableAdded(MovingIndexAbstractTableModel model, int level) {
		StringBuffer label = new StringBuffer("Level : ");
		label.append(level);
		label.append(" ");
		if (level == ((DirectMappedPageTableConfig)Configuration.getInstance().pageTableConfig.getMappingConfig()).getOffsetsLength().length - 1) {
			label.append("PT");
		} else {
			label.append("PD");
		}
		label.append(levelPanels[level].getComponentCount());
		levelPanels[level].add(new DefaultTablePanel(label.toString(), model));
		setVisible(true);
		
	}

	public void tablesCleared() {
		for(int i = 1;i<levelPanels.length;i++){
			levelPanels[i].removeAll();
		}
	}


}
