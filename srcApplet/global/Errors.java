package global;

import java.util.ArrayList;
import java.util.List;

public class Errors {

	private List<String> errors = new ArrayList<String>();
	
	public void addError(String message){
		errors.add(message);
	}
	
	public String displayErrors(){
		StringBuffer sb = new StringBuffer();
		for(int i = 0 ; i<errors.size() ; i++){
			sb.append(errors.get(i));
			sb.append("\n");
		}
		return sb.toString(); 
	}
	
	public boolean isEmpty(){
		return errors.isEmpty();
	}
}
