package view.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;


import constants.CacheCoherencePolicyType;
import constants.CacheEvictionPolicyType;

import messages.Messages;




public class Graph {

	public static JPanel createPanel(JTable table, int width , int height , Component title){
		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane sp = new JScrollPane(table);
		sp.setPreferredSize(new Dimension(width,height));
		panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		panel.add(sp, BorderLayout.CENTER);
		if(title!=null){
			panel.add(title, BorderLayout.NORTH);
		}
		return panel;
	}
	
	public static JPanel createPanel(JTable table, int width , int height , String title){
		return createPanel(table , width , height , title==null?null:new JLabel(title));
	}	
	
	public static RadioGroupPanel createEvictionPolicyRG(boolean isHoriz){
		return new RadioGroupPanel(Messages
			.getText("eviction_policy"), new String[] {
			Messages.getText("random_eviction_policy"),
			Messages.getText("fifo_eviction_policy"),
			Messages.getText("lfu_eviction_policy"),
			Messages.getText("lru_eviction_policy"),
			Messages.getText("mru_eviction_policy"),
			Messages.getText("nfu_eviction_policy"),
			Messages.getText("nru_eviction_policy"),
			Messages.getText("opt_eviction_policy") }, new short[] {
			CacheEvictionPolicyType.RANDOM_POLICY, CacheEvictionPolicyType.FIFO_POLICY,
			CacheEvictionPolicyType.LFU_POLICY, CacheEvictionPolicyType.LRU_POLICY,
			CacheEvictionPolicyType.MRU_POLICY, CacheEvictionPolicyType.NFU_POLICY,
			CacheEvictionPolicyType.NRU_POLICY, CacheEvictionPolicyType.OPT_POLICY },
			CacheEvictionPolicyType.RANDOM_POLICY, isHoriz);
	}

	public static RadioGroupPanel createCacheCoherencePolicyRG(String title,boolean isHoriz){
		return new RadioGroupPanel(title, new String[] {
			Messages.getText("msi_cache_coherence_policy"),
			Messages.getText("mosi_cache_coherence_policy"),
			Messages.getText("mesi_cache_coherence_policy"),
			Messages.getText("moesi_cache_coherence_policy") }, new short[] {
			CacheCoherencePolicyType.MSI, CacheCoherencePolicyType.MOSI,
			CacheCoherencePolicyType.MESI, 
			CacheCoherencePolicyType.MOESI },
			CacheCoherencePolicyType.MSI, isHoriz);
	}

	
	public static JPanel createPanel(JTextField tf, String label1 , String label2){
		JPanel panel = new JPanel();
				panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		panel.add(new JLabel(label1));
		panel.add(tf);
		if(label2!=null){
			panel.add(new JLabel(label2));
		}
		return panel;
	}
	
	
	
	
}
