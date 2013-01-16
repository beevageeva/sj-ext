package runtime.model;

import global.Helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import view.model.DirectPageTableListener;
import view.model.cache.EntryI;
import view.model.cache.EntryIB;
import view.model.interfaces.MovingIndexAbstractTableModel;
import config.model.Configuration;
import config.model.PageTableConfig.DirectMappedPageTableConfig;

public class DirectMappedPageTableModel implements PageTableModel {

	private List<DE>[] directoryTables;

	private List<PE> peTables;

	private Map<Integer, Integer> pidToTableMap = new HashMap<Integer, Integer>();
	
	public static final int MAX_NUMBER_PROCS = 10;
	
	private List<DirectPageTableListener> l = new ArrayList<DirectPageTableListener>();

	/*
	 * l(1), .. l(n) number bits used to split the virtual address for each
	 * level(levels are from 1..n-1 ; level(-1) is the root table) number page
	 * level(0) the process root, l(0) = 2**maxNumberOfProcesses table on each
	 * level = N(i) = 2**(l(0)+l(1) + l(2) + .. + l(i)) for i = 0..n-1 , N(-1) =
	 * 1, each block of size 2**l(i+1) * sizeOfPageTableEntry
	 * (virtualAddressNBits/8) in the case of bottom up search method to calc
	 * the virtual address of block m of level k (assuming that the memory is
	 * continuously allocated for the page tables) T: va =
	 * BasePTLevelk|m|0(sizeOfPageTableEntryNBits) ; put restriction for interm
	 * page tables that have the size of a page and then take the vpn of the
	 * page table S: search for vpn = N(-1)+ + N(1) + .. N(k-1) + m ; take care
	 * that trace does not contain generated va with vpn in [0..(N(0) + N(1) +
	 * ...N(n-1))] (BasePTlevelk is usually kept in a register ,can be calc:
	 * N(-1) * rootPageTableSize + (N(0)+N(1)+ .. N(k-1))* pageSize)
	 * 
	 */

	@SuppressWarnings("unchecked")
	public DirectMappedPageTableModel() {
		// init tables
		int nOffsets = ((DirectMappedPageTableConfig) Configuration
				.getInstance().pageTableConfig.getMappingConfig())
				.getOffsetsLength().length;
		directoryTables = new ArrayList[nOffsets - 1];
		peTables = new ArrayList<PE>();
		for (int i = 0; i < nOffsets - 1; i++) {
			directoryTables[i] = new ArrayList<DE>();
		}
	}

	private int[] getOffsets(int vPN) {
		int vpnNBits = Configuration.getInstance().virtualAddrNBits
				- Configuration.getInstance().pageSizeNBits;
		int[] offsetsLength = ((DirectMappedPageTableConfig) Configuration
				.getInstance().pageTableConfig.getMappingConfig())
				.getOffsetsLength();
		String vpnBinary = Helper.convertDecimalToBinary(vPN, vpnNBits);
		int index = 0;
		int len;
		String nBin;
		int[] offsets = new int[offsetsLength.length];
		for (int i = 0; i < offsetsLength.length; i++) {
			len = offsetsLength[i];
			nBin = vpnBinary.substring(index, index + len);
			offsets[i] = Integer.parseInt(nBin, 2);
			index += len;
		}
		return offsets;
	}

