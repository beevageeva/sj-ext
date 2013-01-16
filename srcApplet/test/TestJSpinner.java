package test;

import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class TestJSpinner extends JFrame {
	
	public TestJSpinner(){
		super();
		setSize(800,600);
		SpinnerNumberModel model = new SpinnerNumberModel(1,1,10,1);
		JSpinner ns = new JSpinner(model);
		ns.setVisible(true);
		getContentPane().add(ns);
	}
	
	public static void main(String[] s){
		TestJSpinner ap = new TestJSpinner();
		ap.pack();
		ap.setVisible(true);
	}


}
