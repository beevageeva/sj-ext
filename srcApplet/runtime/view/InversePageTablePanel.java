package runtime.view;

import javax.swing.JPanel;

import config.model.Configuration;

import runtime.model.InverseMappedPageTableModel;
import runtime.model.RuntimeModel;

public class InversePageTablePanel extends JPanel{

	
	
	public InversePageTablePanel(){
		InverseMappedPageTableModel model = ((InverseMappedPageTableModel)RuntimeModel.getInstance().pageTableModel);
		add(new DefaultTablePanel("invese page table", model.getIPE()));
	}
	
}
