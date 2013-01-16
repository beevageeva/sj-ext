package config.view;

import global.Errors;

public interface ConfigurationElement {

	public void validateFields(Errors errors);
	public void saveFields();
	
	
}
