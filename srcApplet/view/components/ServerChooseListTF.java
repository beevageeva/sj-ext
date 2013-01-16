package view.components;

import java.awt.Component;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.JOptionPane;

import messages.Messages;

public class ServerChooseListTF extends ServerChooseList {

	public ServerChooseListTF(Component parent, String urlString) {
		super(parent, urlString);
	}

	@Override
	public String getName() {
		int sIndex = ListDialogTF.getSelectedIndex();
		if (sIndex != -1) {
			return listValues[sIndex];
		}
		return null;
	}

	public int[] getTextValues() {
		return ListDialogTF.getValues();
	}

	public void showDialog(String[] textLabels, int[] textFieldMinValues,
			int[] textFieldMaxValues, int[] textFieldDefaultValues) {
		this.showDialog(textLabels, textFieldMinValues, textFieldMaxValues,
				textFieldDefaultValues, null);
	}

	public void showDialog(String[] textLabels, int[] textFieldMinValues,
			int[] textFieldMaxValues, int[] textFieldDefaultValues,
			ListTFValidator val) {

		listValues = null;
		try {
			listValues = getListValues();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (listValues != null) {
			if (val == null) {
				ListDialogTF.showDialog(parent, null, Messages
						.getText("choose_server_file"), Messages
						.getText("choose_server_file"), listValues, -1, null,
						"Ok", textLabels, textFieldMinValues,
						textFieldMaxValues, textFieldDefaultValues);
			} else {
				ListDialogTF.showDialog(parent, null, Messages
						.getText("choose_server_file"), Messages
						.getText("choose_server_file"), listValues, -1, null,
						"Ok", textLabels, textFieldMinValues,
						textFieldMaxValues, textFieldDefaultValues, val);

			}
		} else {
			JOptionPane.showMessageDialog(parent, Messages
					.getText("list_from_server_not_available"));
		}
	}

}
