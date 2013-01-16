package view.components;

import global.Errors;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import javax.swing.SwingUtilities;




public class JFileChooserTF extends JFileChooser{

	private static final long serialVersionUID = 8787471995451507527L;
	
	protected int[] values;
	protected DefTextField[] inputTf;
	protected String[] textLabels;
	protected int[] inputMinValues;
	protected int[] inputMaxValues;
	protected int[] inputDefaultValues;

	public JFileChooserTF(String[] textLabels, int[] inputMinValues, int[] inputMaxValues, int[] inputDefaultValues){
		super();
		if(textLabels ==null || inputMinValues==null || inputMaxValues==null || inputDefaultValues==null ||  textLabels.length!=inputMaxValues.length || textLabels.length!=inputMinValues.length||textLabels.length!=inputDefaultValues.length){
			throw new IllegalArgumentException("must have same legth");
		}
		this.textLabels = textLabels;
		this.inputMinValues = inputMinValues;
		this.inputMaxValues = inputMaxValues;
		this.inputDefaultValues = inputDefaultValues;
	     inputTf = new DefTextField[textLabels.length];
	     values = new int[textLabels.length];
	     for(int i = 0; i<textLabels.length;i++){
	    	 inputTf[i] = new DefTextField(5);
	    	 inputTf[i].setText(String.valueOf(inputDefaultValues[i]));
	     }

	}
	
	
	protected JDialog createDialog(Component parent) throws HeadlessException
	   {
	     Frame toUse = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);
	     if(toUse==null){
	    	 throw new HeadlessException("Component has no frame parent");
	     }
	     JDialog dialog = new JDialog(toUse);
	     dialog.setSize(new Dimension(500 , 400));
	     setSelectedFile(null);
	     dialog.getContentPane().setLayout(new BorderLayout());
	     Box nPanel = new Box(BoxLayout.Y_AXIS);
	     for(int i = 0 ; i<textLabels.length ;i++){
	    	 nPanel.add(Graph.createPanel(inputTf[i] , textLabels[i] , null));
	     }
	     dialog.getContentPane().add(nPanel , BorderLayout.NORTH);
	     dialog.getContentPane().add(this , BorderLayout.CENTER);
	     dialog.setModal(true);
	     dialog.invalidate();
	     dialog.repaint();
	 
	     return dialog;
	   }

	@Override
	public void setSelectedFile(File f) {
		super.setSelectedFile(f);
		if(f!=null){
			processTextFields();
		}
	}
	
	public int[] getTextValues(){
		return values;
	}
	

	private void processTextFields(){
		Errors err = new Errors();
		for(int i = 0;i<textLabels.length;i++){
			inputTf[i].validateField(textLabels[i], err, inputMinValues[i], inputMaxValues[i]);
			if(!err.isEmpty()){
				JOptionPane.showMessageDialog(this , err.displayErrors());
				setSelectedFile(null);
				return;
			}
			values[i] = Integer.parseInt(inputTf[i].getText());
		}
	}

	
	
}
