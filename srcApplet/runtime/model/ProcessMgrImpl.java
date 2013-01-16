package runtime.model;

import global.Logger;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import config.model.Configuration;

import trace.Instr;

public class ProcessMgrImpl implements ProcessMgr {

	public List<List<List<ThreadProcDesc>>> procs;

	private Map<P2, ThreadProcDesc> execProcs;

	private List<Integer> availablePids = new ArrayList<Integer>();

	// holds a map between waiting processes and rpn, might have to rceate
	// Interrupted
	private Map<ThreadProcDesc, String> ctxs;

	public ProcessMgrImpl() {
		// map P2 (nodeSMP , numCPU ) - ThreadProcDesc
		// associated with the
		// thread exec for the smpNode and numCPU
		execProcs = new HashMap<P2, ThreadProcDesc>();
		ctxs = new HashMap<ThreadProcDesc, String>();
		procs = new ArrayList<List<List<ThreadProcDesc>>>();
		for (int i = 0; i < Configuration.getInstance().smpNodeConfigs.size(); i++) {
			procs.add(new ArrayList<List<ThreadProcDesc>>());
			for (int j = 0; j < Configuration.getInstance().smpNodeConfigs
					.get(i).cpuCachesToMem.size(); j++) {
				procs.get(i).add(new ArrayList<ThreadProcDesc>());
			}
		}
	}

	public Instr nextInstruction(int smpNodeIndex, int numCPU, int delay) {
		StrLogger sl = RuntimeModel.getInstance().logger;
		ThreadProcDesc tpd = getCurExecProcess(smpNodeIndex, numCPU), otpd;
		if (tpd == null) {
			return null;
		}
		if (ctxs.get(tpd) != null) {
			String ra = ctxs.get(tpd);
			ctxs.remove(tpd);
			return new ExtInstr(tpd.instructions.get(tpd.currentInstrNumber),
					ra);
		}
		sl.addMessage(smpNodeIndex, numCPU, "last instruction delay " + delay);
		for (int k = 0; k < procs.get(smpNodeIndex).get(numCPU).size(); k++) {
			otpd = procs.get(smpNodeIndex).get(numCPU).get(k);
			if (otpd.state == ThreadProcDesc.WAITING_STATE) {
				sl.addMessage(smpNodeIndex, numCPU,
						"put process from waiting to ready state pid = "
								+ tpd.pid + ", numberThread = "
								+ tpd.numberThread);
				otpd.timeUnitsLeft -= delay;
				if (otpd.timeUnitsLeft <= 0) {
					otpd.state = ThreadProcDesc.READY_STATE;
					otpd.timeUnitsLeft = otpd.timeUnits;
				}
			}
		}
		if (tpd != null) {
			tpd.timeUnitsLeft -= delay;
			if (tpd.timeUnitsLeft <= 0) {
				preemptActualExecProcess(smpNodeIndex, numCPU);
				tpd = getCurExecProcess(smpNodeIndex, numCPU);
			}
		}

		if (tpd != null) {
			tpd.currentInstrNumber++;
			if (tpd.instructions.size() <= tpd.currentInstrNumber) {
				// process terminated
				otpd = selectExecProcess(smpNodeIndex, numCPU);
				if (otpd != null) {
					otpd.state = ThreadProcDesc.RUNNING_STATE;
					sl.addMessage(smpNodeIndex, numCPU,
							"process teminated , pid = " + tpd.pid
									+ " , thread = " + tpd.numberThread
									+ ", switch to( " + otpd.pid + ","
									+ otpd.numberThread + ")");
					removeProcess(tpd.pid);
					return otpd.instructions.get(otpd.currentInstrNumber);
				} else {
					sl.addMessage(smpNodeIndex, numCPU,
							"all processes terminated ");
				}

			} else {
				return tpd.instructions.get(tpd.currentInstrNumber);
			}

		}
		return null;

	}

