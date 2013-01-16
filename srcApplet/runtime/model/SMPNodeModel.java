package runtime.model;

import global.Helper;

import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.org.apache.xalan.internal.lib.NodeInfo;

import config.model.CacheConfigMem;
import config.model.CacheConfigPT;
import config.model.Configuration;
import config.model.MainMemCacheConfig;
import config.model.SMPNodeConfig;
import constants.CacheCoherencePolicyType;
import constants.CacheEvictionPolicyType;
import constants.InstructionType;

import view.model.cache.CacheListener;
import view.model.cache.CacheModel;
import view.model.cache.DIAddColumns;
import view.model.cache.DISepAddColumns;
import view.model.cache.Entry;
import view.model.cache.EntryB;
import view.model.cache.EntryI;
import view.model.cache.FIFOAddColumns;
import view.model.cache.LFUAddColumns;
import view.model.cache.LRUAddColumns;
import view.model.cache.MRUAddColumns;
import view.model.cache.MainMemAddColumn;
import view.model.cache.MemCacheAddColumns;
import view.model.cache.NFUAddColumns;
import view.model.cache.NRUAddColumns;
import view.model.cache.PTCacheAddColumn;
import view.model.cache.PidAddColumns;
import view.model.cache.PidExtAddColumns;
import view.model.cache.SetsAddColumns;

public class SMPNodeModel {

	public List<CacheChainModel> cpuToPTCacheModels;

	public List<CacheChainModel> cpuToMemCacheModels;

	public Map<CacheModel, CacheModel> victimCacheModels;

	public List<CacheModel> cacheModelsPT;
	public List<CacheModel> cacheModelsMem;

	public CacheModel mainMemModel;

	public CacheModel remoteDataCacheModel = null;

