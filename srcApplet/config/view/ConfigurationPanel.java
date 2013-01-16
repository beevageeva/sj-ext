package config.view;

import global.Errors;
import global.Helper;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sun.accessibility.internal.resources.accessibility_zh_TW;

import runtime.model.RuntimeModel;

import messages.Messages;
import util.AppProperties;
import view.components.DefTextField;
import view.components.Graph;
import view.components.ListDialogTF;
import view.components.RadioGroupPanel;
import view.components.ServerChooseList;
import config.builder.ConfigReaderXML;
import config.model.CacheBlockCfg;
import config.model.CacheChainMem;
import config.model.CacheChainPT;
import config.model.CacheConfigMem;
import config.model.CacheConfigPT;
import config.model.Configuration;
import config.model.ConfigurationPanelListener;
import config.model.IndexCacheBlockCfg;
import config.model.PageTableConfig;
import config.model.SMPNodeConfig;
import config.model.PageTableConfig.DirectMappedPageTableConfig;
import config.model.PageTableConfig.InverseMappedPageTableConfig;
import config.view.ConfigurationPanel.SMPNodeConfigPanel.CPUPanel.CacheChainPanelPT.SpinnerPanel.SpinnerConfigPanel;
import constants.ConstantsValues;

public class ConfigurationPanel extends JPanel implements ConfigurationElement {

	private DefTextField virtualAddressNBitsTF, pageSizeNBitsTF;

	private RadioGroupPanel cacheMemCoherencePolicyRG;

	private JPanel smpNodesPanel;

	private List<ConfigurationPanelListener> listeners = new ArrayList<ConfigurationPanelListener>();

