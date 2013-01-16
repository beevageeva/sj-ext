package runtime.model;

import global.Helper;
import config.model.Configuration;
import config.model.PageTableConfig.InverseMappedPageTableConfig;
import view.model.cache.EntryIList;

public class InverseMappedPageTableModel implements PageTableModel {

	private IPE ipe;

	public InverseMappedPageTableModel() {
		int hashAnchorSizeNBits = ((InverseMappedPageTableConfig) Configuration
				.getInstance().pageTableConfig.getMappingConfig())
				.getHashAnchorSizeNBits();
		ipe = new IPE((int) Math.pow(2 , hashAnchorSizeNBits));
	}

	@Override
	public P2 getPageNumber(int smpNodeIndex, int numCPU, int vpn, int offset,
			int pid, short instrType) {
		int vpnNBits = Configuration.getInstance().virtualAddrNBits
				- Configuration.getInstance().pageSizeNBits;
		int hashAnchorSizeNBits = ((InverseMappedPageTableConfig) Configuration
				.getInstance().pageTableConfig.getMappingConfig())
				.getHashAnchorSizeNBits();
		String vpnBinary = Helper.convertDecimalToBinary(vpn, vpnNBits);
		int index = Integer.parseInt(vpnBinary.substring(vpnNBits
				- hashAnchorSizeNBits), 2);
		int rpn = index
				% Configuration.getInstance().getMemTotalNumberOfPages();
		StrLogger sl = RuntimeModel.getInstance().logger;

		EntryIList entry = ipe.getEntry(index);
		if (entry == null) {
			ipe.newEntry(index);
			entry = ipe.getEntry(index);
		}
		P2 p2 = new P2();
		p2.x = rpn;
		int indexInVpnArray = entry.getIndexInVPNArray(vpn, pid);
		if (indexInVpnArray != -1 && entry.indexList != -1){
			if(indexInVpnArray != entry.indexList ) {
			// vpn is allocated , but is in the swap file
			// is in the swap file ,bring it from diskin main memory and
			// set true inMemory
			// a node will be evicted , put a cache listener to set in
			// memory in the inverse table to false
			sl.addMessage(smpNodeIndex, numCPU, "page in swap mem , put vpn "
					+ entry.getVPNAtIndex(entry.indexList));
			
			p2.y = -1;
			}
		} else {
			// the vpn hasn't been yet allocated
			sl.addMessage(smpNodeIndex, numCPU, "page not alloc , put vpn at index "+index);
			entry.p = rpn;
			entry.addVPNInArray(vpn, pid);
			p2.y = -2;
		}
		if(entry.indexList!=-1){
			//swap the current page
			P2 p2swap =  Configuration.getInstance().getRLPageNumber(rpn);
			sl.addMessage(smpNodeIndex, numCPU, "must swap page "+p2swap.x+" from smp node = "+p2swap.y );
			RuntimeModel.getInstance().smpNodeModels.get(p2swap.y).mainMemModel
			.removeEntry(p2swap.x);
			
		}
		if(p2.y == -1 || p2.y ==-1){
			//put process in wait state
			String ra = Helper.convertDecimalToBinary(rpn, Helper.getPowerOf2(Configuration.getInstance().getMemTotalNumberOfPages()));
			ra+=Helper.convertDecimalToBinary(offset, Configuration.getInstance().pageSizeNBits);
			RuntimeModel.getInstance().procMgr.putActualExecProcessWait(smpNodeIndex, numCPU,
					Configuration.getInstance().smpNodeConfigs
					.get(smpNodeIndex).diskAccessTime,ra);

		}
		entry.indexList = indexInVpnArray;
		ipe.fireIndexMoved(index);
		return p2;
	}

	@Override
	public void removeProcess(int pid) {
		ipe.removeProcess(pid);
	}

	public IPE getIPE() {
		return ipe;
	}

	@Override
	public void swapMemEntry(int rpn) {
		int hashAnchorSize = (int) Math.pow(2,((InverseMappedPageTableConfig) Configuration
				.getInstance().pageTableConfig.getMappingConfig())
				.getHashAnchorSizeNBits());

		int hashIndex;
		int totMemNPages = Configuration.getInstance().getMemTotalNumberOfPages();
		int c = 0;
		while((hashIndex=totMemNPages * c + rpn)<hashAnchorSize){
			ipe.getEntry(hashIndex).indexList=-1;
			c++;
		}
		
	}

}
