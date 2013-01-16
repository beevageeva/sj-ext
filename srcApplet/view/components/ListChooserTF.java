package view.components;

import global.Errors;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;


public class ListChooserTF extends ListChooser {

	private static final long serialVersionUID = 1L;

	protected DefTextField[] inputText;

	protected int[] textValue;
	protected int[] inputTextMinValue;
	protected int[] inputTextMaxValue;
	protected ListTFValidator listTFValidator;

	
	public ListChooserTF(Frame frame, Component locationComp, String labelText,
			String title, Object[] data, int initialIndex, String longValue,
			String okLabel, String[] inputTextLabel, int[] inputTextMinValue, int[] inputTextMaxValue, int[] inputTextDefaultValues ) {
		super(frame, locationComp, labelText, title, data, initialIndex,
				longValue, okLabel);
		if(inputTextLabel==null || inputTextMaxValue==null || inputTextMinValue==null || inputTextDefaultValues==null || inputTextLabel.length!=inputTextMaxValue.length || inputTextLabel.length!=inputTextMinValue.length||inputTextLabel.length!=inputTextDefaultValues.length){
			throw new IllegalArgumentException("arrays must have same length");
		}
		Box northPanel = new Box(BoxLayout.Y_AXIS);
		getContentPane().add(northPanel, BorderLayout.NORTH);
		inputText = new DefTextField[inputTextLabel.length];
		textValue = new int[inputTextLabel.length];
		for(int i = 0;i<inputTextLabel.length;i++){
			inputText[i] = new DefTextField(5);
			inputText[i].setText(String.valueOf(inputTextDefaultValues[i]));
			northPanel.add(
					Graph.createPanel(inputText[i], inputTextLabel[i], null));

		}
		this.inputTextMinValue = inputTextMinValue;
		this.inputTextMaxValue = inputTextMaxValue;
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (!"cancel".equalsIgnoreCase(e.getActionCommand())) {
			processTextField();
			if(selectedIndex!=-1 && listTFValidator!=null){
				Errors err = new Errors();
				listTFValidator.validateListTFValues(err, selectedIndex, textValue);
				if(!err.isEmpty()){
					JOptionPane.showMessageDialog(this, err.displayErrors());
					selectedIndex = -1;
				}
			}
		}
	}
	


	private void processTextField() {
		Errors err = new Errors();
		for(int i = 0;i<inputText.length;i++){
			inputText[i].validateField("input field", err, inputTextMinValue[i], inputTextMaxValue[i]);
			if (!err.isEmpty()) {
				JOptionPane.showMessageDialog(this, err.displayErrors());
				selectedIndex = -1;
				return;
			}
			textValue[i] = Integer.parseInt(inputText[i].getText());
		}
	}

	public int[] getTextValues() {
		return textValue;
	}
	public void setListTFValidator(ListTFValidator listTFValidator){
		this.listTFValidator = listTFValidator;
	}

	
}
