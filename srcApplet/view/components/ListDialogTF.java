package view.components;

import java.awt.Component;
import java.awt.Frame;

import javax.swing.JOptionPane;

public class ListDialogTF {

	private static ListChooserTF dialog;

	/**
	 * Set up and show the dialog. The first Component argument determines which
	 * frame the dialog depends on; it should be a component in the dialog's
	 * controlling frame. The second Component argument should be null if you
	 * want the dialog to come up with its left corner in the center of the
	 * screen; otherwise, it should be the component on top of which the dialog
	 * should appear.
	 */
	public static void showDialog(Component frameComp, Component locationComp,
			String labelText, String title, String[] possibleValues,
			int initialIndex, String longValue, String okLabel,
			String[] inputTextLabel, int[] inputTextMinValue, int[] inputTextMaxValue, int[] inputTextDefaultValues) {
		Frame frame = JOptionPane.getFrameForComponent(frameComp);
		dialog = new ListChooserTF(frame, locationComp, labelText, title,
				possibleValues, initialIndex, longValue, okLabel,
				inputTextLabel, inputTextMinValue, inputTextMaxValue, inputTextDefaultValues);
		dialog.setVisible(true);
	}
	
	public static void showDialog(Component frameComp, Component locationComp,
			String labelText, String title, String[] possibleValues,
			int initialIndex, String longValue, String okLabel,
			String[] inputTextLabel, int[] inputTextMinValue, int[] inputTextMaxValue, int[] inputTextDefaultValues, ListTFValidator val){
		showDialog(frameComp, locationComp, labelText, title, possibleValues, initialIndex, longValue, okLabel, inputTextLabel, inputTextMinValue, inputTextMaxValue, inputTextDefaultValues);
		dialog.setListTFValidator(val);
	}

	public static int getSelectedIndex() {
		return dialog==null?-1:dialog.getSelectedIndex();
	}

	public static int[] getValues() {
		return dialog.getTextValues();
	}


}