	public SMPNodeModel(final int smpIndex) {
		cacheModelsPT = new ArrayList<CacheModel>();
		cacheModelsMem = new ArrayList<CacheModel>();
		cpuToPTCacheModels = new ArrayList<CacheChainModel>();
		cpuToMemCacheModels = new ArrayList<CacheChainModel>();

		// victim buffers : key the cache , value the victim buffer cache
		victimCacheModels = new HashMap<CacheModel, CacheModel>();

		SMPNodeConfig smpNodeCfg = Configuration.getInstance().smpNodeConfigs
				.get(smpIndex);
		CacheModel cModel, vModel;
		CacheConfigPT ptcCfg;
		// ADD PT CACHES
		for (int i = 0; i < smpNodeCfg.cacheConfigsPT.size(); i++) {
			ptcCfg = smpNodeCfg.cacheConfigsPT.get(i);
			cModel = new CacheModel((int) Math.pow(2, ptcCfg
					.getTotalNumberEntriesNBits()));
			cModel.addAddColumns(new PTCacheAddColumn());
			addColumnsToCacheModel(cModel, ptcCfg);
			cacheModelsPT.add(cModel);
			if (ptcCfg.hasVictimCache()) {
				// create the victim cache for pt
				vModel = createVictimPTModel((int) Math.pow(2,
						ptcCfg.victimCacheCfg.getTotalNumberEntriesNBits()),
						ptcCfg.isDataInstrSeparated() ? (int) Math
								.pow(2, ptcCfg.victimCacheCfg
										.getNumberEntriesNBits()[0]) : -1);
				victimCacheModels.put(cModel, vModel);
				cModel.addCacheListener(new VictimCacheListener(vModel));
			}
		}

		CacheConfigPT ccfgpt;
		for (int i = 0; i < smpNodeCfg.cpuCachesToPT.size(); i++) {
			cpuToPTCacheModels.add(new CacheChainModel());
			for (int j = 0; j < smpNodeCfg.cpuCachesToPT.get(i).caches.size(); j++) {
				ccfgpt = smpNodeCfg.cpuCachesToPT.get(i).caches.get(j);
				cpuToPTCacheModels.get(i).cacheModels.add(cacheModelsPT
						.get(smpNodeCfg.cacheConfigsPT.indexOf(ccfgpt)));
			}
			if (smpNodeCfg.cpuCachesToPT.get(i).exclusiveCacheCfg != null) {
				cpuToPTCacheModels.get(i).victimBufferModel = createVictimMemModel(
						(int) Math
								.pow(
										2,
										smpNodeCfg.cpuCachesToPT.get(i).exclusiveCacheCfg
												.getTotalNumberEntriesNBits()),
						smpNodeCfg.cpuCachesToPT.get(i).exclusiveCacheCfg
								.isDataInstrSeparated() ? (int) Math
								.pow(
										2,
										smpNodeCfg.cpuCachesToPT.get(i).exclusiveCacheCfg
												.getNumberEntriesNBits()[0])
								: -1);

			}
		}
		CacheConfigMem memcCfg;

		// ADD MEM CACHES
		for (int i = 0; i < smpNodeCfg.cacheConfigsMem.size(); i++) {
			memcCfg = smpNodeCfg.cacheConfigsMem.get(i);

			cModel = new CacheModel((int) Math.pow(2, memcCfg
					.getTotalNumberEntriesNBits()));
			cModel.addAddColumns(new MemCacheAddColumns());
			addColumnsToCacheModel(cModel, memcCfg);
			cacheModelsMem.add(cModel);
			if (memcCfg.hasVictimCache()) {
				// create the victim cache
				vModel = createVictimMemModel((int) Math.pow(2,
						memcCfg.victimCacheCfg.getTotalNumberEntriesNBits()),
						memcCfg.isDataInstrSeparated() ? (int) Math
								.pow(2, memcCfg.victimCacheCfg
										.getNumberEntriesNBits()[0]) : -1);
				victimCacheModels.put(cModel, vModel);
				cModel.addCacheListener(new VictimCacheListener(vModel));

			}

		}
		CacheConfigMem ccfgmem;

		for (int i = 0; i < smpNodeCfg.cpuCachesToMem.size(); i++) {
			cpuToMemCacheModels.add(new CacheChainModel());
			for (int j = 0; j < smpNodeCfg.cpuCachesToMem.get(i).caches.size(); j++) {
				ccfgmem = smpNodeCfg.cpuCachesToMem.get(i).caches.get(j);
				cModel = cacheModelsMem.get(smpNodeCfg.cacheConfigsMem
						.indexOf(ccfgmem));
				if (!ccfgmem.isWriteThrough) {
					cModel.addCacheListener(new WriteBackCacheListener(
							smpIndex, i, j + 1, ccfgmem));
				}
				cpuToMemCacheModels.get(i).cacheModels.add(cModel);
			}
			if (smpNodeCfg.cpuCachesToMem.get(i).exclusiveCacheCfg != null) {
				cpuToMemCacheModels.get(i).victimBufferModel = createVictimMemModel(
						(int) Math
								.pow(
										2,
										smpNodeCfg.cpuCachesToMem.get(i).exclusiveCacheCfg
												.getTotalNumberEntriesNBits()),
						smpNodeCfg.cpuCachesToMem.get(i).exclusiveCacheCfg
								.isDataInstrSeparated() ? (int) Math
								.pow(
										2,
										smpNodeCfg.cpuCachesToMem.get(i).exclusiveCacheCfg
												.getNumberEntriesNBits()[0])
								: -1);
			}

		}
		final MainMemCacheConfig mainMemCacheCfg = smpNodeCfg.mainMemoryConfig;
		mainMemModel = new CacheModel((int) Math.pow(2, mainMemCacheCfg
				.getNumberEntriesNBits()));
		mainMemModel.addCacheListener(new CacheListener() {

			@Override
			public void entriesAccessed(int index, CacheModel cm) {
			}

			@Override
			public void entriesPut(int index, CacheModel cm) {
			}

			@Override
			public void entriesToBeEvicted(int index, CacheModel cm) {
				// remove from page table
				int globalrpn = Configuration.getInstance().getRVPageNumber(
						smpIndex, cm.getKeyAtIndex(index));
				RuntimeModel.getInstance().pageTableModel
						.swapMemEntry(globalrpn);
			}
		});
		mainMemModel.addAddColumns(new MainMemAddColumn());

		if (mainMemCacheCfg.mainMemoryAllocConfig != null) {
			mainMemModel.addAddColumns(new PidExtAddColumns());
		} else {
			mainMemModel.addAddColumns(new PidAddColumns());
		}
		if (mainMemCacheCfg.numberSetsNBits != 0) {
			mainMemModel.addAddColumns(new SetsAddColumns((int) Math.pow(2,
					mainMemCacheCfg.numberSetsNBits)));
		}
		// page aging thread
		if (mainMemCacheCfg.pageAgingConfig != null) {
			mainMemModel.addCacheListener(new PageAgingThread(smpIndex,
					mainMemCacheCfg.pageAgingConfig.getPageAgingIncrease(),
					mainMemCacheCfg.pageAgingConfig.getMemRefToBeRun()));
		}

		if(smpNodeCfg.hasRemoteCache()){
			remoteDataCacheModel = new CacheModel((int) Math.pow(2,
					smpNodeCfg.remoteDataCache.getTotalNumberEntriesNBits()));
			remoteDataCacheModel.addAddColumns(new MemCacheAddColumns());
			remoteDataCacheModel.addAddColumns(new PidAddColumns());
		}
	}

