package config.view;

import global.Errors;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

import view.components.DefTextField;
import view.components.Graph;
import view.components.RadioGroupPanel;

import config.model.CacheBlockCfg;
import config.model.CacheConfigPT;
import constants.ConstantsValues;
import messages.Messages;

public class CacheConfigPanelPT extends JPanel implements ConfigurationElement{

	protected CacheConfigPT config; 

	protected RadioGroupPanel evictionPolicyRG = Graph.createEvictionPolicyRG(true);

	protected DefTextField numberSetsNBitsTF = new DefTextField(2);

	
	protected JTextField nameTF = new JTextField(10);

	protected RadioGroupPanel isDataInstrSepRG = new RadioGroupPanel(Messages
			.getText("data_instr_sep"), new String[] { "yes", "no" },
			new short[] { 0, 1 }, 0, true);


	protected RadioGroupPanel hasVictimCacheSepRG = new RadioGroupPanel(Messages
			.getText("has_victim_cache"), new String[] { "yes", "no" },
			new short[] { 0, 1 }, 0, true);
	
	protected CacheBlockCfgPanel victimCachePanel;
	
	protected CacheBlockCfgPanel cacheCfgPanel;
	
	
	CacheConfigPT getCacheConfigPT(){
		return config;
	}
	
	
	public CacheConfigPanelPT(CacheConfigPT cacheConfig){
		super();
		setPreferredSize(new Dimension(800,700));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.config = cacheConfig;
		cacheCfgPanel = new CacheBlockCfgPanel(null, cacheConfig);
		victimCachePanel = new CacheBlockCfgPanel(Messages.getText("victim_cache"),config.victimCacheCfg);
		add(Graph.createPanel(nameTF, Messages.getText("cache_name"), null));
		add(isDataInstrSepRG);
		isDataInstrSepRG.addActionListener((short)0, new ActionListener(){
			
			public void actionPerformed(ActionEvent e) {
				cacheCfgPanel.setDataInstrSep(true);
				if(hasVictimCacheSepRG.getValue()==0){
					victimCachePanel.setDataInstrSep(true);
				}
				
			}});
		isDataInstrSepRG.addActionListener((short)1, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				cacheCfgPanel.setDataInstrSep(false);
				victimCachePanel.setDataInstrSep(false);
			}});
		add(cacheCfgPanel);
		add(Graph.createPanel(numberSetsNBitsTF, Messages.getText("number_sets")+" 2**", "B"));
		add(hasVictimCacheSepRG);
		hasVictimCacheSepRG.addActionListener((short)0, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				victimCachePanel.enableFields(true, isDataInstrSepRG.getValue() == 0);
			}});
		hasVictimCacheSepRG.addActionListener((short)1, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				victimCachePanel.enableFields(false, isDataInstrSepRG.getValue() == 0);
				
			}});
		add(victimCachePanel);
		add(evictionPolicyRG);
		
		nameTF.setText(config.getName());
		victimCachePanel.enableFields(config.hasVictimCache(), config.isDataInstrSeparated());
		hasVictimCacheSepRG.setSelectedValue((short) (config.hasVictimCache()?0:1));
		cacheCfgPanel.setDataInstrSep(config.isDataInstrSeparated());
		isDataInstrSepRG.setSelectedValue((short) (config.isDataInstrSeparated()?0:1));
		numberSetsNBitsTF.setText(String.valueOf(config.getNumberSetsNBits()));
		evictionPolicyRG.setSelectedValue(config .getEvictionPolicy());
		
	}

	public void saveFields() {
		cacheCfgPanel.saveFields(config, isDataInstrSepRG.getValue()==0);
		config.setName(nameTF.getText());
		if(hasVictimCacheSepRG.getValue()==0){
			config.victimCacheCfg = new CacheBlockCfg();
			victimCachePanel.saveFields(config.victimCacheCfg, isDataInstrSepRG.getValue() == 0);
		}
		else{
			config.victimCacheCfg = null;
		}
		config.setEvictionPolicy(evictionPolicyRG.getValue());
		config.setNumberSetsNBits(Integer.parseInt(numberSetsNBitsTF.getText()));
		
	}

	public void validateFields(Errors errors) {
		numberSetsNBitsTF.validateField(Messages.getText("number_sets_num_bits"), errors, 0, cacheCfgPanel.getMinNumberEntries(errors, isDataInstrSepRG.getValue() ==0));
		if(nameTF.getText().length()==0 || nameTF.getText().length()>10 ){
			errors.addError("name must have 1-10 chars");
		}
		if(hasVictimCacheSepRG.getValue()==0){
			victimCachePanel.validateFields(errors, isDataInstrSepRG.getValue()==0);
		}

	}


}
