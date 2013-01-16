package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class AppProperties {

	private Properties properties;
	private static AppProperties instance;
	
	public AppProperties(){
		properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream("app.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static AppProperties getInstance(){
		if(instance == null){
			instance = new AppProperties();
		}
		return instance;
	}
	
	
	
	public String getProperty(String propertyName){
		try{
			return properties.getProperty(propertyName);
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	
}
