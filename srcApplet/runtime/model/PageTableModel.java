package runtime.model;


public interface PageTableModel {

	public P2 getPageNumber(int smpNodeIndex, int numCPU,int vpn, int offset, int pid, short instrType);
	public void removeProcess(int pid);
	public void swapMemEntry(int rpn);
	
}
