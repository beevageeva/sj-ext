package runtime.model;

import java.util.List;

import messages.Messages;

import trace.Instr;

public class ThreadProcDesc {

	public static final short MAX_PRIORITY = 10;
	
	public static final short RUNNING_STATE = 0;
	public static final short WAITING_STATE = 1;
	public static final short READY_STATE = 2;
	
	public  List<Instr> instructions;

	public String name;
 	
	public int pid;
	
	public int timeUnits;
	
	public int timeUnitsLeft;
	
	public int currentInstrNumber;
	
	public short state;
	
	public int numberThread;
	
	//int from 0 - MAX_PRIORITY
	public int priority;

	public static String getStateString(short state) {
		switch(state){
		case RUNNING_STATE:
			return Messages.getText("process_running_state");
		case READY_STATE:
			return Messages.getText("process_ready_state");
		case WAITING_STATE:
			return Messages.getText("process_waiting_state");
		
		default: return Messages.getText("process_unknown_state"); 	
		}
		
		
	}

	
	
	@Override
	public int hashCode() {
		return (String.valueOf(pid)+"|"+String.valueOf(numberThread)).hashCode();
	}



	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ThreadProcDesc)){
			return false;
		}
		ThreadProcDesc tpd = (ThreadProcDesc)obj;
		return pid == tpd.pid && numberThread == tpd.numberThread;
	}
	
	

	
	
}