	private P2 getPhysicalPageNumberFromPageBU(int smpNodeIndex, int numCPU,
			int pid, int vPN, short instrType, int offset) {
		int number = vPN;
		int ptVPN;
		int tlbValue;
		Stack<Integer> ptVPNs = new Stack<Integer>();
		int[] offsetsLength = ((DirectMappedPageTableConfig) Configuration
				.getInstance().pageTableConfig.getMappingConfig())
				.getOffsetsLength();
		int[] offsets = getOffsets((int) (vPN % Math.pow(2, Configuration
				.getInstance().virtualAddrNBits
				- Configuration.getInstance().pageSizeNBits)));
		int[] B = new int[offsetsLength.length];
		int[] N = new int[offsetsLength.length];
		N[0] = MAX_NUMBER_PROCS;
		B[0] = pid;
		for (int i = 0; i < offsetsLength.length - 1 ; i++) {
			B[i + 1] = (int) (Math.pow(2, offsetsLength[i]) * B[i])
					+ offsets[i];
			N[i + 1] = (int) (N[i] * Math.pow(2, offsetsLength[i + 1]));
		}
		P2 p2;
		for (int k = offsetsLength.length - 1; k >= 0; k--) {
			// ptVPN = getPTVirtualPageNumber(k, number);
			ptVPN = 1;
			for (int i = 0; i < k; i++) {
				ptVPN += N[i];
			}
			ptVPN += B[k];
			ptVPNs.push(new Integer(ptVPN));
			p2 = RuntimeModel.getInstance().sched.searchInPTCaches(
					smpNodeIndex, numCPU, ptVPN);
			if ((tlbValue = p2.y) != -1) {
				return getPhysicalPageNumberFromPageTD(smpNodeIndex, numCPU,
						tlbValue, k + 1, pid, vPN, instrType, ptVPNs, offset);
			}
			number /= Math.pow(2, offsetsLength[k]);
		}
		return getPhysicalPageNumberFromPageTD(smpNodeIndex, numCPU, 0, 0, pid,
				vPN, instrType, ptVPNs, offset);
	}

	private P2 getPhysicalPageNumberFromPageTD(int smpNodeIndex, int numCPU,
			int indexPageTable, int startLevel, int pid, int vPN,
			short instrType, Stack<Integer> ptVPNs, int offset) {
		ProcessMgrImpl pmgr = RuntimeModel.getInstance().procMgr;
		StrLogger sl = RuntimeModel.getInstance().logger;

		int[] offsetsLength = ((DirectMappedPageTableConfig) Configuration
				.getInstance().pageTableConfig.getMappingConfig())
				.getOffsetsLength();
		int vpnNBits = Configuration.getInstance().virtualAddrNBits
				- Configuration.getInstance().pageSizeNBits;
		int lastNumber = indexPageTable;
		DE model;
		EntryI entry;
		int[] offsets = getOffsets((int) (vPN % Math.pow(2, vpnNBits)));
		int n = 0;
		for (int i = startLevel; i < offsetsLength.length - 1; i++) {
			n = offsets[i];
			model = directoryTables[i].get(lastNumber);
			entry = model.getEntry(n);
			if (entry == null) {
				model.newEntry(n);
				lastNumber = i==offsetsLength.length - 2?peTables.size(): directoryTables[i+1].size();
				model.getEntry(n).p = lastNumber;
				createTable(i+1);
			} else {
				lastNumber = entry.p;
			}
			model.fireIndexMoved(n);

			if (ptVPNs != null) {
				RuntimeModel.getInstance().sched.putInPTCaches(smpNodeIndex,
						numCPU, Configuration.getInstance().smpNodeConfigs
								.get(smpNodeIndex).cpuCachesToPT.get(numCPU).caches.size() - 1,
						ptVPNs.pop().intValue(), lastNumber, pid, instrType);
			}
		}
		if (ptVPNs != null) {
			RuntimeModel.getInstance().sched.putInPTCaches(smpNodeIndex,
					numCPU, Configuration.getInstance().smpNodeConfigs
							.get(smpNodeIndex).cpuCachesToPT.get(numCPU).caches.size() - 1,
					ptVPNs.pop().intValue(), lastNumber, pid, instrType);
		}
		
		PE pe = peTables.get(lastNumber);
		EntryIB entryib = pe.getEntry(n);
		P2 p2 = new P2();
		if (entryib == null) {
			//from disk ft
			pe.newEntry(n);
			entryib = pe.getEntry(n);
			int localRPN = RuntimeModel.getInstance().smpNodeModels
					.get(smpNodeIndex).mainMemModel.putCacheEntry(pid,
					instrType, vPN);
			
			entryib.p = Configuration.getInstance().getRVPageNumber(smpNodeIndex, localRPN);
			sl.addMessage(smpNodeIndex, numCPU, "local rpn: "+localRPN +" -> global RPN: "+entryib.p);
			entryib.b = true;
			pmgr.putActualExecProcessWait(smpNodeIndex, numCPU,
					Configuration.getInstance().smpNodeConfigs
							.get(smpNodeIndex).diskAccessTime, calculateRA(Helper.getPowerOf2(Configuration.getInstance().getMemTotalNumberOfPages()), entryib.p, offset));
			p2.y = -2;

		} else if (!entryib.b) {
			//swap
			pmgr.putActualExecProcessWait(smpNodeIndex, numCPU,
					Configuration.getInstance().smpNodeConfigs
							.get(smpNodeIndex).diskAccessTime, calculateRA(Helper.getPowerOf2(Configuration.getInstance().getMemTotalNumberOfPages()), entryib.p, offset));
			entryib.b = true;
			p2.y = -2;

		}

		pe.fireIndexMoved(n);
		p2.x = entryib.p;
		return p2;

	}
	
