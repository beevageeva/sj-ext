package config.view;

import global.Errors;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

import view.components.DefTextField;
import view.components.Graph;
import view.components.RadioGroupPanel;

import config.model.CacheConfigMem;
import messages.Messages;

public class CacheConfigPanelMem extends CacheConfigPanelPT{

	
	private DefTextField blockSizeNBitsTF = new DefTextField(2);

	private DefTextField blockSizeInstrNBitsTF = new DefTextField(2);
	
	private DefTextField busSizeTF = new DefTextField(2);
	
	private RadioGroupPanel isWriteThroughRG = new RadioGroupPanel(Messages.getText("isWriteThrough"),new String[]{Messages.getText("yes"), Messages.getText("no")},new short[]{0,1},0,true); 

	private RadioGroupPanel isWriteAllocateRG = new RadioGroupPanel(Messages.getText("isWriteAllocate"),new String[]{Messages.getText("yes"), Messages.getText("no")},new short[]{0,1},0,true); 

		
	private CacheConfigMem cacheConfigMem;
	
	CacheConfigMem getCacheConfigMem(){
		return cacheConfigMem;
	}
	
	public CacheConfigPanelMem(CacheConfigMem cacheConfig){
		super(cacheConfig);
		this.cacheConfigMem = cacheConfig;
		add(Graph.createPanel(blockSizeNBitsTF, Messages.getText("block_size")+" 2**", "B"));
		add(Graph.createPanel(blockSizeInstrNBitsTF, Messages.getText("block_size_instr")+" 2**", "B"));
		
		isDataInstrSepRG.addActionListener((short) 0, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				blockSizeInstrNBitsTF.setEnabled(true);
			}});
		isDataInstrSepRG.addActionListener((short) 1, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				blockSizeInstrNBitsTF.setEnabled(false);

			}});
		add(Graph.createPanel(busSizeTF, Messages.getText("bus_size"), null));
		JPanel p = new JPanel();
		p.add(isWriteThroughRG);
		p.add(isWriteAllocateRG);
		add(p);
		blockSizeNBitsTF.setText(String.valueOf(cacheConfigMem.getBlockSizeNBits()[0]));
		
		
		if(cacheConfigMem.isDataInstrSeparated()){
			blockSizeInstrNBitsTF.setEnabled(true);
			blockSizeInstrNBitsTF.setText(String.valueOf(cacheConfigMem.getBlockSizeNBits()[1]));
		}
		else{
			blockSizeInstrNBitsTF.setEnabled(false);
		}
		busSizeTF.setText(String.valueOf(cacheConfigMem.getBusSize()));
		isWriteThroughRG.setSelectedValue((short)(cacheConfigMem.isWriteThrough?0:1));
		isWriteAllocateRG.setSelectedValue((short)(cacheConfigMem.isWriteAllocate?0:1));
		setVisible(true);
		
	}

	public void saveFields() {
		super.saveFields();
		cacheConfigMem.getBlockSizeNBits()[0] = Integer.parseInt(blockSizeNBitsTF.getText());
		
		if(cacheConfigMem.isDataInstrSeparated()){
			cacheConfigMem.getBlockSizeNBits()[1] = Integer.parseInt(blockSizeInstrNBitsTF.getText());
			
		}
		cacheConfigMem.setBusSize(Integer.parseInt(busSizeTF.getText()));
		cacheConfigMem.isWriteThrough = isWriteThroughRG.getValue()==0;
		cacheConfigMem.isWriteAllocate = isWriteAllocateRG.getValue()==0;
	}

	public void validateFields(Errors errors) {
		super.validateFields(errors);
		blockSizeNBitsTF.validateField(Messages.getText("block_size_num_bits"), errors, 1, 32);
		if(isDataInstrSepRG.getValue()==0){
			blockSizeInstrNBitsTF.validateField(Messages.getText("block_size_instr_num_bits"), errors, 1, 32);
		}
		busSizeTF.validateField(Messages.getText("bus_size"), errors, 1, 32);
	}
}