	private static CacheModel createVictimPTModel(int numEntries,
			int numDataEntries) {
		CacheModel vcModel = new CacheModel(numEntries);
		vcModel.addAddColumns(new PTCacheAddColumn());
		vcModel.addAddColumns(new PidAddColumns());
		if (numDataEntries > 0) {
			vcModel.addAddColumns(new DISepAddColumns(numDataEntries));
		}
		return vcModel;
	}

	private static CacheModel createVictimMemModel(int numEntries,
			int numDataEntries) {
		// TODO state for victim?
		CacheModel vcModel = new CacheModel(numEntries);
		vcModel.addAddColumns(new MemCacheAddColumns());
		vcModel.addAddColumns(new PidAddColumns());
		if (numDataEntries > 0) {
			vcModel.addAddColumns(new DISepAddColumns(numDataEntries));
		}
		return vcModel;
	}

	private static void addColumnsToCacheModel(CacheModel cModel,
			CacheConfigPT cacheCfg) {
		cModel.addAddColumns(new PidAddColumns());
		if (cacheCfg.isDataInstrSeparated()) {
			cModel.addAddColumns(new DISepAddColumns((int) Math.pow(2, cacheCfg
					.getNumberEntriesNBits(true))));
		} else {
			cModel.addAddColumns(new DIAddColumns());
		}
		if (cacheCfg.getNumberSetsNBits() != 0) {
			cModel.addAddColumns(new SetsAddColumns((int) Math.pow(2, cacheCfg
					.getNumberSetsNBits())));
		}
		// cache eviction policy
		switch (cacheCfg.getEvictionPolicy()) {
		case CacheEvictionPolicyType.FIFO_POLICY:
			cModel.addAddColumns(new FIFOAddColumns());
			break;
		case CacheEvictionPolicyType.LFU_POLICY:
			cModel.addAddColumns(new LFUAddColumns());
			break;
		case CacheEvictionPolicyType.LRU_POLICY:
			cModel.addAddColumns(new LRUAddColumns());
			break;
		case CacheEvictionPolicyType.MRU_POLICY:
			cModel.addAddColumns(new MRUAddColumns());
			break;
		case CacheEvictionPolicyType.NFU_POLICY:
			cModel.addAddColumns(new NFUAddColumns());
			break;
		case CacheEvictionPolicyType.NRU_POLICY:
			cModel.addAddColumns(new NRUAddColumns());
			break;
		}

	}

	class PageAgingThread implements Runnable, CacheListener {

		private Map<Integer, Integer> age = new HashMap<Integer, Integer>();
		private int smpNodeIndex, incUnits, memRefToBeRun, memRef;

