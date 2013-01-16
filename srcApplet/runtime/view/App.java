package runtime.view;

import java.awt.Dimension;
import java.awt.Menu;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JApplet;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import runtime.model.RuntimeModel;
import util.MainFrame;

import config.model.ConfigurationPanelListener;
import config.view.ConfigurationPanel;

public class App extends JApplet implements ConfigurationPanelListener, ActionListener{
	
	private JPanel contentPanel;

	
	@Override
	public void destroy() {
		super.destroy();
		System.exit(0);
	}

	@Override
	public void init() {
		int inset = 50;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height
				- inset * 2);
		
		contentPanel = new JPanel();
		ConfigurationPanel configPanel = new ConfigurationPanel();
		configPanel.addConfigurationPanelListener(this);
		setContentPane(contentPanel);
		contentPanel.add(configPanel);
		createMenuBar();
	}

	@Override
	public void start() {
	}

	public void configurationValidated() {
		MainPanel mainPanel = new MainPanel();
		contentPanel.removeAll();
		contentPanel.add(mainPanel);
		contentPanel.repaint();
		contentPanel.updateUI();
		
	}
	
	private void reconfig(){
		ConfigurationPanel configPanel = new ConfigurationPanel();
		configPanel.addConfigurationPanelListener(this);
		RuntimeModel.reset();
		configPanel.updateFromConfiguration();
		contentPanel.removeAll();
		contentPanel.add(configPanel);
		contentPanel.repaint();
		contentPanel.updateUI();
	}

	private void  createMenuBar(){
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Main");
		JMenuItem item = new JMenuItem("Reconfig");
		item.setActionCommand("reconfig");
		item.addActionListener(this);
		menu.add(item);
		item = new JMenuItem("Quit");
		item.setActionCommand("quit");
		item.addActionListener(this);
		menu.add(item);
		menuBar.add(menu);
		setJMenuBar(menuBar);
	}

	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if("reconfig".equals(actionCommand)){
			reconfig();
		}
		else if("quit".equals(actionCommand)){
			System.exit(0);
		}
	}
	
	   //
    // The following allows App to be run as an application
    // as well as an applet
    //
    public static void main(String[] args) {
    	new MainFrame(new App(), 800, 600);
    }

	
}
