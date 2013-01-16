package runtime.model;

import global.Helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import runtime.model.CacheDirectory.P2S1B1;
import runtime.model.ProcessMgrImpl.ExtInstr;
import trace.Instr;
import view.model.cache.CacheModel;
import view.model.cache.Entry;
import view.model.cache.EntryII;
import view.model.cache.PTCacheAddColumn;
import config.model.CacheConfigMem;
import config.model.CacheConfigPT;
import config.model.Configuration;
import config.model.IndexCacheBlockCfg;
import constants.CacheCoherencePolicyType;
import constants.InstructionType;

public class Sched {

	public static final int MIN_DELAY = 5;

	public static final int MAX_DELAY = 100;

	private volatile int delay = (MAX_DELAY - MIN_DELAY) / 2;

	public void setDelay(int delay) {
		this.delay = delay;
	}

	private int currentDuration;

	public int getCurrentDuration() {
		return currentDuration;
	}

	public Sched() {
	}

	public void prepare() {
		StrLogger sl = RuntimeModel.getInstance().logger;
		sl.reset();
		Instr instr;
		ProcessMgrImpl pmgr = RuntimeModel.getInstance().procMgr;
		ThreadProcDesc tpd;
		int lastDuration;
		for (int i = 0; i < Configuration.getInstance().smpNodeConfigs.size(); i++) {
			for (int j = 0; j < Configuration.getInstance().smpNodeConfigs
					.get(i).cpuCachesToMem.size(); j++) {
				currentDuration = 0;
				lastDuration = 0;
				tpd = pmgr.getCurExecProcess(i, j);
				while (currentDuration < delay) {
					instr = pmgr.nextInstruction(i, j, currentDuration
							- lastDuration);
					lastDuration = currentDuration;
					if (instr != null) {
						if (instr instanceof ExtInstr) {
							sl.addMessage(i, j,
									"\n***\nINT current instruction is "
											+ instr);
							int vpn = Integer
									.parseInt(
											instr.va
													.substring(
															0,
															Configuration
																	.getInstance().virtualAddrNBits
																	- Configuration
																			.getInstance().pageSizeNBits),
											2);
							//set all flags to Owned
							putInMemCaches(
									i,
									j,
									Configuration.getInstance().smpNodeConfigs
											.get(i).cpuCachesToMem.get(j).caches
											.size() - 1, tpd.pid,
									((ExtInstr) instr).ra, vpn, instr.type, calculateNewState(instr.type, CacheDirectory.STATE_SHARED));
						} else {
							sl.addMessage(i, j, "\n***\ncurrent instruction "
									+ instr);
							resolve(i, j, instr.va, tpd.pid, instr.type);
						}
					} else {
						// no more instr?
						break;
					}

				}
			}
		}
	}
	
	private static short calculateNewState(short instrType, short oldState){
		if(instrType==InstructionType.WRITE_DATA_TYPE){
			return CacheDirectory.STATE_MODIFIED;
		}
		switch(Configuration.getInstance().cacheMemCacheCoherencePolicy){
		case CacheCoherencePolicyType.MOESI:
			return oldState==CacheDirectory.STATE_EXCLUSIVE?CacheDirectory.STATE_OWNED:CacheDirectory.STATE_EXCLUSIVE;
		case CacheCoherencePolicyType.MOSI:
			return CacheDirectory.STATE_OWNED;
		case CacheCoherencePolicyType.MSI:
			return CacheDirectory.STATE_SHARED;
		case CacheCoherencePolicyType.MESI:
			return CacheDirectory.STATE_EXCLUSIVE;
		}
		return CacheDirectory.STATE_INVALID;
	}
	

