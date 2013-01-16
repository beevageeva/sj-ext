package messages;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import util.AppProperties;

public class Messages {

	private static String FMT_DELIM="#";
	
	private static ResourceBundle bundle = null; 
	
	
	private static ResourceBundle getBundle(){
		if(bundle ==null){
			String locale = AppProperties.getInstance().getProperty("locale");
			
			if(locale == null){
				locale = "en";
			}
			bundle = ResourceBundle.getBundle("messages.Messages", new Locale(locale));
		}
		return bundle;
	}
	
	static{
	}
	
	
	
	public static String getText(String text){
		String result = null;
		try{
			result = getBundle().getString(text); 
		}
		catch(MissingResourceException e){
			//System.out.println("there is no message assoc to "+text);
		}
		if(result == null){
			result = text+"_KEY";
		}
		return result;
	}
	
	public static String getFormattedText(String text , String[] replacement){
		String result = null;
		try{
			result = getBundle().getString(text);
		}
		catch(MissingResourceException e){
			System.out.println("there is no message assoc to "+text);
		}
		
		if(result !=null){
			for(int i = 0; i<replacement.length;i++){
				result = result.replaceAll(FMT_DELIM+String.valueOf(i), replacement[i]);
			}
		}
		else{
			result = text+"_KEY";
		}
		
		return result;
	}
	
}
