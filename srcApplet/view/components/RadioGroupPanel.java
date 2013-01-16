package view.components;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;



public class RadioGroupPanel extends JPanel{

	private static final long serialVersionUID = 1L;
	private ButtonGroup bg;
	private short[] values;
	
	
	
	/**
	 * @param label
	 * @param labels
	 * @param values
	 * @param selectedValue if selectedValue != -1 select the button that have this value 
	 * @param isHorizontal
	 */
	public RadioGroupPanel(String label,String[] labels , short[] values,int selectedValue, boolean isHorizontal){
		this.values = values;
		setLayout(new BorderLayout());
		int nrows , ncols;
		if(isHorizontal){
			nrows = 1;
			ncols = labels.length;
		}
		else{
			nrows = labels.length;
			ncols = 1;
		}
		if(label!=null){
			add(new JLabel(label) , BorderLayout.NORTH);
		}
		JPanel radioButtonsPanel = new JPanel(new GridLayout(nrows , ncols));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		bg = new ButtonGroup();
		JRadioButton rb ;
		for(int i = 0 ; i<labels.length ; i++){
			rb = getButton(labels[i], values[i]);
			radioButtonsPanel.add(rb);
			if(selectedValue!=-1 && values[i] == selectedValue){
				rb.setSelected(true);
			}
		}
		add(radioButtonsPanel, BorderLayout.CENTER);
	}

	private JRadioButton getButton(String text , short value ){
		JRadioButton jb = new JRadioButton(text);
		jb.setActionCommand(String.valueOf(value));
		bg.add(jb);
		return jb;
	}
	
	public short getValue(){
		return Short.parseShort(bg.getSelection().getActionCommand());
	}
	
	public void addActionListener(short buttonValue , ActionListener al ){
		Enumeration buttons = bg.getElements();
		JRadioButton rb ;
		while(buttons.hasMoreElements()){
			rb = (JRadioButton)buttons.nextElement();
			if(rb.getActionCommand().equals(String.valueOf(buttonValue))){
				rb.addActionListener(al);
				return;
			}
		}
		
	}
	
	
	public void setSelectedValue(short value){
		Enumeration buttons = bg.getElements();
		JRadioButton rb ;
		int i = -1;
		while(buttons.hasMoreElements()){
			i++;
			rb = (JRadioButton) buttons.nextElement();
			if(values[i] == value){
				rb.setSelected(true);
			}
		}
	}
	
	public void setEnabled(boolean enable){
		Enumeration buttons = bg.getElements();
		while(buttons.hasMoreElements()){
			((JRadioButton)buttons.nextElement()).setEnabled(enable);
		}
	}
	

}