	private void resolve(int smpNodeIndex, int numCPU, String va, int pid,
			short instrType) {

		CacheModel cm;
		StrLogger sl = RuntimeModel.getInstance().logger;
		List<Entry> entry;
		int rpn = -1, vpn, offset;
		int vpnNBits = Configuration.getInstance().virtualAddrNBits
				- Configuration.getInstance().pageSizeNBits;
		ProcessMgrImpl pmgr = RuntimeModel.getInstance().procMgr;
		vpn = Integer.parseInt(va.substring(0, vpnNBits), 2);
		offset = Integer.parseInt(va.substring(vpnNBits), 2);
		sl.addMessage(smpNodeIndex, numCPU, " vpn = " + vpn + "(B: "
				+ va.substring(0, vpnNBits) + ") offset = " + offset + "(B: "
				+ va.substring(vpnNBits) + ")");
		P2 p2 = searchInPTCaches(smpNodeIndex, numCPU, vpn);
		int indStartPut = p2.y;
		if (p2.x == -1) {
			// search PT
			currentDuration = sl
					.accessPT(smpNodeIndex, numCPU, currentDuration);
			P2 ptp2 = RuntimeModel.getInstance().pageTableModel.getPageNumber(
					smpNodeIndex, numCPU, vpn, offset, pid, instrType);
			rpn = ptp2.x;
			if (ptp2.y == -2) {

				// bring from disk to mem
				sl.addMessage(smpNodeIndex, numCPU, "page not allocated");

				// put in page table
				currentDuration = sl.accessPT(smpNodeIndex, numCPU,
						currentDuration);
				// put in pt caches
				putInPTCaches(smpNodeIndex, numCPU, indStartPut, vpn, rpn, pid,
						instrType);

				return;

			} else if (ptp2.y == -1) {
				// swap
				sl.addMessage(smpNodeIndex, numCPU,
						"page not in mm, get it from swap file");
				// put in pt caches
				putInPTCaches(smpNodeIndex, numCPU, indStartPut, vpn, rpn, pid,
						instrType);

				return;

			}
		} else {

			rpn = p2.x;
			// put in pt caches
			putInPTCaches(smpNodeIndex, numCPU, indStartPut, vpn, rpn, pid,
					instrType);

		}
		int nBitsRPN = Helper.getPowerOf2(Configuration.getInstance()
				.getMemTotalNumberOfPages());
		String ra = Helper.convertDecimalToBinary(rpn, nBitsRPN)
				+ va.substring(nBitsRPN);
		P2 lrpnp2 = Configuration.getInstance().getRLPageNumber(rpn);
		sl.addMessage(smpNodeIndex, numCPU, "rpn = " + rpn + " ra(binary)= "
				+ ra + " home smp node " + lrpnp2.y + " local rpn = "
				+ lrpnp2.x);
		
		//cache coherence protocol
		short[] statesFilter = null;
		
		switch(Configuration.getInstance().cacheMemCacheCoherencePolicy){
		case CacheCoherencePolicyType.MOESI:
			statesFilter = new short[]{CacheDirectory.STATE_EXCLUSIVE,CacheDirectory.STATE_OWNED, CacheDirectory.STATE_MODIFIED,CacheDirectory.STATE_SHARED};
			break;
		case CacheCoherencePolicyType.MOSI:
			statesFilter = new short[]{CacheDirectory.STATE_OWNED,CacheDirectory.STATE_MODIFIED,CacheDirectory.STATE_SHARED};
			break;
		case CacheCoherencePolicyType.MSI:
			statesFilter = new short[]{CacheDirectory.STATE_MODIFIED, CacheDirectory.STATE_SHARED};
			break;
		case CacheCoherencePolicyType.MESI:
			statesFilter = new short[]{CacheDirectory.STATE_EXCLUSIVE,CacheDirectory.STATE_MODIFIED, CacheDirectory.STATE_SHARED};
			break;
		}
		
		
		
		
		List<P2S1B1> allCaches = RuntimeModel.getInstance().cacheDir
		.lookup(ra, statesFilter);
		//difference MSI, MESI
		//in the case of MESI no invalidate broadcast when modifying a S
		//Owner flag means that the current cache holds the data the most recent:
		//only one processor may have O state for that data 
		
		

		// search all other caches, also the victim caches and
		// victim buffer, if any
		indStartPut = searchInMemCaches(smpNodeIndex, numCPU, ra,
				instrType!=InstructionType.FETCH_INSTR_TYPE);

		if (indStartPut == -2) {
			// not found in local caches
			// search mm,from same smp node 
			if (lrpnp2.y == smpNodeIndex) {
				currentDuration = sl.accessMainMem(smpNodeIndex, numCPU,
						smpNodeIndex, currentDuration);
				if (RuntimeModel.getInstance().smpNodeModels.get(smpNodeIndex).mainMemModel
						.getCacheEntry(lrpnp2.x) != null) {
					indStartPut = RuntimeModel.getInstance().smpNodeModels
							.get(smpNodeIndex).cpuToMemCacheModels.get(numCPU).cacheModels
							.size() - 1;
				}
			}
			//search remote cache
			if(Configuration.getInstance().smpNodeConfigs.get(smpNodeIndex).hasRemoteCache()){
				currentDuration = sl.accessRemoteCache(smpNodeIndex, numCPU, smpNodeIndex, currentDuration);
				if (Configuration.getInstance().smpNodeConfigs.get(smpNodeIndex).hasRemoteCache() && RuntimeModel.getInstance().smpNodeModels.get(smpNodeIndex).remoteDataCacheModel
						.getCacheEntry(rpn) != null) {
					indStartPut = RuntimeModel.getInstance().smpNodeModels
							.get(smpNodeIndex).cpuToMemCacheModels.get(numCPU).cacheModels
							.size() - 1;
				}
			}
			
			if (indStartPut == -2) {
				if (allCaches!=null && allCaches.size() > 0) {
					P2S1B1 elem = allCaches.get(0);
					// choose a random CPU for that cache
					P2 snoopCpu = Configuration.getInstance().smpNodeConfigs
					.get(elem.x).getRandomCPUFromIndexInMemArray(elem.y);

					sl.addMessage(smpNodeIndex, numCPU,
							"SNOOP : get data from " + elem.x
									+ " from cache at index " + elem.y);

					currentDuration = sl.accessMemCache(smpNodeIndex, numCPU,
							elem.x, snoopCpu.x, snoopCpu.y, currentDuration);
					pmgr.putActualExecProcessWait(smpNodeIndex, numCPU,
							Configuration.getInstance().smpNodeConfigs
									.get(smpNodeIndex).diskAccessTime, ra);

				}
				// get from home mm
				else {
					sl.addMessage(smpNodeIndex, numCPU, "SNOOP :  search in mm of node "
							+ lrpnp2.y + ",localRpn = " + lrpnp2.x);

					currentDuration = sl.accessMainMem(smpNodeIndex, numCPU,
							lrpnp2.y, currentDuration);

					if (RuntimeModel.getInstance().smpNodeModels.get(lrpnp2.y).mainMemModel
							.getCacheEntry(lrpnp2.x) != null) {
						sl.addMessage(smpNodeIndex, numCPU, "found");

					} else {
						sl.addMessage(smpNodeIndex, numCPU,
								"not found, why is the page in the PT?");
					}

				}
				// put in remote cache
				if(Configuration.getInstance().smpNodeConfigs.get(smpNodeIndex).hasRemoteCache()){
					currentDuration = sl.accessRemoteCache(smpNodeIndex, numCPU, smpNodeIndex, currentDuration);
					RuntimeModel.getInstance().smpNodeModels.get(smpNodeIndex).remoteDataCacheModel.putCacheEntry(pid, instrType, rpn);
					indStartPut = RuntimeModel.getInstance().smpNodeModels
							.get(smpNodeIndex).cpuToMemCacheModels.get(numCPU).cacheModels
							.size() - 1;
				}

			}
		}
		putInMemCaches(smpNodeIndex, numCPU, indStartPut, pid, ra, vpn,
				instrType,calculateNewState(instrType, CacheDirectory.STATE_SHARED));
		// write through?
		if (instrType == InstructionType.WRITE_DATA_TYPE
				&& indStartPut < RuntimeModel.getInstance().smpNodeModels
						.get(smpNodeIndex).cpuToMemCacheModels.get(numCPU).cacheModels
						.size() - 1) {
			putInMemCachesBack(smpNodeIndex, numCPU, indStartPut + 2, pid, ra,
					instrType, false,calculateNewState(instrType, CacheDirectory.STATE_SHARED));
		}

	}