	private String calculateRA(int memNumberEntriesNBits, int rpn , int offset){
		String ra = Helper.convertDecimalToBinary(rpn, memNumberEntriesNBits);
		ra+=Helper.convertDecimalToBinary(offset, Configuration.getInstance().pageSizeNBits);
		return ra;
	}

	private void createTable(int level) {
		int[] offsetsLength = ((DirectMappedPageTableConfig) Configuration
				.getInstance().pageTableConfig.getMappingConfig())
				.getOffsetsLength();
		MovingIndexAbstractTableModel model;
		if (level < offsetsLength.length - 1) {
			model = new DE((int) Math.pow(2, offsetsLength[level]));
			directoryTables[level].add((DE)model);

		} else {
			model = new PE((int) Math.pow(2, offsetsLength[level]));
			peTables.add((PE)model);
		}
		for (int i = 0; i < l.size(); i++) {
			l.get(i).tableAdded(model, level);
		}

	}

	public P2 getPageNumber(int smpNodeIndex, int numCPU, int vPN, int offset, int pid,
			short instrType) {
		P2 p2;
		//check pid entry
		if(pidToTableMap.get(pid)==null){
			pidToTableMap.put(pid , directoryTables[0].size());
			createTable(0);
			
		}
		if (((DirectMappedPageTableConfig) Configuration.getInstance().pageTableConfig
				.getMappingConfig()).isSearchMethodTopDown()) {
			
			p2 = getPhysicalPageNumberFromPageTD(smpNodeIndex, numCPU, directoryTables[0].size() - 1, 0,
					pid, vPN, instrType, null, offset);
		} else {
			p2 = getPhysicalPageNumberFromPageBU(smpNodeIndex, numCPU, pid,
					vPN, instrType , offset);
		}
		return p2;
	}

	public void removeProcess(int pid) {
		//do nothing if the process has not already pages allocated 
		if(pidToTableMap.get(pid) == null){
			return;
		}
		List<Integer> toremoveIndices = new ArrayList<Integer>();
		List<Integer> newToremoveIndices;
		int index;
		toremoveIndices.add(pidToTableMap.get(pid));
		for(int j = 0;j<directoryTables.length;j++){
			newToremoveIndices = new ArrayList<Integer>();
			for(int i = 0;i<toremoveIndices.size();i++){
				index = toremoveIndices.get(i) - i;
				newToremoveIndices.addAll(directoryTables[j].get(index).getAllEntriesValues());
				directoryTables[j].remove(index); 
			}
			toremoveIndices = newToremoveIndices;
		}
		
	}

	public List<DE>[] getDirectoryTables() {
		return directoryTables;
	}

	public List<PE> getPeTables() {
		return peTables;
	}
	
	public void addDirectPageTableListener(DirectPageTableListener list) {
		l.add(list);
	}
	
	public MovingIndexAbstractTableModel getPidToTableTable(){
		
		return new MovingIndexAbstractTableModel(){

			public int getColumnCount() {
				return 2;
			}

			public int getRowCount() {
				return MAX_NUMBER_PROCS;
			}

			public Object getValueAt(int rowIndex, int columnIndex) {
				Integer n = pidToTableMap.get(rowIndex);
				if(n==null){
					return null;
				}
				if(columnIndex==0){
					return rowIndex;
				}
				else if(columnIndex==1){
					return n;
				}
				return null;
			}

			@Override
			public String getColumnName(int column) {
				if(column==0){
					return "pid";
				}
				else if(column==1){
					return "p";
				}
				return null;
			}
			
			
			
		};
		
		
	}

	@Override
	public void swapMemEntry(int rpn) {
		for(int i = 0;i<peTables.size();i++){
			if(peTables.get(i).swapMemEntry(rpn)){
				return;
			}
		}
	}

}