	public void addNewProcesses(List<Instr> instructions, String processName,
			int numThreads, int timeUnits) {
		if (instructions.size() < numThreads) {
			Logger.log("instructions size < numThreads");
			return;
		}

		// processor allocator policy
		// first try to allocate all threads in the same node if avail
		// order by processor tasks
		// make an array of Point

		List<Point> a = new ArrayList<Point>();
		final int numberSMPNodes = procs.size();
		for (int i = 0; i < numberSMPNodes; i++) {
			for (int j = 0; j < procs.get(i).size(); j++) {
				a.add(new Point(i, j));
			}
		}
		assert a.size() > 0;
		Collections.sort(a, new Comparator<Point>() {

			public int compare(Point o1, Point o2) {
				return (procs.get(o1.x).get(o1.y).size() - procs.get(o2.x).get(
						o2.y).size());
			}
		});
		ThreadProcDesc tpd;
		int pid;
		if (availablePids.size() > 0) {
			pid = availablePids.remove(0);
		} else {
			pid = getTotalNumberOfProcesses();
		}

		for (int n = 0; n < numThreads; n++) {
			tpd = new ThreadProcDesc();
			procs.get(a.get(n % a.size()).x).get(a.get(n % a.size()).y)
					.add(tpd);
			tpd.pid = pid;
			tpd.numberThread = n;
			tpd.timeUnits = timeUnits;
			tpd.state = ThreadProcDesc.READY_STATE;
			tpd.timeUnitsLeft = timeUnits;
			tpd.priority = 0;
			tpd.currentInstrNumber = -1;
			tpd.name = processName;
			tpd.instructions = new ArrayList<Instr>();
			for (int k = 0; k < instructions.size(); k++) {
				if (k % numThreads == n) {
					tpd.instructions.add(instructions.get(k));
				}
			}
		}

	}

	public int getTotalNumberOfThreads() {
		int n = 0;
		for (int i = 0; i < procs.size(); i++) {
			for (int j = 0; j < procs.get(i).size(); j++) {
				n += procs.get(i).get(j).size();
			}
		}
		return n;
	}

	private int getTotalNumberOfProcesses() {
		List<Integer> pids = new ArrayList<Integer>();
		for (int i = 0; i < procs.size(); i++) {
			for (int j = 0; j < procs.get(i).size(); j++) {
				for (int k = 0; k < procs.get(i).get(j).size(); k++) {
					if (!pids.contains(procs.get(i).get(j).get(k).pid)) {
						pids.add(procs.get(i).get(j).get(k).pid);
					}
				}
			}
		}
		return pids.size();
	}

	public void putActualExecProcessWait(int smpNodeIndex, int numCPU,
			int waitTime, String ra) {
		ThreadProcDesc tpd = getCurExecProcess(smpNodeIndex, numCPU);
		if (tpd == null) {
			System.out.println("proc null, getCurrExecProc must return ");
			return;
		}
		RuntimeModel.getInstance().logger.addMessage(smpNodeIndex, numCPU,
				"put current process in wait state ");
		tpd.state = ThreadProcDesc.WAITING_STATE;
		tpd.timeUnitsLeft = waitTime;
		ctxs.put(tpd, ra);

	}