		public PageAgingThread(int smpNodeIndex, int incUnits, int memRefToBeRun) {
			this.smpNodeIndex = smpNodeIndex;
			this.memRefToBeRun = memRefToBeRun;
			this.incUnits = incUnits;
		}

		@Override
		public void run() {
			CacheModel memModel = RuntimeModel.getInstance().smpNodeModels
					.get(smpNodeIndex).mainMemModel;
			Iterator<Integer> ageIndexesIt = age.keySet().iterator();
			int index;
			StrLogger sl = RuntimeModel.getInstance().logger;
			while (ageIndexesIt.hasNext()) {
				index = ageIndexesIt.next();
				if (memModel.getEntriesAtIndex(index) != null) {
					if (age.get(index) <= 0) {
						// remove row at this index
						// sl.add(smpNodeIndex, numCPU, msg);
						memModel.removeEntry(index);
					}
				}
				age.put(index, age.get(index) - 1);
			}
			memRef = 0;
		}

		@Override
		public void entriesAccessed(int index, CacheModel cm) {
			if (age.get(index) == null) {
				age.put(index, 0);
			}
			age.put(index, age.get(index) + incUnits);
			memRef++;
			if (memRef >= memRefToBeRun) {
				new Thread(this).start();
			}

		}

		@Override
		public void entriesPut(int index, CacheModel cm) {
			entriesAccessed(index, cm);
		}

		@Override
		public void entriesToBeEvicted(int index, CacheModel cm) {

		}

	}

	class WriteBackCacheListener implements CacheListener {

		private int nodeIndex;
		private int numCPU;
		private int indStartPut;
		private CacheConfigMem ccfg;

		public WriteBackCacheListener(int nodeIndex, int numCPU,
				int indexStartPut, CacheConfigMem ccfg) {
			this.nodeIndex = nodeIndex;
			this.numCPU = numCPU;
			this.indStartPut = indexStartPut;
			this.ccfg = ccfg;
		}

		@Override
		public void entriesAccessed(int index, CacheModel cm) {
		}

		@Override
		public void entriesPut(int index, CacheModel cm) {
		}

		@Override
		public void entriesToBeEvicted(int index, CacheModel cm) {
			int pid = cm.getPidAtIndex(index);
			int cacheKey = cm.getKeyAtIndex(index);
			boolean isData = cm.isDataAtIndex(index);
			RuntimeModel.getInstance().sched.putInMemCachesBack(nodeIndex,
					numCPU, indStartPut, pid, Helper.convertDecimalToBinary(
							cacheKey, (int) Math.pow(2, ccfg
									.getNumberEntriesNBits(isData))),
					isData ? InstructionType.READ_DATA_TYPE
							: InstructionType.FETCH_INSTR_TYPE, true, CacheDirectory.STATE_MODIFIED);
		}

	}

	class VictimCacheListener implements CacheListener {

		private CacheModel cvm;

		public VictimCacheListener(CacheModel cm) {
			this.cvm = cm;
		}

		@Override
		public void entriesAccessed(int index, CacheModel cm) {
		}

		@Override
		public void entriesPut(int index, CacheModel cm) {
		}

		@Override
		public void entriesToBeEvicted(int index, CacheModel cm) {
			int key = cm.getKeyAtIndex(index);
			boolean isData = cm.isDataAtIndex(index);
			cvm.putCacheEntry(cm.getPidAtIndex(index),
					isData ? InstructionType.READ_DATA_TYPE
							: InstructionType.FETCH_INSTR_TYPE, key);
			P2 p2 = RuntimeModel.getInstance().getCacheModelIndices(cm);
			if (p2 == null) {
				return;
			}
			// put with same state in the cache dir
			String binaryKey = Helper
			.convertDecimalToBinary(key,
					Configuration.getInstance().smpNodeConfigs
							.get(p2.x).cacheConfigsMem.get(p2.y)
							.getNumberEntriesNBits(isData));
			RuntimeModel.getInstance().cacheDir.put(p2.x,
					p2.y, binaryKey,  RuntimeModel.getInstance().cacheDir.getState(binaryKey , p2.x , p2.y , true), true);
		}

	}

}
