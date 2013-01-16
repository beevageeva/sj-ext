package view.components;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import messages.Messages;


public class ServerChooseList {

	protected Component parent;

	protected String urlString;
	
	protected String[] listValues;

	public ServerChooseList(Component parent, String urlString) {
		this.parent = parent;
		this.urlString = urlString;
	}

	public void showDialog() {

			listValues = null;
			try{
				listValues = getListValues();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(listValues==null){
				JOptionPane.showMessageDialog(parent, Messages.getText("list_from_server_not_available"));
			}
			else{
				ListDialog.showDialog(parent, null,  Messages.getText("choose_server_file"),
					 Messages.getText("choose_server_file"), listValues, -1, null , "Ok");
			}
	}
	
	public String getName(){
		int sIndex = ListDialog.getSelectedIndex();
		if(sIndex!=-1 && listValues!=null){
			return listValues[sIndex];
		}
		return null;
	}

	protected String[] getListValues() throws MalformedURLException, IOException {
		InputStream is = new URL(urlString).openStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		ArrayList<String> val = new ArrayList<String>();
		while ((line = reader.readLine()) != null) {
			val.add(line);
		}
		is.close();
		
		return (String[]) val.toArray(new String[val.size()]);

	}

}