	// writes in indStartPut and goes on if this one is write - through cache
	public void putInMemCachesBack(int smpNodeIndex, int numCPU,
			int indStartPut, int pid, String ra, short instrType, boolean isEvict, short state) {
		List<CacheConfigMem> ccfgmem = Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToMem.get(numCPU).caches;
		List<CacheModel> memcms = RuntimeModel.getInstance().smpNodeModels
				.get(smpNodeIndex).cpuToMemCacheModels.get(numCPU).cacheModels;
		StrLogger sl = RuntimeModel.getInstance().logger;
		CacheDirectory cdir = RuntimeModel.getInstance().cacheDir;
		CacheModel cm;
		int key;
		String binaryKey;
		int putIndex;
		boolean put = Configuration.getInstance().smpNodeConfigs.get(smpNodeIndex).cpuCachesToMem
		.get(numCPU).caches.get(indStartPut - 1).isWriteThrough && !isEvict;
		int indExclusiveCache = Configuration.getInstance().smpNodeConfigs
		.get(smpNodeIndex).cpuCachesToMem.get(numCPU).hasExclusiveCache()?Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToMem.get(numCPU).exclusiveCacheCfg.indexCache:-1; 

		// TODO calculate state from cache coherence policy (MSI, ...)
		// check write through
		for (int i = indStartPut; i < ccfgmem.size() && put; i--) {
			//if comes from a previous cache that is exclusive, put in previous assoc victim buffer
			if(indExclusiveCache==i-1){
				currentDuration = sl.accessMemExcBufferCache(smpNodeIndex, numCPU, smpNodeIndex, numCPU, currentDuration);
				binaryKey = ra.substring(0,Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToMem.get(numCPU).exclusiveCacheCfg.getNumberEntriesNBits(instrType!=InstructionType.FETCH_INSTR_TYPE));
				key = Integer.parseInt(binaryKey, 2);
				cm = RuntimeModel.getInstance().smpNodeModels.get(smpNodeIndex).cpuToMemCacheModels.get(numCPU).victimBufferModel;
				cm.putCacheEntry(pid, instrType, key);
				sl.addMessage(smpNodeIndex, numCPU,
						"WRITE THROUGH from exclusive cache put in victim buffer cache: "
								+ ccfgmem.get(i-1).getName() + " key = " + key);
			}
			binaryKey = ra.substring(0,ccfgmem.get(i).getNumberEntriesNBits(
					instrType!=InstructionType.FETCH_INSTR_TYPE));
			key = Integer.parseInt(binaryKey, 2);
			cm = memcms.get(i);
			currentDuration = sl.accessMemCache(smpNodeIndex, numCPU,
					smpNodeIndex, numCPU, i, currentDuration);
			putIndex = cm.putCacheEntry(pid, instrType, key);
			cdir
					.put(smpNodeIndex,
							Configuration.getInstance().smpNodeConfigs.get(
									smpNodeIndex).getIndexInMemCacheCfgs(
									numCPU, i), binaryKey, state, false);
			sl.addMessage(smpNodeIndex, numCPU,
					"WRITE THROUGH Put in mem cache "
							+ ccfgmem.get(i).getName() + " key = " + key);
			put = ccfgmem.get(i).isWriteThrough  && !isEvict;

		}
		if (put) {
			// put in mm
			key = Integer
					.parseInt(
							ra
									.substring(
											0,
											Configuration.getInstance().smpNodeConfigs
													.get(smpNodeIndex).mainMemoryConfig.numberEntriesNBits),
							2);
			RuntimeModel.getInstance().smpNodeModels.get(smpNodeIndex).mainMemModel
					.putCacheEntry(pid, instrType, key);
		}
	}

