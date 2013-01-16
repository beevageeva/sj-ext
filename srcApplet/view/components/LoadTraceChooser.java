package view.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

public class LoadTraceChooser extends JDialog implements ActionListener , WindowListener{

	
		private static final long serialVersionUID = 1L;
		private static int selectedIndex1, selectedIndex2;
		private JList list1, list2;
		private DefTextField inputTF;
		private RadioGroupPanel useSecondListRG;


		public  LoadTraceChooser(Frame frame, String labelText1, String labelText2,
				String title, Object[] data1, Object[] data2, String okLabel, String inputTFLabel, String useSecondListRGLabel) {
			super(frame, title, true);
			addWindowListener(this);
			// Create and initialize the buttons.
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("Cancel");
			cancelButton.addActionListener(this);
			//
			final JButton setButton = new JButton(okLabel);
			setButton.addActionListener(this);
			getRootPane().setDefaultButton(setButton);
			list1 = createJList(data1);
			list2 = createJList(data2);
			JPanel panelList1 = createListPanel(list1, labelText1);
			JPanel panelList2 = createListPanel(list2, labelText2);

			// Lay out the buttons from left to right.
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
			buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
			buttonPane.add(Box.createHorizontalGlue());
			buttonPane.add(cancelButton);
			buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPane.add(setButton);

			// Put everything together, using the content pane's BorderLayout.
			Container contentPane = getContentPane();
			inputTF = new DefTextField(2);
			contentPane.add(Graph.createPanel(inputTF, inputTFLabel, null), BorderLayout.NORTH);
			
			//create mid panel
			JPanel listsPanel = new JPanel();
			listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.Y_AXIS));
			listsPanel.add(panelList1);
			useSecondListRG = new RadioGroupPanel(useSecondListRGLabel,new String[]{"yes", "no"}, new short[]{0,1},0, true);
			useSecondListRG.addActionListener((short)0, new ActionListener(){

				public void actionPerformed(ActionEvent e) {
					list2.setEnabled(true);
				}});
			useSecondListRG.addActionListener((short)1, new ActionListener(){

				public void actionPerformed(ActionEvent e) {
					list2.setEnabled(false);
				}});
			
			listsPanel.add(useSecondListRG);
			listsPanel.add(panelList2);
			contentPane.add(listsPanel, BorderLayout.CENTER);
			contentPane.add(buttonPane, BorderLayout.PAGE_END);

			pack();
		}

		
	   public int getSelectedIndex1(){
		   return selectedIndex1;
	   }

	   public int getSelectedIndex2(){
		   return selectedIndex2;
	   }

		// Handle clicks on the Set and Cancel buttons.
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
			if (!"cancel".equalsIgnoreCase(e.getActionCommand())) {
				selectedIndex1 = list1.getSelectedIndex();
				if(useSecondListRG.getValue()==0){
					selectedIndex2 = list2.getSelectedIndex();
				}
				else{
					selectedIndex2 = -1;
				}
			} else {
				selectedIndex1 = -1;
				selectedIndex2 = -1;
			}
		}

		public void windowOpened(WindowEvent arg0) {
		}

		public void windowClosing(WindowEvent arg0) {
			selectedIndex1 = -1;
			selectedIndex2 = -1;
			
		}

		public void windowClosed(WindowEvent arg0) {
		}

		public void windowIconified(WindowEvent arg0) {
		}

		public void windowDeiconified(WindowEvent arg0) {
		}

		public void windowActivated(WindowEvent arg0) {
		}

		public void windowDeactivated(WindowEvent arg0) {
		}
		
		
		private JPanel createListPanel(JList list, String labelText){
			// main part of the dialog
			JScrollPane listScroller = new JScrollPane(list);
			listScroller.setPreferredSize(new Dimension(250, 80));
			listScroller.setAlignmentX(LEFT_ALIGNMENT);

			// Create a container so that we can add a title around
			// the scroll pane. Can't add a title directly to the
			// scroll pane because its background would be white.
			// Lay out the label and scroll pane from top to bottom.
			JPanel listPane = new JPanel();
			listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
			JLabel label = new JLabel(labelText);
			label.setLabelFor(list);
			listPane.add(label);
			listPane.add(Box.createRigidArea(new Dimension(0, 5)));
			listPane.add(listScroller);
			listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			return listPane;


		}
		
		private JList createJList(Object[] data){
			JList list = new JList(data) {
				private static final long serialVersionUID = 1L;

				// Subclass JList to workaround bug 4832765, which can cause the
				// scroll pane to not let the user easily scroll up to the beginning
				// of the list. An alternative would be to set the unitIncrement
				// of the JScrollBar to a fixed value. You wouldn't get the nice
				// aligned scrolling, but it should work.
				public int getScrollableUnitIncrement(Rectangle visibleRect,
						int orientation, int direction) {
					int row;
					if (orientation == SwingConstants.VERTICAL && direction < 0
							&& (row = getFirstVisibleIndex()) != -1) {
						Rectangle r = getCellBounds(row, row);
						if ((r.y == visibleRect.y) && (row != 0)) {
							Point loc = r.getLocation();
							loc.y--;
							int prevIndex = locationToIndex(loc);
							Rectangle prevR = getCellBounds(prevIndex, prevIndex);

							if (prevR == null || prevR.y >= r.y) {
								return 0;
							}
							return prevR.height;
						}
					}
					return super.getScrollableUnitIncrement(visibleRect,
							orientation, direction);
				}
			};

			list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			list.setVisibleRowCount(-1);
			return list;
		}

}
