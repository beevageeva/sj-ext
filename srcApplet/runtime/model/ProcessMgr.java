package runtime.model;

import java.util.List;

import trace.Instr;

public interface ProcessMgr {
	
	public void addNewProcesses(List<Instr> instructions, String processName, int numThreads, int timeUnits);
	public void removeProcess(int pid);
}
