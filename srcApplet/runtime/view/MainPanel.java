package runtime.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import messages.Messages;
import runtime.model.CacheChainModel;
import runtime.model.RuntimeModel;
import runtime.model.SMPNodeModel;
import runtime.model.Sched;
import runtime.model.ThreadProcDesc;
import trace.Instr;
import trace.TraceFileReader;
import util.AppProperties;
import view.components.JFileChooserTF;
import view.components.ServerChooseListTF;
import view.model.cache.CacheModel;
import config.model.CacheConfigMem;
import config.model.CacheConfigPT;
import config.model.Configuration;
import config.model.PageTableConfig;
import config.model.SMPNodeConfig;
import constants.InstructionType;

public class MainPanel extends JPanel implements ActionListener, ChangeListener {

	private Box smpNodesPanel;
	private JTextArea consoleTextArea;
	private JTextField delayTF;
	private JTextField filterTextField;

	public MainPanel() {
		super();
		setSize(800, 600);
		setLayout(new BorderLayout());
		Box mainPanel = new Box(BoxLayout.Y_AXIS);
		smpNodesPanel = new Box(BoxLayout.Y_AXIS);
		smpNodesPanel.setSize(800, 600);
		add(createToolbar(), BorderLayout.PAGE_START);
		mainPanel.add(smpNodesPanel);
		// buttons panel
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEtchedBorder());
		JButton pButton;
		if (Configuration.getInstance().pageTableConfig.getMappingType() == PageTableConfig.DIRECT_MAPPED_TYPE) {
			pButton = createDialogButton("PT", new DirectPageTablePanel());
		} else {
			pButton = createDialogButton("PT", new InversePageTablePanel());
		}
		p.add(pButton);