	private void putInMemCaches(int smpNodeIndex, int numCPU, int indStartPut,
			int pid, String ra, int vpn, short instrType, short state) {
		List<CacheConfigMem> ccfgmem = Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToMem.get(numCPU).caches;
		List<CacheModel> memcms = RuntimeModel.getInstance().smpNodeModels
				.get(smpNodeIndex).cpuToMemCacheModels.get(numCPU).cacheModels;
		StrLogger sl = RuntimeModel.getInstance().logger;
		CacheModel cm;
		int key;
		String binaryKey;
		int indexPut;
		// TODO calculate state from cache coherence policy (MSI, ...)
		CacheDirectory cdir = RuntimeModel.getInstance().cacheDir;
		int indExclusiveCache = Configuration.getInstance().smpNodeConfigs
		.get(smpNodeIndex).cpuCachesToMem.get(numCPU).hasExclusiveCache()?Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToMem.get(numCPU).exclusiveCacheCfg.indexCache:-1; 

		// check write allocate
		for (int i = indStartPut; i >= 0
				&& (i == ccfgmem.size() - 1 || ccfgmem.get(i + 1).isWriteAllocate); i--) {
			if(indExclusiveCache!=i){
			binaryKey = ra.substring(0,ccfgmem.get(i).getNumberEntriesNBits(
					instrType!=InstructionType.FETCH_INSTR_TYPE));
			key = Integer.parseInt(binaryKey, 2);
			cm = memcms.get(i);
			indexPut = cm.putCacheEntry(pid, instrType, key);
			cdir
			.put(smpNodeIndex,
					Configuration.getInstance().smpNodeConfigs.get(
							smpNodeIndex).getIndexInMemCacheCfgs(
							numCPU, i), binaryKey, state, false);
			
			currentDuration = sl.accessMemCache(smpNodeIndex, numCPU,
					smpNodeIndex, numCPU, i, currentDuration);
			sl.addMessage(smpNodeIndex, numCPU,
					"WRITE ALLOCATE Put in mem cache "
							+ ccfgmem.get(i).getName() + " key = " + key);
			}
			else{
				sl.addMessage(smpNodeIndex, numCPU, "exclusive cache : skipped  "
						+ ccfgmem.get(i).getName() );
			}
		}
	}