	public ConfigurationPanel() {
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		virtualAddressNBitsTF = new DefTextField(2);
		JPanel fpanel = new JPanel();
		fpanel.add(Graph.createPanel(virtualAddressNBitsTF, Messages
				.getText("virtual_address")
				+ "2**", "B"));
		pageSizeNBitsTF = new DefTextField(2);
		fpanel.add(Graph.createPanel(pageSizeNBitsTF, Messages
				.getText("page_size")
				+ "2**", "B"));
		JButton bt = new JButton(Messages.getText("pt"));
		bt.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				new DialogComponent(new PageTableConfigPanel(Configuration
						.getInstance().pageTableConfig));
			}
		});
		fpanel.add(bt);
		add(fpanel);
		cacheMemCoherencePolicyRG = Graph.createCacheCoherencePolicyRG(Messages
				.getText("mem_cache_coherence_policy"), false);
		cacheMemCoherencePolicyRG.setEnabled(false);
		fpanel = new JPanel();
		fpanel.add(cacheMemCoherencePolicyRG);
		add(fpanel);
		smpNodesPanel = new JPanel();
		smpNodesPanel.setLayout(new BoxLayout(smpNodesPanel, BoxLayout.Y_AXIS));
		add(new JScrollPane(smpNodesPanel));
		bt = new JButton(Messages.getText("add_smp_node"));
		bt.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				SMPNodeConfig smpNode = new SMPNodeConfig();
				new SMPNodePanelDialogComponent(new SMPNodeConfigPanel(smpNode))
						.setVisible(true);
			}
		});
		add(bt);
		// ok button, load config buttons
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout());
		bt = new JButton("OK");
		bt.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Errors err = new Errors();
				validateFields(err);
				if (!err.isEmpty()) {
					JOptionPane.showMessageDialog(smpNodesPanel, err
							.displayErrors());
				} else {
					saveFields();
					configurationValidated();
				}
			}
		});
		buttonsPanel.add(bt);
		bt = new JButton(Messages.getText("load_local_config"));
		bt.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser configChooser = new JFileChooser();
				ExampleFileFilter fileFilter = new ExampleFileFilter("xml");
				configChooser.setFileFilter(fileFilter);
				int returnVal = configChooser.showOpenDialog(smpNodesPanel);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						File file = configChooser.getSelectedFile();
						configureFromInputStream(new FileInputStream(file));
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}
			}
		});
		buttonsPanel.add(bt);
		bt = new JButton(Messages.getText("load_server_config"));
		bt.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ServerChooseList configListChooser = new ServerChooseList(
						smpNodesPanel, AppProperties.getInstance().getProperty(
								"server.context")
								+ "sendFileNames.html?fileType=conf");
				configListChooser.showDialog();
				String serverFileName = configListChooser.getName();
				if (serverFileName != null) {
					try {
						configureFromInputStream(new URL(AppProperties
								.getInstance().getProperty("server.context")
								+ "files/conf/" + serverFileName).openStream());
					} catch (MalformedURLException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}

			}
		});
		buttonsPanel.add(bt);
		add(buttonsPanel);

		// update from configuration
		virtualAddressNBitsTF.setText(String.valueOf(Configuration
				.getInstance().virtualAddrNBits));
		pageSizeNBitsTF.setText(String
				.valueOf(Configuration.getInstance().pageSizeNBits));

	}

	public void saveFields() {
		Configuration.getInstance().virtualAddrNBits = Integer
				.parseInt(virtualAddressNBitsTF.getText());
		if (Configuration.getInstance().getTotalNumberOfProcs() > 1) {
			Configuration.getInstance().cacheMemCacheCoherencePolicy = cacheMemCoherencePolicyRG
					.getValue();
		}
		Configuration.getInstance().pageSizeNBits = Integer
				.parseInt(pageSizeNBitsTF.getText());
		// set numberSets of each mainmem depending of the type of the page
		// table
		for (int i = 0; i < Configuration.getInstance().smpNodeConfigs.size(); i++) {
			Configuration.getInstance().smpNodeConfigs.get(i).mainMemoryConfig.numberSetsNBits = Configuration
					.getInstance().pageTableConfig.getMappingType() == PageTableConfig.DIRECT_MAPPED_TYPE ? 0
					: Configuration.getInstance().smpNodeConfigs.get(i).mainMemoryConfig
							.getNumberEntriesNBits();
		}

	}

	public void validateFields(Errors errors) {
		if(Configuration.getInstance().smpNodeConfigs.size()==0){
			errors.addError(Messages.getFormattedText("field_must_ge_than", new String[]{Messages.getText("num_nodes"),"1"}));
			return;
		}
		// VirtualAddress must be greater tha the max(PhysicalAddress) of all
		// main mems
		int tmNB = Helper.getPowerOf2(Configuration.getInstance()
				.getMemTotalNumberOfPages());
		pageSizeNBitsTF.validateField("page_size_num_bits", errors, 1,
				ConstantsValues.MAX_MEM_VIRT_NBITS);
		virtualAddressNBitsTF.validateField(Messages
				.getText("virtual_address_num_bits"), errors, tmNB,
				ConstantsValues.MAX_MEM_VIRT_NBITS
						- Configuration.getInstance().pageSizeNBits);
		int vpnb, psnb = -1;
		if (errors.isEmpty()) {
			vpnb = Integer.parseInt(virtualAddressNBitsTF.getText());
			psnb = Integer.parseInt(pageSizeNBitsTF.getText());

			// validate page table

			if (Configuration.getInstance().pageTableConfig.getMappingType() == PageTableConfig.DIRECT_MAPPED_TYPE) {
				DirectMappedPageTableConfig dmptcfg = (DirectMappedPageTableConfig) Configuration
						.getInstance().pageTableConfig.getMappingConfig();
				if (dmptcfg.getOffsetsLength().length > vpnb - psnb) {
					errors.addError(Messages.getFormattedText(
							"field_must_le_than", new String[] {
									"number levels ",
									String.valueOf(vpnb - psnb) }));
				}
				int sum = 0;
				for (int i = 0; i < dmptcfg.getOffsetsLength().length; i++) {
					sum += dmptcfg.getOffsetsLength()[i];
				}
				if (sum != vpnb - psnb) {
					errors
							.addError("sum of lengths must be eq to number of virtual pages :sumoflength = "
									+ sum + ",num_virt_pages=" + (vpnb - psnb));
				}
			} else if (Configuration.getInstance().pageTableConfig
					.getMappingType() == PageTableConfig.INVERSE_MAPPED_TYPE) {
				int hashAnchorSizeNB = ((InverseMappedPageTableConfig) Configuration
						.getInstance().pageTableConfig.getMappingConfig())
						.getHashAnchorSizeNBits();
				if (hashAnchorSizeNB < tmNB) {
					errors.addError(Messages.getFormattedText(
							"field_must_ge_than", new String[] {
									"impt_hash_anchor_size",
									String.valueOf(tmNB) }));
				}
				if (hashAnchorSizeNB > vpnb - psnb) {
					errors.addError(Messages.getFormattedText(
							"field_must_le_than", new String[] {
									"impt_hash_anchor_size_num_bits",
									String.valueOf(vpnb - psnb) }));
				}

			}
		}

	}

	private void configureFromInputStream(InputStream is) {
		Configuration.resetConfiguration();
		RuntimeModel.reset();
		ConfigReaderXML.setConfig(is);
		updateFromConfiguration();

	}

	public void updateFromConfiguration() {
		// only put onscreen the smpNodes and set interconnection policy?;
		smpNodesPanel.removeAll();
		for (int i = 0; i < Configuration.getInstance().smpNodeConfigs.size(); i++) {
			addSMPNodePanel(Configuration.getInstance().smpNodeConfigs.get(i));
		}
		if (Configuration.getInstance().getTotalNumberOfProcs() > 1) {
			cacheMemCoherencePolicyRG.setEnabled(true);

		}
		repaint();
		updateUI();
	}

	protected void configurationValidated() {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).configurationValidated();
		}
	}

	public void addConfigurationPanelListener(ConfigurationPanelListener l) {
		listeners.add(l);
	}

	class DialogComponent extends JDialog implements ActionListener {

		protected JComponent panel;

		protected boolean validated;

		protected JPanel buttonsPanel;

		public boolean isValidated() {
			return validated;
		}

		public DialogComponent(JComponent panel) {
			this.panel = panel;
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setSize(800, 600);
			setLayout(new BorderLayout());
			add(panel, BorderLayout.CENTER);
			buttonsPanel = new JPanel();
			buttonsPanel.setLayout(new FlowLayout());
			JButton okButton = new JButton("OK");
			okButton.setActionCommand("ok");
			okButton.addActionListener(this);
			buttonsPanel.add(okButton);
			add(buttonsPanel, BorderLayout.SOUTH);
			setVisible(true);

		}

		public void actionPerformed(ActionEvent e) {
			validated = false;
			if ("ok".equalsIgnoreCase(e.getActionCommand())) {
				if (!(panel instanceof ConfigurationElement)) {
					setVisible(false);
					validated = true;
				} else {
					ConfigurationElement cePanel = (ConfigurationElement) panel;
					Errors errors = new Errors();
					cePanel.validateFields(errors);
					if (!errors.isEmpty()) {
						JOptionPane.showMessageDialog(panel, errors
								.displayErrors());
						validated = false;
					} else {

						cePanel.saveFields();
						setVisible(false);
						validated = true;
						this.dispose();
					}
				}
			}
		}
	}

	class SMPNodePanelDialogComponent extends DialogComponent {

		public SMPNodePanelDialogComponent(SMPNodeConfigPanel panel) {
			super(panel);
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("cancel");
			cancelButton.addActionListener(this);
			buttonsPanel.add(cancelButton);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			if ("cancel".equalsIgnoreCase(e.getActionCommand())) {
				setVisible(false);
				validated = false;
				this.dispose();
			}
			if (validated) {
				Configuration.getInstance().smpNodeConfigs
						.add(((SMPNodeConfigPanel) panel).smpNode);
				addSMPNodePanel(Configuration.getInstance().smpNodeConfigs.get(Configuration.getInstance().smpNodeConfigs
						.size() - 1));
				if (Configuration.getInstance().getTotalNumberOfProcs() > 1) {
					cacheMemCoherencePolicyRG.setEnabled(true);
				}
				this.dispose();
			}
			
		}

	}

	private void addSMPNodePanel(final SMPNodeConfig smpNode) {
		JButton bt = new JButton(smpNode.name);
		final JPanel smpButtonsPanel = new JPanel();
		smpButtonsPanel.add(bt);
		bt.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				new DialogComponent(new SMPNodeConfigPanel(smpNode))
						.setVisible(true);
			}
		});
		bt = new JButton(Messages.getText("remove"));
		smpButtonsPanel.add(bt);
		bt.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				Configuration.getInstance().smpNodeConfigs.remove(smpNode);
				smpNodesPanel.remove(smpButtonsPanel);
				smpNodesPanel.repaint();
				smpNodesPanel.updateUI();

			}});
		smpNodesPanel.add(smpButtonsPanel);
		smpNodesPanel.repaint();
		updateUI();
	}

	class SMPNodeConfigPanel extends JPanel implements ConfigurationElement {

		private DefTextField diskAccessTimeTF;

		private JTextField nameTF;
		
		private RadioGroupPanel hasRemoteCacheRG  = new RadioGroupPanel(Messages
				.getText("has_remote_cache"), new String[] { "yes", "no" },
				new short[] { 0, 1 }, 0, true);
		
		private RadioGroupPanel isRemoteCacheDISepRG;

		private CacheBlockCfgPanel remoteCachePanel;

		private JPanel memCachesPanel;

		private JPanel ptCachesPanel;

		private JPanel cpusPanel;

		private SMPNodeConfig smpNode;

		public SMPNodeConfigPanel(SMPNodeConfig smpnode) {
			super();
			this.smpNode = smpnode;
			setSize(800, 600);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			diskAccessTimeTF = new DefTextField(3);
			add(Graph.createPanel(diskAccessTimeTF, Messages
					.getText("disk_access_time"), null));
			nameTF = new JTextField(10);
			add(Graph.createPanel(nameTF, Messages
					.getText("name"), null));
			
			isRemoteCacheDISepRG = new RadioGroupPanel(Messages
					.getText("data_instr_sep"), new String[] { "yes", "no" },
					new short[] { 0, 1 }, 0, true);
			remoteCachePanel = new CacheBlockCfgPanel(Messages
					.getText("remote_cache"), smpNode.remoteDataCache);
			hasRemoteCacheRG.addActionListener((short) 0,
					new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							remoteCachePanel.enableFields(true, isRemoteCacheDISepRG.getValue() == 0);
						}
					});
			hasRemoteCacheRG.addActionListener((short) 1,
					new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							remoteCachePanel.enableFields(false, isRemoteCacheDISepRG.getValue() == 0);
						}
					});
			isRemoteCacheDISepRG.addActionListener((short) 0,
					new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							remoteCachePanel.setDataInstrSep(true);
						}
					});
			isRemoteCacheDISepRG.addActionListener((short) 1,
					new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							remoteCachePanel.setDataInstrSep(false);
						}
					});
			JPanel rcache = new JPanel();
			rcache.add(hasRemoteCacheRG);
			rcache.add(isRemoteCacheDISepRG);
			rcache.add(remoteCachePanel);
			add(rcache);
			final JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new GridLayout(1, 2));

			final JPanel leftPanel = new JPanel();
			leftPanel.setLayout(new BorderLayout());
			cpusPanel = new JPanel();
			cpusPanel.setLayout(new BoxLayout(cpusPanel, BoxLayout.Y_AXIS));
			leftPanel.add(new JScrollPane(cpusPanel));
			mainPanel.add(leftPanel);

			JPanel rightPanel = new JPanel();
			// caches panel
			JPanel cachesRightPanel = new JPanel();
			cachesRightPanel.setLayout(new GridLayout(1, 2));
			// memCachesPanel
			JPanel memCachesPanelAll = new JPanel();
			memCachesPanelAll.setBorder(BorderFactory
					.createEtchedBorder(EtchedBorder.LOWERED));
			memCachesPanelAll.setLayout(new BoxLayout(memCachesPanelAll,
					BoxLayout.Y_AXIS));
			JButton bt = new JButton(Messages.getText("mm"));
			bt.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					new DialogComponent(new MainMemoryConfigPanel(
							smpNode.mainMemoryConfig));
				}
			});
			memCachesPanelAll.add(bt);
			bt = new JButton(Messages.getText("add_mem_cache"));
			bt.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					CacheConfigMem ccfgmem = new CacheConfigMem();
					new CacheToMemDialogComponent(ccfgmem);
				}
			});
			memCachesPanelAll.add(bt);
			memCachesPanel = new JPanel();
			memCachesPanel.setLayout(new BoxLayout(memCachesPanel,
					BoxLayout.Y_AXIS));
			memCachesPanelAll.add(new JScrollPane(memCachesPanel));
			rightPanel.add(memCachesPanelAll);

			JPanel ptCachesPanelAll = new JPanel();
			ptCachesPanelAll.setBorder(BorderFactory
					.createEtchedBorder(EtchedBorder.LOWERED));
			ptCachesPanelAll.setLayout(new BoxLayout(ptCachesPanelAll,
					BoxLayout.Y_AXIS));

			bt = new JButton(Messages.getText("add_pt_cache"));
			bt.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					CacheConfigPT ccfgpt = new CacheConfigPT();
					new CacheToPTDialogComponent(ccfgpt);
				}
			});
			ptCachesPanelAll.add(bt);
			ptCachesPanel = new JPanel();
			ptCachesPanel.setLayout(new BoxLayout(ptCachesPanel,
					BoxLayout.Y_AXIS));
			ptCachesPanelAll.add(new JScrollPane(ptCachesPanel));
			rightPanel.add(ptCachesPanelAll);

			mainPanel.add(rightPanel);
			add(mainPanel);

			// add cpu
			bt = new JButton(Messages.getText("add_cpu"));
			bt.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					smpNode.cpuCachesToPT
							.add(new CacheChainPT<CacheConfigPT>());
					smpNode.cpuCachesToMem.add(new CacheChainMem());
					cpusPanel
							.add(new CPUPanel(smpNode.cpuCachesToPT.size() - 1));
					cpusPanel.repaint();
					updateUI();
				}
			});

			add(bt);

			// set fields from config
			remoteCachePanel.enableFields(smpnode.hasRemoteCache(), smpnode.hasRemoteCache()?smpnode.remoteDataCache.isDataInstrSeparated():false);
			hasRemoteCacheRG.setSelectedValue((short) (smpnode.hasRemoteCache()?0:1));
			if(smpnode.hasRemoteCache()){	
				isRemoteCacheDISepRG.setSelectedValue(smpNode.remoteDataCache
					.isDataInstrSeparated() ? (short) 0 : (short) 1);
			}
			CPUPanel cpupanel;
			List<CacheConfigPT> cpucacheptconfigs;
			List<CacheConfigMem> cpucachememconfigs;
			for (int i = 0; i < smpNode.cacheConfigsMem.size(); i++) {
				addCacheToMemButton(smpNode.cacheConfigsMem.get(i));
			}
			for (int i = 0; i < smpNode.cacheConfigsPT.size(); i++) {
				addCacheToPtButton(smpNode.cacheConfigsPT.get(i));
			}

			for (int i = 0; i < smpNode.cpuCachesToPT.size(); i++) {
				cpucacheptconfigs = smpNode.cpuCachesToPT.get(i).caches;
				cpucachememconfigs = smpNode.cpuCachesToMem.get(i).caches;
				cpupanel = new CPUPanel(i);
				for (int j = 0; j < cpucacheptconfigs.size(); j++) {
					cpupanel.ptcPanel.addCacheButtonToCPUPanel(
							cpucacheptconfigs.get(j).getName(), j);

				}
				for (int j = 0; j < cpucachememconfigs.size(); j++) {
					cpupanel.memcPanel.addCacheButtonToCPUPanel(
							cpucachememconfigs.get(j).getName(), j);
				}
				cpusPanel.add(cpupanel);

			}
			diskAccessTimeTF.setText(String.valueOf(smpNode.diskAccessTime));
			nameTF.setText(String.valueOf(smpNode.name));
			setVisible(true);

		}

		abstract class CacheDialogComponent extends DialogComponent {

			public CacheDialogComponent(JComponent panel) {
				super(panel);
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("cancel");
				cancelButton.addActionListener(this);
				buttonsPanel.add(cancelButton);
			}

			protected abstract void addToCacheArray();

			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				if ("cancel".equalsIgnoreCase(e.getActionCommand())) {
					setVisible(false);
					validated = false;
					this.dispose();
				}
				if (validated) {
					addToCacheArray();
				}
			}

		}

		class CacheToMemDialogComponent extends CacheDialogComponent {

			public CacheToMemDialogComponent(CacheConfigMem model) {
				super(new CacheConfigPanelMem(model));
			}

			@Override
			protected void addToCacheArray() {
				final CacheConfigMem model = ((CacheConfigPanelMem) panel)
						.getCacheConfigMem();
				smpNode.cacheConfigsMem.add(model);
				addCacheToMemButton(model);
			}

		}

		class CacheToPTDialogComponent extends CacheDialogComponent {

			public CacheToPTDialogComponent(CacheConfigPT model) {
				super(new CacheConfigPanelPT(model));
			}

			@Override
			protected void addToCacheArray() {
				final CacheConfigPT model = ((CacheConfigPanelPT) panel)
						.getCacheConfigPT();
				smpNode.cacheConfigsPT.add(model);
				addCacheToPtButton(model);
			}

		}

		private void addCacheToPtButton(final CacheConfigPT model) {
			JButton bt = new JButton(model.getName());
			bt.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					new DialogComponent(new CacheConfigPanelPT(model));
				}
			});
			ptCachesPanel.add(bt);
			ptCachesPanel.repaint();
			updateUI();
		}

		private void addCacheToMemButton(final CacheConfigMem model) {
			JButton bt = new JButton(model.getName());
			bt.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					new DialogComponent(new CacheConfigPanelMem(model));
				}
			});
			memCachesPanel.add(bt);
			memCachesPanel.repaint();
			updateUI();
		}

		public void saveFields() {
			smpNode.diskAccessTime = Integer.parseInt(diskAccessTimeTF
					.getText());
			smpNode.name = nameTF.getText();
			if(hasRemoteCacheRG.getValue()==0){
				smpNode.remoteDataCache = new CacheBlockCfg();
				remoteCachePanel.saveFields(smpNode.remoteDataCache,
					isRemoteCacheDISepRG.getValue() == 0);
			}
			else{
				smpNode.remoteDataCache = null;
			}
		}

		public void validateFields(Errors errors) {
			diskAccessTimeTF.validateField(Messages
					.getText("disk_access_time_units"), errors, 1, 500);
			if(hasRemoteCacheRG.getValue()==0){
				remoteCachePanel.validateFields(errors, isRemoteCacheDISepRG
					.getValue() == 0);
			}
			if(smpNode.cpuCachesToMem.size()==0){
				errors.addError(Messages.getFormattedText("field_must_ge_than", new String[]{"cpu number", String.valueOf(1)}));
			}
		}

		class CPUPanel extends JPanel {

			class CacheChainPanelPT<CCH extends CacheChainPT<CC>, CC extends CacheConfigPT>
					extends JPanel {

				abstract class SpinnerPanel extends JPanel implements
						ConfigurationElement {

					class SpinnerConfigPanel extends Box implements
							ConfigurationElement {
						private ConfigurationElement parent;

						public SpinnerConfigPanel(ConfigurationElement parent) {
							super(BoxLayout.Y_AXIS);
							this.parent = parent;
							JSpinner spinner = new JSpinner(numberModel);
							add(new JLabel("index of cache in array"));
							add(spinner);
						}

						public void saveFields() {
							parent.saveFields();
						}

						public void validateFields(Errors errors) {
							parent.validateFields(errors);
						}

					}

					protected SpinnerNumberModel numberModel;

					protected abstract void disabledAction();

					protected abstract String getLabelText();

					protected JButton configButton;

					protected RadioGroupPanel enabledRG;

					protected JLabel infoLabel;

					protected SpinnerConfigPanel createSpinnerConfigPanel() {
						return new SpinnerConfigPanel(this);
					}

					public SpinnerPanel(String label) {
						super();
						enabledRG = new RadioGroupPanel(label, new String[] {
								"yes", "no" }, new short[] { 0, 1 }, 1, true);
						configButton = new JButton(Messages
								.getText("configure"));
						numberModel = new SpinnerNumberModel();
						numberModel.setStepSize(1);
						if (cacheChain.caches.size() == 0) {
							numberModel.setMinimum(new Integer(-1));
							numberModel.setMaximum(new Integer(-1));
							numberModel.setValue(new Integer(-1));
						} else {
							numberModel.setMaximum(new Integer(
									cacheChain.caches.size() - 1));
							numberModel.setMinimum(new Integer(0));
							numberModel.setValue(new Integer(0));
						}
						setLayout(new BorderLayout());
						JPanel northPanel = new JPanel();

						configButton.addActionListener(new ActionListener() {

							public void actionPerformed(ActionEvent e) {
								new DialogComponent(createSpinnerConfigPanel())
										.setVisible(true);
							}
						});

						enabledRG.addActionListener((short) 0,
								new ActionListener() {

									public void actionPerformed(ActionEvent e) {
										configButton.setEnabled(true);
									}
								});

						enabledRG.addActionListener((short) 1,
								new ActionListener() {

									public void actionPerformed(ActionEvent e) {
										disabledAction();
										configButton.setEnabled(false);
									}
								});

						northPanel.add(enabledRG);
						infoLabel = new JLabel(getLabelText());
						northPanel.add(infoLabel);
						northPanel.add(configButton);
						add(northPanel, BorderLayout.NORTH);

					}
				}

				class ExclusiveCacheIndexSpinnerPanel extends SpinnerPanel {

					protected DefTextField victimBufferNumberEntriesTF;

					protected DefTextField victimBufferNumberEntriesInstrTF;

					@Override
					protected SpinnerConfigPanel createSpinnerConfigPanel() {
						return new ExcSpinnerConfigPanel(this);
					}

					protected CC getCurrentCacheConfig() {
						return cacheChain.caches.get((Integer) numberModel
								.getValue());
					}

					class ExcSpinnerConfigPanel extends SpinnerConfigPanel {

						public ExcSpinnerConfigPanel(ConfigurationElement parent) {
							super(parent);
							add(Graph.createPanel(victimBufferNumberEntriesTF,
									"numberEntries: 2 ** ", null));
							add(Graph.createPanel(
									victimBufferNumberEntriesInstrTF,
									"numberEntriesInstr: 2 ** ", null));
							numberModel.addChangeListener(new ChangeListener() {

								public void stateChanged(ChangeEvent e) {
									if (getCurrentCacheConfig()
											.isDataInstrSeparated()) {
										victimBufferNumberEntriesInstrTF
												.setEnabled(true);
									} else {
										victimBufferNumberEntriesInstrTF
												.setEnabled(false);
									}
								}
							});
						}

					}

					public ExclusiveCacheIndexSpinnerPanel(String label) {
						super(label);
						victimBufferNumberEntriesTF = new DefTextField(2);
						victimBufferNumberEntriesInstrTF = new DefTextField(2);
						if (cacheChain.exclusiveCacheCfg != null) {
							configButton.setEnabled(true);
							enabledRG.setSelectedValue((short) 0);
							numberModel
									.setValue(cacheChain.exclusiveCacheCfg.indexCache);
							CC cc = getCurrentCacheConfig();
							victimBufferNumberEntriesTF.setText(String
									.valueOf(cc.getNumberEntriesNBits()[0]));
							if (cc.isDataInstrSeparated()) {
								victimBufferNumberEntriesInstrTF
										.setEnabled(true);
								victimBufferNumberEntriesInstrTF
										.setText(String.valueOf(cc
												.getNumberEntriesNBits()[0]));
							} else {
								victimBufferNumberEntriesInstrTF
										.setEnabled(false);
							}
						} else {
							configButton.setEnabled(false);
						}
					}

					public void saveFields() {
						if (cacheChain.exclusiveCacheCfg == null) {
							cacheChain.exclusiveCacheCfg = new IndexCacheBlockCfg();
						}
						CC cc = getCurrentCacheConfig();
						cacheChain.exclusiveCacheCfg.setDataInstrSeparated(cc
								.isDataInstrSeparated());
						cacheChain.exclusiveCacheCfg.getNumberEntriesNBits()[0] = Integer
								.parseInt(victimBufferNumberEntriesTF.getText());
						if (cc.isDataInstrSeparated()) {
							cacheChain.exclusiveCacheCfg
									.getNumberEntriesNBits()[1] = Integer
									.parseInt(victimBufferNumberEntriesInstrTF
											.getText());
						}
						cacheChain.exclusiveCacheCfg.indexCache = (Integer) numberModel
								.getValue();
						infoLabel
								.setText(String
										.valueOf(cacheChain.exclusiveCacheCfg.indexCache)
										+ ","
										+ String
												.valueOf(cacheChain.exclusiveCacheCfg
														.getTotalNumberEntriesNBits()));
					}

					public void validateFields(Errors errors) {
						victimBufferNumberEntriesTF.validateField(
								"victimBufferNumberEntriesNumBits", errors, 0,
								cacheChain.caches.get(
										(Integer) numberModel.getValue())
										.getNumberEntriesNBits()[0]);
						CC cc = getCurrentCacheConfig();
						if (cc.isDataInstrSeparated()) {
							victimBufferNumberEntriesInstrTF.validateField(
									"victimBufferNumberEntriesInstrNumBits",
									errors, 0, cacheChain.caches.get(
											(Integer) numberModel.getValue())
											.getNumberEntriesNBits()[1]);
						}
					}

					@Override
					protected void disabledAction() {
						cacheChain.exclusiveCacheCfg = null;
					}

					@Override
					protected String getLabelText() {
						return cacheChain.exclusiveCacheCfg != null ? String
								.valueOf(cacheChain.exclusiveCacheCfg.indexCache)
								+ ","
								+ String.valueOf(cacheChain.exclusiveCacheCfg
										.getTotalNumberEntriesNBits())
								: "";
					}

				}

				protected JPanel cPanel;

				protected CCH cacheChain;

				protected List<CC> availCacheCfgs;

				protected ExclusiveCacheIndexSpinnerPanel exclusiveSP;

				protected void addNewCacheToArray() {
					String[] cacheNames = new String[availCacheCfgs.size()];
					for (int i = 0; i < availCacheCfgs.size(); i++) {
						cacheNames[i] = availCacheCfgs.get(i).getName();
					}
					ListDialogTF.showDialog(cPanel, null, "add new cache",
							"Add", cacheNames, -1, null, "add",
							new String[] { "index to insert" },
							new int[] { 0 },
							new int[] { availCacheCfgs.size() - 1 },
							new int[] { 0 });
					int index = ListDialogTF.getSelectedIndex();
					if (index != -1) {
						int indexInCpuArray = ListDialogTF.getValues()[0];
						cacheChain.caches.add(indexInCpuArray, availCacheCfgs
								.get(index));
						addCacheButtonToCPUPanel(availCacheCfgs.get(index)
								.getName(), indexInCpuArray);
						exclusiveSP.numberModel.setMaximum(cacheChain.caches
								.size() - 1);
						if (exclusiveSP.numberModel.getMinimum().equals(-1)) {
							exclusiveSP.numberModel.setMinimum(0);
						}
						if (exclusiveSP.numberModel.getValue().equals(-1)) {
							exclusiveSP.numberModel.setValue(-1);
						}

					}
				}

				public CacheChainPanelPT(CCH cChain, List<CC> availCCfgs) {
					super();
					setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
					cPanel = new JPanel();
					cPanel.setLayout(new BoxLayout(cPanel, BoxLayout.X_AXIS));
					cPanel.setBorder(BorderFactory
							.createEtchedBorder(EtchedBorder.LOWERED));

					this.cacheChain = cChain;
					this.availCacheCfgs = availCCfgs;
					cPanel = new JPanel();
					cPanel.setLayout(new BoxLayout(cPanel, BoxLayout.X_AXIS));
					cPanel.setBorder(BorderFactory
							.createEtchedBorder(EtchedBorder.LOWERED));
					JButton bt = new JButton(Messages.getText("new_cache"));
					bt.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							addNewCacheToArray();
						}
					});
					cPanel.add(bt);
					add(cPanel);
					exclusiveSP = new ExclusiveCacheIndexSpinnerPanel(Messages
							.getText("exclusive_cache"));
					add(exclusiveSP);
				}

				private int getButtonIndex(JButton but) {
					Component[] comps = cPanel.getComponents();
					int j = -1;
					for (int i = 0; i < comps.length; i++) {
						if (comps[i] instanceof JButton) {
							j++;
							if (but == comps[i]) {
								return j;
							}
						}
					}
					return -1;
				}

				public void addCacheButtonToCPUPanel(String buttonLabel,
						int indexInCpuArray) {

					JButton bt = new JButton(buttonLabel);
					bt.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							Object[] options = { "ok", "remove" };
							JButton but = (JButton) e.getSource();
							int indexInPTCachesIndexes = getButtonIndex(but);
							int retVal = JOptionPane.showOptionDialog(cPanel,
									cacheChain.caches
											.get(indexInPTCachesIndexes),
									"inf", JOptionPane.DEFAULT_OPTION,
									JOptionPane.WARNING_MESSAGE, null, options,
									options[0]);
							if (retVal == 1) {
								cacheChain.caches
										.remove(indexInPTCachesIndexes);
								cPanel.remove(but);
								cPanel.repaint();
								updateUI();
							}
						}
					});
					cPanel.add(bt, indexInCpuArray);
					cPanel.repaint();
					updateUI();
				}

			}

			protected CacheChainPanelPT<CacheChainMem, CacheConfigMem> memcPanel;

			protected CacheChainPanelPT<CacheChainPT<CacheConfigPT>, CacheConfigPT> ptcPanel;

			protected int cpuIndex;

			private JLabel label;

			private JPanel northPanel;

			public CPUPanel(int cpuInd) {
				super();
				this.cpuIndex = cpuInd;
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				northPanel = new JPanel();
				label = new JLabel("CPU " + String.valueOf(cpuIndex));
				label.setLabelFor(northPanel);
				northPanel.add(label);
				JButton bt = new JButton(Messages.getText("remove"));
				bt.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						removeThisCPUPanel();
					}
				});
				northPanel.add(bt);
				add(northPanel);
				ptcPanel = new CacheChainPanelPT<CacheChainPT<CacheConfigPT>, CacheConfigPT>(
						smpNode.cpuCachesToPT.get(cpuIndex),
						smpNode.cacheConfigsPT);
				add(ptcPanel);
				memcPanel = new CacheChainPanelPT<CacheChainMem, CacheConfigMem>(
						smpNode.cpuCachesToMem.get(cpuIndex),
						smpNode.cacheConfigsMem);
				add(memcPanel);

			}

			private void removeThisCPUPanel() {
				smpNode.cpuCachesToPT.remove(cpuIndex);
				smpNode.cpuCachesToMem.remove(cpuIndex);
				cpusPanel.remove(this);
				// set the indexes of all the cpusPanels greater than this
				// cpuIndex -1
				Component comp;
				for (int i = 0; i < cpusPanel.getComponentCount(); i++) {
					comp = cpusPanel.getComponent(i);
					if (comp instanceof CPUPanel
							&& ((CPUPanel) comp).cpuIndex > this.cpuIndex) {
						((CPUPanel) comp).cpuIndex--;
						// TODO this does not update the label?
						// label.setText("CPU "+((CPUPanel)comp).cpuIndex);
						northPanel.remove(label);
						label = new JLabel("CPU " + ((CPUPanel) comp).cpuIndex);
						label.setLabelFor(northPanel);
						northPanel.add(label);
						northPanel.repaint();
						northPanel.updateUI();

					}
				}
				cpusPanel.repaint();
				cpusPanel.updateUI();
				if (smpNode.cpuCachesToPT.size() == 1) {
					cacheMemCoherencePolicyRG.setEnabled(false);
				}
			}

		}
	}

}
