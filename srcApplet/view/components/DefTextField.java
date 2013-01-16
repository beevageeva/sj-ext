package view.components;

import global.Errors;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextField;



import messages.Messages;


public class DefTextField extends JTextField{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int ncol;
	
	public DefTextField(int ncol) {
		super(ncol);
		this.ncol = ncol;
		addKeyListener(new DigitTextFieldKeyAdapter());
	}
	
	public boolean validateFieldLength(String fieldname , Errors err ){
		if(getText()==null || getText().length() == 0){
			err.addError(Messages.getFormattedText("field_empty", new String[]{fieldname}));
			return false;
		}
		if(getText().length()>ncol){
			err.addError(Messages.getFormattedText("field_toomany_digits", new String[]{fieldname, String.valueOf(ncol)}));
			return false;
		}
		return true;
	}
	
	public boolean validateFieldMinValue(String fieldname , Errors err , int minValue ){
		int val ;
		try{
			val = Integer.parseInt(getText());
			if(val<minValue){
				err.addError(Messages.getFormattedText("field_must_ge_than", new String[]{fieldname, String.valueOf(minValue)}));
				return false;
			}
		}
		catch(NumberFormatException e){
			err.addError(Messages.getFormattedText("field_not_number", new String[]{fieldname}));
			return false;
		}
		return true;
	}
	
	public boolean validateFieldMaxValue(String fieldname , Errors err , int maxValue ){
		int val ;
		try{
			val = Integer.parseInt(getText());
			if(val>maxValue){
				err.addError(Messages.getFormattedText("field_must_le_than", new String[]{fieldname, String.valueOf(maxValue)}));
				return false;
			}
		}
		catch(NumberFormatException e){
			err.addError(Messages.getFormattedText("field_not_number", new String[]{fieldname}));
			return false;
		}
		return true;
	}
	
	public boolean validateField(String fieldname , Errors err , int minValue , int maxValue){
		return validateFieldLength(fieldname, err) && validateFieldMinValue(fieldname, err, minValue) && validateFieldMaxValue(fieldname, err, maxValue);
	}
	
	class DigitTextFieldKeyAdapter extends KeyAdapter{

	    public void keyTyped(KeyEvent e) {
	        char c = e.getKeyChar();      
	        if (!((Character.isDigit(c) ||
	           (c == KeyEvent.VK_BACK_SPACE) ||
	           (c == KeyEvent.VK_DELETE)))) {
	          e.consume();
	        }
	      }

	}

}