	public void putInPTCaches(int smpNodeIndex, int numCPU, int indStartPut,
			int vpn, int rpn, int pid, short instrType) {
		List<CacheConfigPT> ccfgPt = Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToPT.get(numCPU).caches;
		List<CacheModel> ptcms = RuntimeModel.getInstance().smpNodeModels
				.get(smpNodeIndex).cpuToPTCacheModels.get(numCPU).cacheModels;
		StrLogger sl = RuntimeModel.getInstance().logger;
		CacheModel cm;
		int indExclusiveCache = Configuration.getInstance().smpNodeConfigs
		.get(smpNodeIndex).cpuCachesToPT.get(numCPU).hasExclusiveCache()?Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToPT.get(numCPU).exclusiveCacheCfg.indexCache:-1; 
		for (int i = indStartPut; i >= 0; i--) {
			if(indExclusiveCache!=i){
				cm = ptcms.get(i);
				cm.putCacheEntry(pid, instrType, vpn, rpn);
				currentDuration = sl.accessPTCache(smpNodeIndex, numCPU,
						smpNodeIndex, numCPU, i, currentDuration);
				sl.addMessage(smpNodeIndex, numCPU, "Put in pt cache  "
						+ ccfgPt.get(i).getName() + " vpn = " + vpn + " , key = " + rpn);
			}
			else{
				sl.addMessage(smpNodeIndex, numCPU, "exclusive cache : skipped  "
						+ ccfgPt.get(i).getName() );
			}
			
		}
	}

