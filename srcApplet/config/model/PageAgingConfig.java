package config.model;

public class PageAgingConfig {

	private int pageAgingIncrease ;
	private int memRefToBeRun = -1;
	
	
	public int getMemRefToBeRun() {
		return memRefToBeRun;
	}
	public void setMemRefToBeRun(int memRefToBeRun) {
		this.memRefToBeRun = memRefToBeRun;
	}
	public int getPageAgingIncrease() {
		return pageAgingIncrease;
	}
	public void setPageAgingIncrease(int pageAgingIncrease) {
		this.pageAgingIncrease = pageAgingIncrease;
	}
	
	public boolean isEnabled(){
		return memRefToBeRun!=-1;
	}
	
	public void disable(){
		memRefToBeRun = -1;
	}
	
}