	private boolean preemptActualExecProcess(int smpNodeIndex, int numCPU) {
		ThreadProcDesc tpd = getCurExecProcess(smpNodeIndex, numCPU);
		if (tpd != null) {
			StrLogger sl = RuntimeModel.getInstance().logger;
			sl.addMessage(smpNodeIndex, numCPU,
					"check to see if preempt actual process " + tpd.pid
							+ ", numberThread = " + tpd.numberThread);
			tpd.timeUnitsLeft = tpd.timeUnits;
			ThreadProcDesc otpd;
			int maxGoodness = 0;
			int goodness;
			ThreadProcDesc ptpd = null;
			for (int i = 0; i < procs.get(smpNodeIndex).size(); i++) {
				for (int j = 0; j < procs.get(smpNodeIndex).get(i).size(); j++) {
					otpd = procs.get(smpNodeIndex).get(i).get(j);
					if (otpd.state == ThreadProcDesc.READY_STATE) {
						goodness = goodness(tpd, otpd, (j == numCPU));
						if (goodness > 0 && goodness > maxGoodness) {
							maxGoodness = goodness;
							ptpd = otpd;
						}

					}
				}
			}
			P2 p2 = new P2();
			p2.x = smpNodeIndex;
			p2.y = numCPU;
			if (ptpd != null) {
				sl.addMessage(smpNodeIndex, numCPU,
						"preempt actual process, switch to = " + ptpd.pid
								+ ", numberThread = " + ptpd.numberThread);
				execProcs.put(p2, ptpd);
				ptpd.state = ThreadProcDesc.RUNNING_STATE;
				tpd.state = ThreadProcDesc.READY_STATE;
				return true;
			}
			sl.addMessage(smpNodeIndex, numCPU, "not preempting");
			if (tpd.currentInstrNumber == tpd.instructions.size()) {
				// remove the thread
				removeThreadExt(smpNodeIndex, numCPU, tpd);
				// check if there any other thread of this process,
				// if not, we must remove the process:
				// remove all entries of this process from caches , memory and
				// page
				// table
				boolean found = false;
				for (int i = 0; i < procs.size() && !found; i++) {
					for (int j = 0; j < procs.get(i).size() && !found; j++) {
						for (int k = 0; k < procs.get(i).get(j).size()
								&& !found; k++) {
							if (procs.get(i).get(j).get(k).pid == tpd.pid) {
								found = true;
							}
						}
					}
				}
				if (!found) {
					removeProcessExt(tpd.pid);
				}
			}
		}
		return false;
	}

	private void removeThreadExt(int smpNodeIndex, int numCPU,
			ThreadProcDesc tpd) {
		P2 p2 = new P2(smpNodeIndex, numCPU);
		procs.get(smpNodeIndex).get(numCPU).remove(tpd);
		if (execProcs.get(p2) != null && execProcs.get(p2).equals(tpd)) {
			execProcs.remove(p2);
		}
		ctxs.remove(tpd);

	}

	public void removeProcess(int pid) {
		// remove all threads of this pid from all smpnodes
		for (int i = 0; i < procs.size(); i++) {
			for (int j = 0; j < procs.get(i).size(); j++) {
				for (int k = 0; k < procs.get(i).get(j).size(); k++) {
					if (procs.get(i).get(j).get(k).pid == pid) {
						removeThreadExt(i, j, procs.get(i).get(j).get(k));
					}

				}
			}
		}
		// remove process from pt, caches
		removeProcessExt(pid);
	}

	private void removeProcessExt(int pid) {
		availablePids.add(pid);
		// remove process
		RuntimeModel.getInstance().pageTableModel.removeProcess(pid);
		//remove from mainmem, remotecache models
		for(int i = 0;i<RuntimeModel.getInstance().smpNodeModels.size();i++){
			RuntimeModel.getInstance().smpNodeModels.get(i).mainMemModel.removeAllEntries(pid);
			if(Configuration.getInstance().smpNodeConfigs.get(i).hasRemoteCache()){
				RuntimeModel.getInstance().smpNodeModels.get(i).remoteDataCacheModel.removeAllEntries(pid);
			}
		}
		
		for(int i = 0;i<RuntimeModel.getInstance().smpNodeModels.size();i++){
			for(int j = 0;j<RuntimeModel.getInstance().smpNodeModels.get(i).cacheModelsMem.size();j++){
				RuntimeModel.getInstance().smpNodeModels.get(i).cacheModelsMem.get(j).removeAllEntries(pid);
			}
			for(int j = 0;j<RuntimeModel.getInstance().smpNodeModels.get(i).cacheModelsPT.size();j++){
				RuntimeModel.getInstance().smpNodeModels.get(i).cacheModelsPT.get(j).removeAllEntries(pid);
			}
		}
		for(int i = 0;i<RuntimeModel.getInstance().smpNodeModels.size();i++){
			for(int j = 0;j<RuntimeModel.getInstance().smpNodeModels.get(i).cpuToMemCacheModels.size();j++){
				if(RuntimeModel.getInstance().smpNodeModels.get(i).cpuToMemCacheModels.get(j).victimBufferModel!=null){
					RuntimeModel.getInstance().smpNodeModels.get(i).cpuToMemCacheModels.get(j).victimBufferModel.removeAllEntries(pid);
				}
			}
			for(int j = 0;j<RuntimeModel.getInstance().smpNodeModels.get(i).cpuToPTCacheModels.size();j++){
				if(RuntimeModel.getInstance().smpNodeModels.get(i).cpuToPTCacheModels.get(j).victimBufferModel!=null){
					RuntimeModel.getInstance().smpNodeModels.get(i).cpuToPTCacheModels.get(j).victimBufferModel.removeAllEntries(pid);
				}
			}	

		}	
		
	}

