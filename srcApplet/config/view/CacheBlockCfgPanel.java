package config.view;

import global.Errors;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import config.model.CacheBlockCfg;
import constants.ConstantsValues;

import messages.Messages;

import view.components.DefTextField;
import view.components.Graph;

public class CacheBlockCfgPanel extends JPanel {

	protected DefTextField numberEntriesNBitsTF = new DefTextField(2);

	protected DefTextField numberEntriesInstrNBitsTF = new DefTextField(2);

	protected DefTextField accessTimeUnitsTF = new DefTextField(2);

	public CacheBlockCfgPanel(String label, CacheBlockCfg config) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		if (label != null) {
			add(new JLabel(label));
		}
		add(Graph.createPanel(numberEntriesNBitsTF, Messages
				.getText("number_entries")
				+ " 2**", "B"));
		add(Graph.createPanel(numberEntriesInstrNBitsTF, Messages
				.getText("number_entries_instr")
				+ " 2**", "B"));
		add(Graph.createPanel(accessTimeUnitsTF, Messages
				.getText("access_time_units"), null));
		if (config != null) {
			// set values
			numberEntriesNBitsTF.setText(String.valueOf(config
					.getNumberEntriesNBits()[0]));
			if (config.isDataInstrSeparated()) {
				numberEntriesInstrNBitsTF.setText(String.valueOf(config
						.getNumberEntriesNBits()[1]));
			}
			accessTimeUnitsTF.setText(String.valueOf(config
					.getAccessTimeUnits()));
		}
	}

	public void setDataInstrSep(boolean b) {
		numberEntriesInstrNBitsTF.setEnabled(b);
	}

	public void enableFields(boolean b, boolean dataInstrSep) {
		numberEntriesNBitsTF.setEnabled(b);
		if (!b) {
			numberEntriesInstrNBitsTF.setEnabled(false);
		} else {
			if (dataInstrSep) {
				numberEntriesInstrNBitsTF.setEnabled(true);
			}
		}
		accessTimeUnitsTF.setEnabled(b);
	}

	public void saveFields(CacheBlockCfg config, boolean dataInstrSep) {
		if (dataInstrSep) {
			config.setDataInstrSeparated(dataInstrSep);
			config.getNumberEntriesNBits()[1] = Integer
					.parseInt(numberEntriesInstrNBitsTF.getText());
		}
		config.getNumberEntriesNBits()[0] = Integer
				.parseInt(numberEntriesNBitsTF.getText());
		config
				.setAccessTimeUnits(Integer.parseInt(accessTimeUnitsTF
						.getText()));

	}

	public int getMinNumberEntries(Errors err, boolean isDataInstrSep) {
		if (validateFields(err, isDataInstrSep)) {
			int ne = Integer.parseInt(numberEntriesNBitsTF.getText());
			return isDataInstrSep ? Math.min(ne, Integer
					.parseInt(numberEntriesInstrNBitsTF.getText())) : ne;
		}
		return -1;
	}

	public boolean validateFields(Errors errors, boolean isDataInstrSep) {
		numberEntriesNBitsTF.validateField(Messages
				.getText("number_entries_num_bits"), errors, 1,
				ConstantsValues.MAX_VICTIM_CACHE_NUM_ENTRIES_NBITS);
		accessTimeUnitsTF.validateField(Messages.getText("access_time_units"),
				errors, 1, ConstantsValues.MAX_ACCESS_TIME_UNITS);
		if (isDataInstrSep) {
			numberEntriesInstrNBitsTF.validateField(Messages
					.getText("number_entries_instr_num_bits"), errors, 1,
					ConstantsValues.MAX_VICTIM_CACHE_NUM_ENTRIES_NBITS);
		}
		return errors.isEmpty();
	}

}