	// return a P2 the rpn from pt caches if found and insdStartPut
	public P2 searchInPTCaches(int smpNodeIndex, int numCPU, int vpn) {

		IndexCacheBlockCfg exclusiveCacheCfg = Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToPT.get(numCPU).exclusiveCacheCfg;

		StrLogger sl = RuntimeModel.getInstance().logger;

		int rpn = -1;
		P2 p2 = null;
		List<CacheConfigPT> ccfgPt = Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToPT.get(numCPU).caches;
		List<CacheModel> ptcms = RuntimeModel.getInstance().smpNodeModels
				.get(smpNodeIndex).cpuToPTCacheModels.get(numCPU).cacheModels;
		CacheModel cm, cmo;

		int indStartPut = ccfgPt.size() - 1;
		List<Entry> entry;

		// then start put from
		boolean f = false;
		if (rpn == -1) {
			f = false;
			indStartPut = ptcms.size() - 1;
			for (int i = 0; i < ptcms.size() && !f; i++) {
				cm = ptcms.get(i);
				entry = cm.getCacheEntry(vpn);
				currentDuration = sl.accessPTCache(smpNodeIndex, numCPU,
						smpNodeIndex, numCPU, i, currentDuration);
				sl.addMessage(smpNodeIndex, numCPU, "Search in pt cache "
						+ ccfgPt.get(i).getName() + " for vpn " + vpn);
				if (entry != null) {
					rpn = getRpn(cm, entry);
					sl.addMessage(smpNodeIndex, numCPU, "found, rpn is " + rpn);
					indStartPut = i - 1;
					f = true;
				} else {
					sl.addMessage(smpNodeIndex, numCPU, "not found");
				}
				// look in victim cache
				if (!f) {
					cmo = RuntimeModel.getInstance().smpNodeModels
							.get(smpNodeIndex).victimCacheModels.get(cm);
					if (cmo != null) {
						currentDuration = sl.accessPTVictimCache(smpNodeIndex,
								numCPU, smpNodeIndex, numCPU, i,
								currentDuration);
						sl.addMessage(smpNodeIndex, numCPU,
								"Search in victim cache of pt cache  "
										+ ccfgPt.get(i).getName());

						if (entry != null) {
							sl.addMessage(smpNodeIndex, numCPU,
									"found, rpn is " + rpn);
							rpn = getRpn(cm, entry);
							indStartPut = i;
							f = true;
						} else {
							sl.addMessage(smpNodeIndex, numCPU, "not found");
						}
					}
				}
				// if it's the exclusive cache search in victim buffer
				if (!f && exclusiveCacheCfg != null
						&& exclusiveCacheCfg.indexCache == i) {
					cm = RuntimeModel.getInstance().smpNodeModels
							.get(smpNodeIndex).cpuToPTCacheModels.get(numCPU).victimBufferModel;
					entry = cm.getCacheEntry(vpn);
					currentDuration = sl.accessPTExcBufferCache(smpNodeIndex,
							numCPU, smpNodeIndex, numCPU, currentDuration);
					sl.addMessage(smpNodeIndex, numCPU,
							"Exclusive cache: search in pt victim buffer ");
					if (entry != null) {
						sl.addMessage(smpNodeIndex, numCPU, "found, rpn is "
								+ rpn);
						rpn = getRpn(cm, entry);
						if (exclusiveCacheCfg.indexCache < ccfgPt.size() - 1) {
							indStartPut = exclusiveCacheCfg.indexCache + 1;
						}
					} else {
						sl.addMessage(smpNodeIndex, numCPU, "not found");
					}

				}
			}

		}
		p2 = new P2();
		p2.x = rpn;
		p2.y = indStartPut;
		return p2;
	}

	

	
	private int searchInMemCaches(int smpNodeIndex, int numCPU, String ra, boolean isData) {
		StrLogger sl = RuntimeModel.getInstance().logger;
		List<CacheConfigMem> ccfgmem = Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToMem.get(numCPU).caches;

		List<CacheModel> memcms = RuntimeModel.getInstance().smpNodeModels
				.get(smpNodeIndex).cpuToMemCacheModels.get(numCPU).cacheModels;

		IndexCacheBlockCfg exclusiveCacheCfg = Configuration.getInstance().smpNodeConfigs
				.get(smpNodeIndex).cpuCachesToMem.get(numCPU).exclusiveCacheCfg;
		CacheModel cm, cmo;
		List<Entry> entry;
		int indStartPut = -2;
		boolean f = false;
		int key;
		// find the index
		for (int i = 0; i < memcms.size() && !f; i++) {
			key = Integer.parseInt(ra.substring(0, ccfgmem.get(i)
					.getBlockSizeNBits(isData)), 2);
			cm = memcms.get(i);
			entry = cm.getCacheEntry(key);
			currentDuration = sl.accessMemCache(smpNodeIndex, numCPU,
					smpNodeIndex, numCPU, i, currentDuration);
			sl.addMessage(smpNodeIndex, numCPU, "Search in mem cache  "
					+ ccfgmem.get(i).getName() + " key = " + key);
			if (entry != null) {
				sl.addMessage(smpNodeIndex, numCPU, "found ");
				indStartPut = i - 1;
				f = true;
			} else {
				sl.addMessage(smpNodeIndex, numCPU, "not found");
			}
			// look in victim cache
			cmo = RuntimeModel.getInstance().smpNodeModels.get(smpNodeIndex).victimCacheModels
					.get(cm);
			if (!f && cmo != null) {
				entry = cmo.getCacheEntry(key);
				currentDuration = sl.accessMemVictimCache(smpNodeIndex, numCPU,
						smpNodeIndex, numCPU, i, currentDuration);
				sl.addMessage(smpNodeIndex, numCPU,
						"Search in victim cache of mem cache  "
								+ ccfgmem.get(i).getName() + " for key " + key);
				if (entry != null) {
					sl.addMessage(smpNodeIndex, numCPU, "found ");
					indStartPut = i;
					f = true;
				} else {
					sl.addMessage(smpNodeIndex, numCPU, "not found");
				}
			}
			if (!f && exclusiveCacheCfg != null
					&& exclusiveCacheCfg.indexCache == i) {
				key = Integer.parseInt(ra.substring(0, ccfgmem.get(
						exclusiveCacheCfg.indexCache).getBlockSizeNBits(
						isData)), 2);
				cm = RuntimeModel.getInstance().smpNodeModels.get(smpNodeIndex).cpuToMemCacheModels
						.get(numCPU).victimBufferModel;
				entry = cm.getCacheEntry(key);
				currentDuration = sl.accessMemExcBufferCache(smpNodeIndex,
						numCPU, smpNodeIndex, numCPU, currentDuration);
				sl.addMessage(smpNodeIndex, numCPU,
						"Exclusive cache: search in mem victim buffer key = "
								+ key);
				if (entry != null) {
					sl.addMessage(smpNodeIndex, numCPU, "found ");
					if (exclusiveCacheCfg.indexCache < ccfgmem.size() - 1) {
						indStartPut = exclusiveCacheCfg.indexCache + 1;
					}
					f = true;
				} else {
					sl.addMessage(smpNodeIndex, numCPU, "not found");
				}
			}
		}

		return indStartPut;
	}

	class SyncObj {
		Object syncObj;

		int duration;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SyncObj) {
				SyncObj so = (SyncObj) obj;
				return so.duration == duration && so.syncObj.equals(syncObj);
			}
			return false;
		}

	}

	private static int getRpn(CacheModel cm, List<Entry> le) {
		Entry e;
		for (int i = 0; i < le.size(); i++) {
			e = le.get(i);
			if (cm.getAddColumn(i) instanceof PTCacheAddColumn) {
				return ((EntryII) e).q;
			}
		}
		return -1;
	}

	
	class P4 extends P3 {
		int t;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof P4) {
				P4 p4 = (P4) obj;
				return x == p4.x && y == p4.y && z == p4.z && t == p4.t;
			}
			return false;

		}

	}

	class P5 extends P4 {
		int u;

		@Override
		public boolean equals(Object obj) {
			;
			if (obj instanceof P5) {
				P5 p5 = (P5) obj;
				return x == p5.x && y == p5.y && z == p5.z && t == p5.t
						&& u == p5.u;
			}
			return false;

		}

	}

}
