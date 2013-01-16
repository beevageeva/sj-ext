package view.components;

import global.Errors;

public interface ListTFValidator {

	public void validateListTFValues(Errors err,int selectedIndex,int[] textFieldsValues);
	
}
