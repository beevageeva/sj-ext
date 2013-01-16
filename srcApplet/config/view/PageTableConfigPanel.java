package config.view;

import global.Errors;
import global.Helper;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import messages.Messages;

import view.components.DefTextField;
import view.components.Graph;
import view.components.RadioGroupPanel;

import config.model.Configuration;
import config.model.MainMemCacheConfig;
import config.model.PageTableConfig;
import config.model.PageTableConfig.DirectMappedPageTableConfig;
import config.model.PageTableConfig.InverseMappedPageTableConfig;
import constants.ConstantsValues;

public class PageTableConfigPanel extends JPanel implements
		ConfigurationElement {

	private RadioGroupPanel directMappedRG;

	private DirectConfigPanel direct;

	private InverseConfigPanel inverse;

	private JPanel mappingPanel;

	private PageTableConfig pageTableConfig;

	private DefTextField accessTimeUnitsTF;

	public PageTableConfigPanel(PageTableConfig pageTableConfig) {
		super();
		setSize(800, 600);
		this.pageTableConfig = pageTableConfig;
		setLayout(new BorderLayout());
		accessTimeUnitsTF = new DefTextField(2);
		directMappedRG = new RadioGroupPanel("mapping type", new String[] {
				"direct", "inverse" }, new short[] { 0, 1 }, 0, true);
		JPanel northPanel = new JPanel();
		northPanel.add(Graph.createPanel(accessTimeUnitsTF, Messages
				.getText("access_time_units"), null));
		northPanel.add(directMappedRG);
		add(northPanel, BorderLayout.NORTH);
		mappingPanel = new JPanel(new GridLayout(2, 1));

		direct = new DirectConfigPanel();
		inverse = new InverseConfigPanel();

		add(mappingPanel, BorderLayout.CENTER);
		directMappedRG.addActionListener((short) 0, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				mappingPanel.remove(inverse);
				mappingPanel.add(direct);
				mappingPanel.revalidate();
				mappingPanel.repaint();
				updateUI();
			}
		});
		directMappedRG.addActionListener((short) 1, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				mappingPanel.remove(direct);
				mappingPanel.add(inverse);
				mappingPanel.revalidate();
				mappingPanel.repaint();
				updateUI();
			}
		});
		accessTimeUnitsTF.setText(String
				.valueOf(pageTableConfig.accessTimeUnits));
		if (pageTableConfig.getMappingType() == PageTableConfig.NO_MAPPING) {
			pageTableConfig.setMappingType(PageTableConfig.DIRECT_MAPPED_TYPE);
		}
		if (pageTableConfig.getMappingType() == PageTableConfig.DIRECT_MAPPED_TYPE) {
			directMappedRG.setSelectedValue((short) 0);
			mappingPanel.add(direct);
		} else {
			directMappedRG.setSelectedValue((short) 1);
			mappingPanel.add(inverse);
		}

		setVisible(true);
	}

	public void saveFields() {
		pageTableConfig.accessTimeUnits = Integer.parseInt(accessTimeUnitsTF
				.getText());
		if (directMappedRG.getValue() == 0) {
			pageTableConfig.setMappingType(PageTableConfig.DIRECT_MAPPED_TYPE);
			// direct
			direct.saveFields();
		} else {
			// inverse
			pageTableConfig.setMappingType(PageTableConfig.INVERSE_MAPPED_TYPE);
			inverse.saveFields();
		}
	}

	class DirectConfigPanel extends JPanel implements ConfigurationElement {
		private static final long serialVersionUID = 1L;

		private DefTextField numberLevelsTF;

		private JPanel bottomPanel;

		private JPanel lengthsPanel;

		private JButton cfgButton;

		private JLabel lengthsLabel;

		private RadioGroupPanel searchMethod;

		private DefTextField[] lengthOffsets;

		public DirectConfigPanel() {
			super();
			setLayout(new GridLayout(1, 2));
			setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

			JPanel leftPanel = new JPanel(new GridLayout(3, 1));

			leftPanel.setBorder(BorderFactory
					.createEtchedBorder(EtchedBorder.LOWERED));
			JPanel upperPanel = new JPanel();
			upperPanel.add(new JLabel("number of levels"));
			numberLevelsTF = new DefTextField(2);
			upperPanel.add(numberLevelsTF);
			cfgButton = new JButton("config");
			cfgButton.addActionListener(new CfgActionListener());
			upperPanel.add(cfgButton);
			leftPanel.add(upperPanel);

			JPanel lp = new JPanel();
			lengthsLabel = new JLabel();
			lp.add(lengthsLabel);
			leftPanel.add(lp);

			bottomPanel = new JPanel();
			lengthsPanel = new JPanel();
			lengthsPanel.setBorder(BorderFactory
					.createEtchedBorder(EtchedBorder.LOWERED));
			JButton okButton = new JButton("ok");
			okButton.addActionListener(new OkActionListener());
			bottomPanel.add(lengthsPanel);
			bottomPanel.add(okButton);
			bottomPanel.setVisible(false);
			leftPanel.add(bottomPanel);
			add(leftPanel);
			searchMethod = new RadioGroupPanel("search method", new String[] {
					"top-down", "bottom-up" }, new short[] { 0, 1 }, 0, false);
			add(searchMethod);
			if (pageTableConfig.getMappingType() == PageTableConfig.DIRECT_MAPPED_TYPE) {
				DirectMappedPageTableConfig dptCfg = (DirectMappedPageTableConfig) pageTableConfig
						.getMappingConfig();
				searchMethod.setSelectedValue((short) (dptCfg
						.isSearchMethodTopDown() ? 0 : 1));
				numberLevelsTF.setText(String
						.valueOf(dptCfg.getOffsetsLength().length));
				lengthOffsets = new DefTextField[dptCfg.getOffsetsLength().length];
				for (int i = 0; i < lengthOffsets.length; i++) {
					lengthOffsets[i] = new DefTextField(2);
					lengthOffsets[i].setText(String.valueOf(dptCfg
							.getOffsetsLength()[i]));
				}

				updateLabelText();
			}
			setVisible(true);
		}

		private void updateLabelText() {
			StringBuffer sb = new StringBuffer("lengths :");
			int[] lengths = getNumberLevels();
			for (int i = 0; i < lengths.length; i++) {
				sb.append(" ");
				sb.append(lengths[i]);
			}

			lengthsLabel.setText(sb.toString());
		}

		public boolean isSearchMethodTopDown() {
			return searchMethod.getValue() == 0;
		}

		// public int[] getDefNumberLevels() {
		// int nLevels = Integer.parseInt(numberLevelsTF.getText());
		// if (nLevels == 0) {
		// // TODO another method to force nLevels>0
		// nLevels = 1;
		// numberLevelsTF.setText("1");
		// JOptionPane.showMessageDialog(this,
		// "The number of levels must be greater than 0");
		// }
		// int[] res = new int[nLevels];
		// int vpnNBits = Configuration.getInstance().virtualAddrNBits
		// - Helper.getPowerOf2(Configuration.getInstance().pageSizeNBits);
		// int defLength = nLevels == 0 ? 0 : vpnNBits / nLevels;
		// res[0] = vpnNBits - (nLevels - 1) * defLength;
		// for (int i = 1; i < nLevels; i++) {
		// res[i] = defLength;
		// }
		// return res;
		// }

		private int[] getNumberLevels() {
			int[] res = new int[lengthOffsets.length];
			for (int i = 0; i < lengthOffsets.length; i++) {
				res[i] = Integer.parseInt(lengthOffsets[i].getText());
			}
			return res;
		}

		public void saveFields() {
			DirectMappedPageTableConfig dptCfg = (DirectMappedPageTableConfig) pageTableConfig
					.getMappingConfig();
			dptCfg.setOffsetsLength(direct.getNumberLevels());
			dptCfg.setSearchMethodTopDown(direct.isSearchMethodTopDown());
		}

		class CfgActionListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				lengthsPanel.removeAll();
				int[] defValues = ((DirectMappedPageTableConfig) pageTableConfig
						.getMappingConfig()).getOffsetsLength();
				lengthOffsets = new DefTextField[defValues.length];
				for (int i = 0; i < lengthOffsets.length; i++) {
					lengthOffsets[i] = new DefTextField(2);
					lengthOffsets[i].setText(String.valueOf(defValues[i]));
					lengthsPanel.add(lengthOffsets[i]);
				}
				updateLabelText();
				bottomPanel.setVisible(true);
				cfgButton.setEnabled(false);
				numberLevelsTF.setEnabled(false);
			}

		}

		class OkActionListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				updateLabelText();
				bottomPanel.setVisible(false);
				cfgButton.setEnabled(true);
				numberLevelsTF.setEnabled(true);
			}

		}

		public void validateFields(Errors err) {
			numberLevelsTF.validateFieldLength("number levels", err);
			numberLevelsTF.validateFieldMinValue("number levels", err, 1);
			if (lengthOffsets == null) {
				err.addError("offsets must be defined");
				return;
			}
			for (int i = 0; i < lengthOffsets.length; i++) {
				lengthOffsets[i].validateFieldLength("offset[]", err);
			}

		}
	}

	class InverseConfigPanel extends JPanel implements ConfigurationElement {

		private static final long serialVersionUID = 1L;

		private DefTextField hashAnchorSizeNBTF;

		public InverseConfigPanel() {
			super();
			setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			hashAnchorSizeNBTF = new DefTextField(2);
			add(Graph.createPanel(hashAnchorSizeNBTF,
					"hash anchor number entries : 2**", null));
			if (pageTableConfig.getMappingType() == PageTableConfig.INVERSE_MAPPED_TYPE) {
				InverseMappedPageTableConfig iCfg = (InverseMappedPageTableConfig) pageTableConfig
						.getMappingConfig();
				hashAnchorSizeNBTF.setText(String.valueOf(iCfg
						.getHashAnchorSizeNBits()));
			}
			setVisible(true);
		}

		public int getHashAnchorSizeNBits() {
			return Integer.parseInt(hashAnchorSizeNBTF.getText());
		}

		public void saveFields() {
			InverseMappedPageTableConfig iCfg = (InverseMappedPageTableConfig) pageTableConfig
					.getMappingConfig();
			iCfg.setHashAnchorSizeNBits(Integer.parseInt(hashAnchorSizeNBTF
					.getText()));
		}

		public void validateFields(Errors err) {
			hashAnchorSizeNBTF
					.validateFieldLength("hash anchor size (p2)", err);
		}

	}

	public void validateFields(Errors err) {
		accessTimeUnitsTF.validateField(Messages.getText("access_time_units"),
				err, 1, ConstantsValues.MAX_ACCESS_TIME_UNITS);
		if (directMappedRG.getValue() == 0) {
			direct.validateFields(err);
		} else {
			inverse.validateFields(err);
		}

	}

}
