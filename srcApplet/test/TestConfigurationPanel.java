package test;

import javax.swing.JFrame;

import config.view.ConfigurationPanel;

public class TestConfigurationPanel {

	public static void main(String[] s){
		JFrame frame = new JFrame();
		frame.setSize(800,600);
		frame.add(new ConfigurationPanel());
		frame.pack();
		frame.setVisible(true);
	}
	
}