	// returns a natural number , a value<=0 means that the current process
	// should not be preempted
	private int goodness(ThreadProcDesc curProc, ThreadProcDesc oProc,
			boolean sameCPU) {
		int res = 0;
		// TODO calcres
		if (curProc.currentInstrNumber == curProc.instructions.size()) {
			res++;
		}
		return res;
	}

	private int goodness(ThreadProcDesc tpd, boolean sameCPU) {
		// TODO
		return tpd.priority + (sameCPU ? ThreadProcDesc.MAX_PRIORITY / 2 : 0);
	}

	public ThreadProcDesc getCurExecProcess(int smpNodeIndex, int numCPU) {
		P2 p2 = new P2(smpNodeIndex, numCPU);
		if (execProcs.get(p2) == null) {
			ThreadProcDesc tpd = selectExecProcess(smpNodeIndex, numCPU);
			if (tpd != null) {
				execProcs.put(p2, tpd);
				tpd.state = ThreadProcDesc.RUNNING_STATE;

			}
		}
		return execProcs.get(p2);
	}

	// calledfirst time, when no running process and when puttin a proc in
	// waiting state
	private ThreadProcDesc selectExecProcess(int smpNodeIndex, int numCPU) {
		StrLogger sl = RuntimeModel.getInstance().logger;
		int maxGoodness = -1;
		int n = 0;
		int goodness;
		ThreadProcDesc tpd;
		// get thread executing, with greatest priority

		// TODO other cpu can get a thread from other cpu in same SMP, must sync
		// the process..
		// for (int i = 0; i < procs.get(smpNodeIndex).size(); i++) {
		for (int k = 0, i = numCPU; k < procs.get(smpNodeIndex).get(i).size(); k++) {
			tpd = procs.get(smpNodeIndex).get(i).get(k);
			if (tpd.currentInstrNumber >= tpd.instructions.size()) {
				sl.addMessage(smpNodeIndex, numCPU,
						"process teminated , pid = " + tpd.pid + " , thread = "
								+ tpd.numberThread);
			} else {
				if (tpd.state == ThreadProcDesc.READY_STATE) {
					goodness = goodness(tpd, (i == numCPU));
					if (goodness > maxGoodness) {
						maxGoodness = goodness;
						n = 1;
					} else {
						if (goodness == maxGoodness) {
							n++;
						}
					}
				}
			}
		}
		// }
		int randInd = (int) (Math.random() * n);
		int t = 0;
		for (int i = 0; i < procs.get(smpNodeIndex).size(); i++) {
			for (int k = 0; k < procs.get(smpNodeIndex).get(i).size(); k++) {
				tpd = procs.get(smpNodeIndex).get(i).get(k);
				if (tpd.state == ThreadProcDesc.READY_STATE) {
					goodness = goodness(tpd, (i == numCPU));
					if (goodness == maxGoodness) {
						if (t == randInd) {
							return tpd;
						}
						t++;
					}
				}
			}
		}
		return null;
	}

	class ExtInstr extends Instr {

		String ra;

		public ExtInstr(Instr instr, String ra) {
			this.va = instr.va;
			this.type = instr.type;
			this.ra = ra;
		}

		@Override
		public String toString() {
			return super.toString() + "  - physical address: " + ra;
		}

	}

}
