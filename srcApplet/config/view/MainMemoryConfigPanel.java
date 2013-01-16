package config.view;

import global.Errors;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import view.components.DefTextField;
import view.components.Graph;
import view.components.RadioGroupPanel;

import messages.Messages;

import config.model.MainMemCacheConfig;
import config.model.MainMemoryAllocConfig;
import config.model.PageAgingConfig;


public class MainMemoryConfigPanel extends JPanel implements ConfigurationElement{

	private DefTextField numberEntriesNBitsTF = new DefTextField(2);

	private RadioGroupPanel evictionPolicyRG = Graph.createEvictionPolicyRG(true);

	private DefTextField accessTimeUnitsTF = new DefTextField(2);
	
	private DefTextField busSizeTF = new DefTextField(2);
	
	private PageAgingPanel pageAgingPanel;
	
	private MainMemoryAllocPanel mainMemoryAllocPanel;
	
	private MainMemCacheConfig mainMemCacheConfig;
	
	public MainMemoryConfigPanel(MainMemCacheConfig mainMemCacheConfig) {
		super();
		setSize(800,600);
		this.mainMemCacheConfig = mainMemCacheConfig;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Graph.createPanel(numberEntriesNBitsTF, Messages.getText("number_pages")+" 2** ","B" ));
		add(evictionPolicyRG);
		add(Graph.createPanel(busSizeTF, Messages.getText("bus_size"),"B" ));
		add(Graph.createPanel(accessTimeUnitsTF, Messages.getText("access_time_units"),"T.U." ));
		numberEntriesNBitsTF.setText(String.valueOf(mainMemCacheConfig.getNumberEntriesNBits()));
		busSizeTF.setText(String.valueOf(mainMemCacheConfig.getBusSize()));
		accessTimeUnitsTF.setText(String.valueOf(mainMemCacheConfig.getAccessTimeUnits()));
		evictionPolicyRG.setSelectedValue(mainMemCacheConfig.getEvictionPolicy());
		mainMemoryAllocPanel = new MainMemoryAllocPanel();
		pageAgingPanel = new PageAgingPanel();
		add(pageAgingPanel);
		add(mainMemoryAllocPanel);
		
	}
	
	
	public void saveFields() {
		mainMemCacheConfig.setNumberEntriesNBits(Integer.parseInt(numberEntriesNBitsTF.getText()));
		mainMemCacheConfig.setBusSize(Integer.parseInt(busSizeTF.getText()));
		mainMemCacheConfig.setEvictionPolicy(evictionPolicyRG.getValue());
		mainMemCacheConfig.setAccessTimeUnits(Integer.parseInt(accessTimeUnitsTF.getText()));
		pageAgingPanel.saveFields();
		mainMemoryAllocPanel.saveFields();
	}

	public void validateFields(Errors errors) {
		numberEntriesNBitsTF.validateField(Messages.getText("number_pages_num_bits"), errors, 1, 32);
		busSizeTF.validateField(Messages.getText("bus_size"), errors, 1, 32);
		accessTimeUnitsTF.validateField(Messages.getText("access_time_units"), errors, 0, 50000);
		pageAgingPanel.validateFields(errors);
		mainMemoryAllocPanel.validateFields(errors);
	}
	
	class PageAgingPanel extends JPanel implements ConfigurationElement{
		
		private static final long serialVersionUID = 1L;
		private DefTextField pageAgingIncTF;
		private DefTextField memRefToBeRunTF;
		private RadioGroupPanel enabled;
	
		
		public PageAgingPanel(){
			super();
			setLayout(new BorderLayout());
			add(new JLabel("page aging") , BorderLayout.NORTH);
			pageAgingIncTF = new DefTextField(2);
			memRefToBeRunTF = new DefTextField(2);
			enabled = new RadioGroupPanel("enabled",
					new String[] { "yes", "no" }, new short[] { 0, 1 }, 0 ,true);
			enabled.addActionListener((short)0, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					enableFields(true);
				}
			});
			enabled.addActionListener((short)1, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					enableFields(false);
				}
			});
			
			JPanel compPanel = new JPanel(new GridLayout(3 , 1)); 
			compPanel.add(enabled);
			compPanel.add(Graph.createPanel(pageAgingIncTF , "ref inc units" , null));
			compPanel.add(Graph.createPanel(memRefToBeRunTF , "will run after" , "mem ref"));
			add(compPanel,  BorderLayout.CENTER);
			if(mainMemCacheConfig.hasPageAgingConfig()){
				enabled.setSelectedValue((short) 0);
				enableFields(true);
				pageAgingIncTF.setText(String.valueOf(mainMemCacheConfig.pageAgingConfig.getPageAgingIncrease()));
				memRefToBeRunTF.setText(String.valueOf(mainMemCacheConfig.pageAgingConfig.getMemRefToBeRun()));
			}
			else{
				enabled.setSelectedValue((short) 1);
				enableFields(false);
			}

		}
		
		public void saveFields() {
			if(enabled.getValue()==0){
				mainMemCacheConfig.pageAgingConfig = new PageAgingConfig();
				mainMemCacheConfig.pageAgingConfig.setPageAgingIncrease(Integer.parseInt(pageAgingIncTF.getText()));
				mainMemCacheConfig.pageAgingConfig.setMemRefToBeRun(Integer.parseInt(memRefToBeRunTF.getText()));
			}
			else{
				mainMemCacheConfig.pageAgingConfig = null;
			}
			
		}


		public void validateFields(Errors err) {
			if(enabled.getValue()==0){
				pageAgingIncTF.validateField("page aging ref inc units" , err , 1 , 20);
				memRefToBeRunTF.validateField("mem ref" , err , 1 , 50);
				
			}
		}
		
		private void enableFields(boolean b){
			pageAgingIncTF.setEnabled(b);
			memRefToBeRunTF.setEnabled(b);
		}

		
	}

	
	class MainMemoryAllocPanel extends JPanel implements ConfigurationElement{
		private static final long serialVersionUID = 1L;
		private DefTextField minPFFTF;
		private DefTextField maxPFFTF;
		private DefTextField evNodesToBeRunTF;
		
		private RadioGroupPanel enabled;
		
		public MainMemoryAllocPanel(){
			super();
			setLayout(new BorderLayout());
			add(new JLabel("mem alloc") , BorderLayout.NORTH);
			minPFFTF = new DefTextField(2);
			maxPFFTF = new DefTextField(2);
			evNodesToBeRunTF = new DefTextField(2);
			enabled = new RadioGroupPanel("alloc policy",
					new String[] { "local", "global" }, new short[] { 0, 1 }, 0 ,true);
			enabled.addActionListener((short)0, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					enableFields(true);
				}
			});
			enabled.addActionListener((short)1, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					enableFields(false);
				}
			});
			
			JPanel compPanel = new JPanel(new GridLayout(4 , 1)); 
			compPanel.add(enabled);
			compPanel.add(Graph.createPanel(minPFFTF , "min PFF" , null));
			compPanel.add(Graph.createPanel(maxPFFTF , "max PFF" , null));
			compPanel.add(Graph.createPanel(evNodesToBeRunTF , "will run after" , "ev nodes"));
			add(compPanel,  BorderLayout.CENTER);
			if(mainMemCacheConfig.hasMainMemoryAllocConfig()){
				enabled.setSelectedValue((short)0);
				enableFields(true);
				minPFFTF.setText(String.valueOf(mainMemCacheConfig.mainMemoryAllocConfig.getMinPFF()));
				maxPFFTF.setText(String.valueOf(mainMemCacheConfig.mainMemoryAllocConfig.getMaxPFF()));
				evNodesToBeRunTF.setText(String.valueOf(mainMemCacheConfig.mainMemoryAllocConfig.getNEvictedNodesToRun()));
			}
			else{
				enabled.setSelectedValue((short)1);
				enableFields(false);
			}

		}
		
		private void enableFields(boolean b){
			minPFFTF.setEnabled(b);
			maxPFFTF.setEnabled(b);
			evNodesToBeRunTF.setEnabled(b);
		}
		
		public void saveFields() {
			if(enabled.getValue() ==0){
				mainMemCacheConfig.mainMemoryAllocConfig =  new MainMemoryAllocConfig();
				mainMemCacheConfig.mainMemoryAllocConfig.setMinPFF(Integer.parseInt(minPFFTF.getText()));
				mainMemCacheConfig.mainMemoryAllocConfig.setMaxPFF(Integer.parseInt(maxPFFTF.getText()));
				mainMemCacheConfig.mainMemoryAllocConfig.setNEvictedNodesToRun(Integer.parseInt(evNodesToBeRunTF.getText()));
			}
			else{
				mainMemCacheConfig.mainMemoryAllocConfig = null;
			}
			
		}

		public void validateFields(Errors err) {
			if(enabled.getValue()==0){
				if(minPFFTF.validateField("min PFF" , err , 1 , 50)){
					maxPFFTF.validateField("max PFF" , err , Integer.parseInt(minPFFTF.getText()) , 50);
				}
				evNodesToBeRunTF.validateField("n ev nodes" , err , 1 , 20);
			}
		}
	}

	


}