		// view processes button
		pButton = new JButton(Messages.getText("view_processes"));
		pButton.setActionCommand("showProcesses");
		pButton.addActionListener(this);
		p.add(pButton);
		pButton = new JButton(Messages.getText("cache_directory"));
		pButton = createDialogButton(Messages.getText("cache_directory"),
				createCacheDirectoryPanel());
		p.add(pButton);
		mainPanel.add(p);
		// console(logger) panel
		p = new JPanel(new BorderLayout());
		consoleTextArea = new JTextArea(25, 70);
		consoleTextArea.setForeground(Color.WHITE);
		consoleTextArea.setBackground(Color.BLACK);
		consoleTextArea.setFont(consoleTextArea.getFont().deriveFont(20));
		consoleTextArea.setEditable(false);
		p.add(new JLabel(Messages.getText("logger")), BorderLayout.NORTH);
		p.add(new JScrollPane(consoleTextArea));
		JToolBar toolbar = new JToolBar();
		filterTextField = new JTextField(15);
		toolbar.add(filterTextField);
		pButton = new JButton(Messages.getText("showMessages"));
		pButton.setActionCommand("showMessages");
		pButton.addActionListener(this);
		toolbar.add(pButton);
		toolbar.addSeparator();
		pButton = new JButton(Messages.getText("syncObjs"));
		pButton.setActionCommand("syncObjs");
		pButton.addActionListener(this);
		toolbar.add(pButton);
		p.add(toolbar, BorderLayout.SOUTH);
		mainPanel.add(p);
		add(mainPanel);
		int N2 = Configuration.getInstance().smpNodeConfigs.size() / 2;
		smpNodesPanel.add(createSMPNodePanel(0, N2));
		smpNodesPanel.add(Box.createVerticalStrut(50));
		smpNodesPanel.add(createSMPNodePanel(N2,
				Configuration.getInstance().smpNodeConfigs.size()));
		setVisible(true);
		repaint();
	}

	private JComponent createCacheDirectoryPanel() {
		JTable dirTable = new JTable(RuntimeModel.getInstance().cacheDir);
		return new JScrollPane(dirTable);
	}

	private Box createSMPNodePanel(final int indexStart, final int indexEnd) {
		Box panel = new Box(BoxLayout.X_AXIS);
		if (indexStart < indexEnd) {
			JButton bt;
			int dx = 600 / (indexEnd - indexStart);
			for (int i = indexStart; i < indexEnd; i++) {
				bt = new JButton((Messages.getText("SMPNode"))
						+ String.valueOf(i));
				final int smpNodeIndex = i;
				bt.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						JDialog dialog = new JDialog();
						dialog.setSize(800, 600);
						dialog.setModal(false);
						dialog.add(new SMPPanel(smpNodeIndex));
						dialog.setVisible(true);
					}
				});
				panel.add(bt);
				panel.add(Box.createHorizontalStrut(dx));
			}
		}
		return panel;
	}

	class SMPPanel extends JPanel {

		private int smpNodeIndex;

		public SMPPanel(int smpNodeIndex) {
			super();
			Map<CacheConfigMem, JButton> memButtons = new HashMap<CacheConfigMem, JButton>();
			Map<CacheConfigPT, JButton> ptButtons = new HashMap<CacheConfigPT, JButton>();
			JButton mainMemButton;
			this.smpNodeIndex = smpNodeIndex;
			setSize(800, 600);
			SMPNodeConfig smpNode = Configuration.getInstance().smpNodeConfigs
					.get(smpNodeIndex);
			int numCpus = smpNode.cpuCachesToPT.size();
			List<CacheConfigMem> cacheMems;
			List<CacheConfigPT> cachePts;
			Integer cLevel;
			int maxPTLevel = 0, maxMemLevel = 0;
			Map<CacheConfigMem, Integer> memlevels = new HashMap<CacheConfigMem, Integer>();
			Map<CacheConfigPT, Integer> ptlevels = new HashMap<CacheConfigPT, Integer>();
			for (int i = 0; i < numCpus; i++) {
				cacheMems = smpNode.cpuCachesToMem.get(i).caches;
				cachePts = smpNode.cpuCachesToPT.get(i).caches;
				for (int j = 0; j < cacheMems.size(); j++) {
					cLevel = memlevels.get(cacheMems.get(j));
					if (cLevel == null || j > cLevel) {
						memlevels.put(cacheMems.get(j), j);
					}
				}
				if (maxMemLevel < cacheMems.size()) {
					maxMemLevel = cacheMems.size();
				}
				for (int j = 0; j < cachePts.size(); j++) {
					cLevel = ptlevels.get(cachePts.get(j));
					if (cLevel == null || j > cLevel) {
						ptlevels.put(cachePts.get(j), j);
					}
				}
				if (maxPTLevel < cachePts.size()) {
					maxPTLevel = cachePts.size();
				}

			}
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			JPanel leftPanel = new JPanel(new GridLayout(maxPTLevel
					+ maxMemLevel + 3, 1));
			Box rightPanel = new Box(BoxLayout.Y_AXIS);
			if(Configuration.getInstance().smpNodeConfigs.get(smpNodeIndex).hasRemoteCache()){
				rightPanel.add(createDialogButton(Messages
						.getText("remote_data_cache"),
						createCacheModelPanel(
								Messages.getText("remote_data_cache"), RuntimeModel
										.getInstance().smpNodeModels
										.get(smpNodeIndex).remoteDataCacheModel)));
			}
			Box ptVictimBufferPanel = new Box(BoxLayout.Y_AXIS);
			Box memVictimBufferPanel = new Box(BoxLayout.Y_AXIS);
			rightPanel.add(new JScrollPane(ptVictimBufferPanel));
			ptVictimBufferPanel.add(new JLabel(Messages
					.getText("pt_cache_victim_buffers")));
			rightPanel.add(new JScrollPane(memVictimBufferPanel));
			memVictimBufferPanel.add(new JLabel(Messages
					.getText("mem_cache_victim_buffers")));
			JButton cButton;

			Box[] cachePanels = new Box[maxPTLevel];
			for (int i = 0; i < maxPTLevel; i++) {
				cachePanels[i] = new Box(BoxLayout.X_AXIS);
				leftPanel.add(cachePanels[i]);
				cachePanels[i].add(Box.createHorizontalStrut(10));
			}

			for (int i = 0; i < numCpus; i++) {
				cachePts = smpNode.cpuCachesToPT.get(i).caches;
				for (int j = 0; j < cachePts.size(); j++) {
					if (ptButtons.get(cachePts.get(j)) == null) {
						cButton = createCachePTPanelButton(i, j);
						cachePanels[ptlevels.get(cachePts.get(j))].add(cButton);
						cachePanels[ptlevels.get(cachePts.get(j))].add(Box
								.createHorizontalStrut(10));
						ptButtons.put(cachePts.get(j), cButton);
					}

				}
				// add victim buffers

				CacheModel vm = RuntimeModel.getInstance().smpNodeModels
						.get(smpNodeIndex).cpuToPTCacheModels.get(i).victimBufferModel;
				if (vm != null) {
					ptVictimBufferPanel.add(createDialogButton("CPU "
							+ String.valueOf(i) + "("
							+ Messages.getText("victim_buffer") + ")",
							createCacheModelPanel("CPU " + String.valueOf(i)
									+ "(" + Messages.getText("victim_buffer")
									+ ")", vm)));
				}
			}

			JPanel cpusPanel = new JPanel();
			cpusPanel.setLayout(new BoxLayout(cpusPanel, BoxLayout.X_AXIS));
			for (int i = 0; i < numCpus; i++) {
				cpusPanel.add(createCPUPanel(i));
			}
			leftPanel.add(cpusPanel);
			cachePanels = new Box[maxMemLevel];
			for (int i = 0; i < maxMemLevel; i++) {
				cachePanels[i] = new Box(BoxLayout.X_AXIS);
				leftPanel.add(cachePanels[i]);
				cachePanels[i].add(Box.createHorizontalStrut(10));
			}
			for (int i = 0; i < numCpus; i++) {
				cacheMems = smpNode.cpuCachesToMem.get(i).caches;
				for (int j = 0; j < cacheMems.size(); j++) {
					if (memButtons.get(cacheMems.get(j)) == null) {
						cButton = createCacheMemPanelButton(i, j);
						cachePanels[memlevels.get(cacheMems.get(j))]
								.add(cButton);
						cachePanels[memlevels.get(cacheMems.get(j))].add(Box
								.createHorizontalStrut(10));
						memButtons.put(cacheMems.get(j), cButton);
					}

				}
				// add victim buffers
				CacheModel vm = RuntimeModel.getInstance().smpNodeModels
						.get(smpNodeIndex).cpuToMemCacheModels.get(i).victimBufferModel;
				if (vm != null) {
					memVictimBufferPanel.add(createDialogButton("CPU "
							+ String.valueOf(i) + "("
							+ Messages.getText("victim_buffer") + ")",
							createCacheModelPanel("CPU " + String.valueOf(i)
									+ "(" + Messages.getText("victim_buffer")
									+ ")", vm)));
				}
			}
			mainMemButton = createDialogButton("MM", createCacheModelPanel(
					"MM", RuntimeModel.getInstance().smpNodeModels
							.get(smpNodeIndex).mainMemModel));
			Box panel = new Box(BoxLayout.X_AXIS);
			panel.add(Box.createHorizontalStrut(200));
			panel.add(mainMemButton);
			panel.add(Box.createHorizontalGlue());
			leftPanel.add(panel);
			add(leftPanel);
			add(new JScrollPane(rightPanel));

		}

		private JButton createCachePTPanelButton(final int cpuIndex,
				final int index) {
			final SMPNodeConfig nodeCfg = Configuration.getInstance().smpNodeConfigs
					.get(smpNodeIndex);
			final SMPNodeModel nodeModel = RuntimeModel.getInstance().smpNodeModels
					.get(smpNodeIndex);
			final CacheConfigPT cfg = nodeCfg.cpuCachesToPT.get(cpuIndex).caches
					.get(index);
			JButton but = new JButton(cfg.getName());
			but.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					JDialog dialog = new JDialog();
					dialog.setSize(800, 600);
					dialog.setModal(false);
					CacheChainModel cc = nodeModel.cpuToPTCacheModels
							.get(cpuIndex);
					CacheModel model = cc.cacheModels.get(index);
					JPanel p = new JPanel();
					p.add(createCacheModelPanel(cfg.getName(), model));
					if (cfg.hasVictimCache()) {
						p.add(createCacheModelPanel(cfg.getName() + "("
								+ Messages.getText("victim_cache") + ")",
								nodeModel.victimCacheModels.get(model)));
					}
					dialog.add(new JScrollPane(p));
					dialog.setVisible(true);

				}
			});
			return but;
		}

		private JButton createCacheMemPanelButton(final int cpuIndex,
				final int index) {
			final SMPNodeConfig nodeCfg = Configuration.getInstance().smpNodeConfigs
					.get(smpNodeIndex);
			final SMPNodeModel nodeModel = RuntimeModel.getInstance().smpNodeModels
					.get(smpNodeIndex);
			final CacheConfigPT cfg = nodeCfg.cpuCachesToMem.get(cpuIndex).caches
					.get(index);
			JButton but = new JButton(cfg.getName());
			but.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					JDialog dialog = new JDialog();
					dialog.setSize(800, 600);
					dialog.setModal(false);
					CacheChainModel cc = nodeModel.cpuToMemCacheModels
							.get(cpuIndex);
					CacheModel model = cc.cacheModels.get(index);
					JPanel p = new JPanel();
					p.add(createCacheModelPanel(cfg.getName(), model));
					if (cfg.hasVictimCache()) {
						p.add(createCacheModelPanel(cfg.getName() + "("
								+ Messages.getText("victim_cache") + ")",
								nodeModel.victimCacheModels.get(model)));
					}
					dialog.add(new JScrollPane(p));
					dialog.setVisible(true);

				}
			});
			return but;
		}

		private JPanel createCPUPanel(int numCPU) {
			JPanel p = new JPanel();
			p.add(new JLabel("CPU " + numCPU));
			return p;

		}

	}

	private JToolBar createToolbar() {
		JToolBar tb = new JToolBar(JToolBar.HORIZONTAL);
		tb.setFloatable(false);
		tb.setRollover(true);
		JButton but = new JButton(Messages.getText("load_local_trace"));
		but.setActionCommand("loadLocalTrace");
		but.addActionListener(this);
		tb.add(but);
		but = new JButton(Messages.getText("load_server_trace"));
		but.setActionCommand("loadServerTrace");
		but.addActionListener(this);
		tb.add(but);
		tb.addSeparator();
		delayTF = new JTextField(2);
		delayTF
				.setText(String
						.valueOf((Sched.MAX_DELAY - Sched.MIN_DELAY) / 2));
		delayTF.setEditable(false);
		tb.add(delayTF);
		JSlider delaySlider = new JSlider(JSlider.HORIZONTAL, Sched.MIN_DELAY,
				Sched.MAX_DELAY, (Sched.MAX_DELAY - Sched.MIN_DELAY) / 2);
		delaySlider.addChangeListener(this);
		delaySlider.setMajorTickSpacing(10);
		delaySlider.setPaintTicks(true);
		tb.add(delaySlider);
		but = new JButton(Messages.getText("start"));
		but.setActionCommand("start");
		but.addActionListener(this);
		tb.add(but);
		return tb;
	}

	public void actionPerformed(ActionEvent e) {
		if ("loadLocalTrace".equals(e.getActionCommand())) {

			JFileChooserTF localTraceChooser = new JFileChooserTF(new String[] {
					Messages.getText("access_time_units"),
					Messages.getText("num_threads") }, new int[] { 1, 1 },
					new int[] { 200, 10 }, new int[] { 1, 1 });
			int retValue = localTraceChooser.showOpenDialog(this);
			if (retValue == JFileChooser.APPROVE_OPTION) {
				File selectedFile = localTraceChooser.getSelectedFile();
				if (selectedFile != null) {
					List<Instr> instructions = null;
					try {
						instructions = TraceFileReader.readInstructions(
								new FileInputStream(selectedFile),
								Configuration.getInstance().virtualAddrNBits);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					if (instructions != null) {
						int timeUnits = localTraceChooser.getTextValues()[0];
						int numThreads = localTraceChooser.getTextValues()[1];
						String filename = selectedFile.getName();
						String processName = filename.indexOf(".") != -1 ? filename
								.substring(0, filename.indexOf("."))
								: filename;
						RuntimeModel.getInstance().procMgr.addNewProcesses(
								instructions, processName, numThreads,
								timeUnits);
					}
				}

			}

		} else if ("loadServerTrace".equals(e.getActionCommand())) {
			// TODO validate the number of threads <=number lines in file or
			// duplicate
			ServerChooseListTF scltf = new ServerChooseListTF(smpNodesPanel,
					AppProperties.getInstance().getProperty("server.context")
							+ "sendFileNames.html?fileType=trace");

			scltf.showDialog(new String[] {
					Messages.getText("access_time_units"),
					Messages.getText("num_threads") }, new int[] { 1, 1 },
					new int[] { 200, 10 }, new int[] { 1, 1 });
			String filename = scltf.getName();
			if (filename != null) {
				List<Instr> instructions = null;
				try {
					instructions = TraceFileReader.readInstructions(new URL(
							AppProperties.getInstance().getProperty(
									"server.context")
									+ "files/trace/" + filename).openStream(),
							Configuration.getInstance().virtualAddrNBits);

				} catch (IOException ex) {
					ex.printStackTrace();
				}
				if (instructions != null) {
					int timeUnits = scltf.getTextValues()[0];
					int numThreads = scltf.getTextValues()[1];
					String processName = filename.indexOf(".") != -1 ? filename
							.substring(0, filename.indexOf(".")) : filename;
					RuntimeModel.getInstance().procMgr.addNewProcesses(
							instructions, processName, numThreads, timeUnits);
				}

			}

		} else if ("start".equals(e.getActionCommand())) {
			RuntimeModel.getInstance().sched.prepare();
			setDebuggingText(RuntimeModel.getInstance().logger.getSyncObjMessages() + RuntimeModel.getInstance().logger.getMessages(null));
		} else if ("showMessages".equals(e.getActionCommand())) {
			setDebuggingText(RuntimeModel.getInstance().logger
					.getMessages(filterTextField.getText()));
		} else if ("syncObjs".equals(e.getActionCommand())) {
			setDebuggingText(RuntimeModel.getInstance().logger
					.getSyncObjMessages());
		} else if ("showProcesses".equals(e.getActionCommand())) {
			List<List<List<ThreadProcDesc>>> procs = RuntimeModel.getInstance().procMgr.procs;
			JDialog finalPanel = new JDialog();
			finalPanel.setSize(600, 400);
			final JPanel procPanel = new JPanel(new GridLayout(procs.size(), 1));
			Box smpNodePanel;

			JButton bt;
			int oldpid = -1;
			for (int i = 0; i < procs.size(); i++) {
				smpNodePanel = new Box(BoxLayout.Y_AXIS);
				smpNodePanel.add(new JLabel(Messages.getText("SMPNode") + i));
				for (int j = 0; j < procs.get(i).size(); j++) {
					final Box smpProcPanel = new Box(BoxLayout.Y_AXIS);
					smpProcPanel.add(new JLabel(Messages.getText("Processor")
							+ j));
					for (int k = 0; k < procs.get(i).get(j).size(); k++) {
						final JPanel threadPanel = new JPanel();
						final ThreadProcDesc tpd = procs.get(i).get(j).get(k);
						threadPanel.add(new JLabel("pid=" + tpd.pid
								+ " , threadNumber" + tpd.numberThread
								+ " , procName = " + tpd.name + " , state = "
								+ ThreadProcDesc.getStateString(tpd.state)
								+ " , priority = " + tpd.priority
								+ " , timeUnits = " + tpd.timeUnits
								+ " , timeUnitsLeft = " + tpd.timeUnitsLeft
								+ " , currentInstrNbr = "
								+ tpd.currentInstrNumber));
						bt = new JButton(Messages.getText("show_instructions"));
						bt.addActionListener(new ActionListener() {

							public void actionPerformed(ActionEvent e) {
								JTextArea ta = new JTextArea(20, 40);
								for (int t = 0; t < tpd.instructions.size(); t++) {
									ta
											.append(InstructionType
													.getString(tpd.instructions
															.get(t).type));
									ta.append(" ");
									ta.append(tpd.instructions.get(t).va);
									ta.append("(D:");
									ta.append(String.valueOf(Integer.parseInt(
											tpd.instructions.get(t).va, 2)));
									ta.append(")\n");
								}
								JScrollPane sp = new JScrollPane(ta);
								sp.setPreferredSize(new Dimension(800, 200));
								JOptionPane.showMessageDialog(procPanel, sp);
							}
						});
						threadPanel.add(bt);

						if (tpd.pid!=oldpid) {
							bt = new JButton(Messages.getText("remove_process"));
							bt.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									RuntimeModel.getInstance().procMgr
											.removeProcess(tpd.pid);
									smpProcPanel.remove(threadPanel);
									smpProcPanel.repaint();
									smpProcPanel.updateUI();
								}
							});
						}
						oldpid = tpd.pid;
						threadPanel.add(bt);

						smpProcPanel.add(threadPanel);
					}

					smpNodePanel.add(smpProcPanel);
				}

				procPanel.add(smpNodePanel);
			}

			finalPanel.add(new JScrollPane(procPanel));
			finalPanel.setVisible(true);
		}

	}

	public void stateChanged(ChangeEvent e) {
		JSlider source = (JSlider) e.getSource();
		if (source.getValueIsAdjusting()) {
			int delay = (int) source.getValue();
			RuntimeModel.getInstance().sched.setDelay(delay);
			delayTF.setText(String.valueOf(delay));
		}
	}

	public void setDebuggingText(String text) {
		consoleTextArea.setText(text);
	}

	private JButton createDialogButton(String label, final JComponent p) {
		JButton but = new JButton(label);
		but.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JDialog dialog = new JDialog();
				Dimension panelDimension = p.getPreferredSize();
				dialog.setSize((int) panelDimension.getWidth() + 20,
						(int) panelDimension.getHeight() + 20);
				dialog.setModal(false);
				dialog.add(p);
				dialog.setVisible(true);
			}
		});
		return but;
	}
	
	
	private JPanel createCacheModelPanel(String cacheName , final CacheModel cm){
		JPanel titlePanel = new JPanel();
		titlePanel.add(new JLabel(cacheName));
		JButton bt = new JButton(Messages.getText("stat"));
		titlePanel.add(bt);
		final JPanel resPanel = new DefaultTablePanel(titlePanel, cm);
		bt.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				JTextArea ta = new JTextArea(5,20);
				ta.append("PUT: "+cm.getNumPutEntries()+ "\n");
				ta.append("ACCESSED: "+cm.getNumAccessedEntries()+ "\n");
				ta.append("REMOVED: "+cm.getNumEvictedEntries()+ "\n");
				JOptionPane.showMessageDialog(resPanel, ta);
			}});
	
		return resPanel;
	}
	
	

}
