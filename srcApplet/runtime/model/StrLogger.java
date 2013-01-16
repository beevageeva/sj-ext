package runtime.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import messages.Messages;

import runtime.model.Sched.SyncObj;

import config.model.Configuration;
import config.model.SMPNodeConfig;

public class StrLogger {

	
	private List<Sync>[] syncObjs;
	private List<SyncMessage> syncMsgs;
	private static int[] duration;
	private static String[] names;
	
	
	static{
		//PT, SMP1, SMP2, ..
		//SMP : MM, remote cache if any ,  all PTs, victim buffer if any, all MEMs,victim buffer if any
		//PT|MEM : pt|mem , victim cache if any
		//if the order change , change methods access caches
		int n = getNumberSyncObjectsUntil(Configuration.getInstance().smpNodeConfigs.size());
		duration = new int[n];
		names = new String[n];
		SMPNodeConfig smpNodeCfg;
		int index = 1;
		duration[0] = Configuration.getInstance().pageTableConfig.accessTimeUnits;
		names[0] = Messages.getText("pt");
		for(int i = 0;i<Configuration.getInstance().smpNodeConfigs.size();i++){
			smpNodeCfg = Configuration.getInstance().smpNodeConfigs.get(i);
			duration[index] = smpNodeCfg.mainMemoryConfig.getAccessTimeUnits();
			names[index] = Messages.getText("mm");
			index++;
			if(smpNodeCfg.hasRemoteCache()){
				duration[index] = smpNodeCfg.remoteDataCache.getAccessTimeUnits();
				names[index] = Messages.getText("remote_cache");
				index++;
			}
			//pt caches
			for(int k = 0;k< smpNodeCfg.cacheConfigsPT.size();k++){
				duration[index] = smpNodeCfg.cacheConfigsPT.get(k).getAccessTimeUnits();
				names[index] = Messages.getText("pt_cache")+" (nodo " +i+ " , número " + k + " )";
				index++;
				//pt victim cache
				if(smpNodeCfg.cacheConfigsPT.get(k).hasVictimCache()){
					duration[index] = smpNodeCfg.cacheConfigsPT.get(k).victimCacheCfg.getAccessTimeUnits();
					names[index] = Messages.getText("pt_victim_cache")+" (nodo "+i+ " , número " + k + " )";
					index++;
				}
			}
			//for all cpus
			for(int k = 0;k< smpNodeCfg.cpuCachesToPT.size();k++){
				//add victim buffer if exists exclusive cache 
				if(smpNodeCfg.cpuCachesToPT.get(k).hasExclusiveCache()){
					duration[index] = smpNodeCfg.cpuCachesToPT.get(k).exclusiveCacheCfg.getAccessTimeUnits();
					names[index] = Messages.getText("pt_victim_buffer")+" (nodo " +i+ " , número "+k + " )";
					index++;
				}
			}
			//Mem caches
			for(int k = 0;k< Configuration.getInstance().smpNodeConfigs.get(i).cacheConfigsMem.size();k++){
				duration[index] = Configuration.getInstance().smpNodeConfigs.get(i).cacheConfigsMem.get(k).getAccessTimeUnits();
				names[index] = Messages.getText("mem_cache")+" (nodo " +i+ " , número "+k+ " )";
				index++;
				if(Configuration.getInstance().smpNodeConfigs.get(i).cacheConfigsMem.get(k).hasVictimCache()){
					duration[index] = Configuration.getInstance().smpNodeConfigs.get(i).cacheConfigsMem.get(k).victimCacheCfg.getAccessTimeUnits();
					names[index] = Messages.getText("mem_victim_cache")+" (nodo " +i+ " , número "+k + " )";
					index++;
				}
			}
			for(int k = 0;k< smpNodeCfg.cpuCachesToMem.size();k++){
				if(smpNodeCfg.cpuCachesToMem.get(k).hasExclusiveCache()){
					duration[index] = smpNodeCfg.cpuCachesToMem.get(k).exclusiveCacheCfg.getAccessTimeUnits();
					names[index] = Messages.getText("mem_victim_buffer")+" (nodo " +i+ " , número "+k+ " )";
					index++;
				}
			}
		}
	}
	
	static short[] selectableDebugTypes = new short[]{SyncMessage.DEBUG_1,SyncMessage.DEBUG_2,SyncMessage.DEBUG_3,SyncMessage.DEBUG_4};
	
	private static int getNumberSyncObjects(int smpNodeIndex){
		SMPNodeConfig smpNodeCfg = Configuration.getInstance().smpNodeConfigs.get(smpNodeIndex);
		int n = 1 + smpNodeCfg.cacheConfigsPT.size() +smpNodeCfg.cacheConfigsMem.size();
		if(smpNodeCfg.hasRemoteCache()){
			n++;
		}
		for(int j = 0;j<smpNodeCfg.cacheConfigsPT.size();j++){
			if(smpNodeCfg.cacheConfigsPT.get(j).hasVictimCache()){
				n++;
			}
		}
		for(int i = 0;i<smpNodeCfg.cpuCachesToPT.size();i++){
			if(smpNodeCfg.cpuCachesToPT.get(i).hasExclusiveCache()){
				n++;
			}
		}

		for(int j = 0;j<smpNodeCfg.cacheConfigsMem.size();j++){
			if(smpNodeCfg.cacheConfigsMem.get(j).hasVictimCache()){
				n++;
			}
		}
		for(int i = 0;i<smpNodeCfg.cpuCachesToMem.size();i++){
			if(smpNodeCfg.cpuCachesToMem.get(i).hasExclusiveCache()){
				n++;
			}
		}
		return n;
		
	}
	
	
	private static int getNumberSyncObjectsUntil(int smpNodeIndex){
		int n = 1;
		for(int i = 0;i<smpNodeIndex;i++){
			n += getNumberSyncObjects(i); 
		}
		return n;
	}
	
	
	
	public StrLogger(){
		int n = getNumberSyncObjectsUntil(Configuration.getInstance().smpNodeConfigs.size());
		syncObjs = new List[n];
		for(int i = 0;i<n;i++){
			syncObjs[i] = new ArrayList<Sync>();
		}
		syncMsgs = new ArrayList<SyncMessage>();
	}
	
	
	//search all and delay for specified smpNode and numCPU with startTime> startTime that comes as parameter
	private void delayAllAfter(int smpNodeIndex, int numCPU, int startTime, int delay, List<P2> visited){
		for(int i = 0;i<syncObjs.length;i++){
			for(int j=0;j<syncObjs[i].size();j++){
				if(syncObjs[i].get(j).fromSMPNodeIndex == smpNodeIndex && syncObjs[i].get(j).fromNumCPU == numCPU && syncObjs[i].get(j).startTime>=startTime){
					if(!visited.contains(new P2(i,j))){
						syncObjs[i].get(j).startTime+=delay;
						addMessage(smpNodeIndex, numCPU, "DELAY "+delay, syncObjs[i].get(j).startTime, SyncMessage.DEBUG_1);
						visited.add(new P2(i,j));
						//check if caused by the delay must delay all after the array
						if(j<syncObjs[i].size()-1 && syncObjs[i].get(j+1).startTime<syncObjs[i].get(j).startTime+duration[i]){
							delayInArray(i, j+1, syncObjs[i].get(j).startTime+duration[i] - syncObjs[i].get(j+1).startTime, visited);
						}
					}
				}
			}
		}
		for(int i = 0;i<syncMsgs.size();i++){
			if(syncMsgs.get(i).fromSMPNodeIndex == smpNodeIndex && syncMsgs.get(i).fromNumCPU == numCPU && syncMsgs.get(i).startTime>=startTime){
				syncMsgs.get(i).startTime+=delay;
			}
		}

	}
	
	private void delayInArray(int arrayIndex, int indexStart, int delay, List<P2> visited){
		for(int i = indexStart;i<syncObjs[arrayIndex].size();i++){
			delayAllAfter(syncObjs[arrayIndex].get(i).fromSMPNodeIndex, syncObjs[arrayIndex].get(i).fromNumCPU, syncObjs[arrayIndex].get(i).startTime, delay, visited);
		}
	}

//	private int getLastStartTime(int smpNodeIndex, int numCPU){
//		int maxStartTime = 0;
//		boolean found;
//		for(int i = 0;i<syncObjs.length;i++){
//			found = false;
//			for(int j=syncObjs[i].size()-1 ;j>=0 && !found;j--){
//				if(syncObjs[i].get(j).fromSMPNodeIndex == smpNodeIndex && syncObjs[i].get(j).fromNumCPU==numCPU){
//					found = true;
//					if(maxStartTime<syncObjs[i].get(j).startTime+duration[i]){
//						maxStartTime=syncObjs[i].get(j).startTime+duration[i];
//					}
//				}
//			}
//		}	
//		return maxStartTime;
//	}

	public void addMessage(int fromSMPNodeIndex, int fromNumCPU ,String msg,int startTime, short debugType){
		int i;
		for(i = 0;i<syncMsgs.size() && startTime>syncMsgs.get(i).startTime;i++){
		}
		SyncMessage sm = new SyncMessage();
		sm.fromNumCPU = fromNumCPU;
		sm.fromSMPNodeIndex = fromSMPNodeIndex;
		sm.startTime = startTime;
		sm.debugType = debugType;
		sm.msg = msg;
		syncMsgs.add(i,sm);
	}
	
	public void addMessage(int smpNodeIndexFrom, int numCPUFrom, String msg){
		addMessage(smpNodeIndexFrom, numCPUFrom,msg, RuntimeModel.getInstance().sched.getCurrentDuration(), SyncMessage.DEBUG_2);
	}

	
	public int accessPT(int smpNodeIndexFrom, int numCPUFrom, int startTime){
		return access(0,smpNodeIndexFrom, numCPUFrom, startTime);
	}
	
	
	public int accessMainMem(int smpIndexFrom, int numCPUFrom, int smpIndexTo, int startTime){
		return access(getNumberSyncObjectsUntil(smpIndexTo),smpIndexFrom,numCPUFrom, startTime);
	}
	
	public int accessRemoteCache(int smpIndexFrom, int numCPUFrom, int smpIndexTo, int startTime){
		if(!Configuration.getInstance().smpNodeConfigs.get(smpIndexFrom).hasRemoteCache()){
			System.out.println("********ACCEES REMOTE CACHE IN A NODE THAT DOES NOT HAVE REMOTE CACHE***********");
			return 0;
		}
		return access(getNumberSyncObjectsUntil(smpIndexTo) + 1,smpIndexFrom,numCPUFrom, startTime);
	}
	
	
	public int accessPTCache(int smpIndexFrom, int numCPUFrom, int smpIndexTo, int numCPUTo, int cacheIndex, int startTime){
		int n = getNumberSyncObjectsUntil(smpIndexTo);
		n++;
		SMPNodeConfig smpNodeCfg=Configuration.getInstance().smpNodeConfigs.get(smpIndexTo);
		if(smpNodeCfg.hasRemoteCache()){
			n++;
		}
		for(int i = 0;i<smpNodeCfg.getIndexInPTCacheCfgs(numCPUTo, cacheIndex);i++){
			n++;
			if(smpNodeCfg.cacheConfigsPT.get(i).hasVictimCache()){
				n++;
			}
		}
		return access(n,smpIndexFrom,numCPUFrom, startTime);
	}
	

	public int accessMemCache(int smpIndexFrom, int numCPUFrom, int smpIndexTo, int numCPUTo, int cacheIndex, int startTime){
		int n = getNumberSyncObjectsUntil(smpIndexTo);
		SMPNodeConfig smpNodeCfg=Configuration.getInstance().smpNodeConfigs.get(smpIndexTo);
		n+=1+smpNodeCfg.cacheConfigsPT.size();
		if(smpNodeCfg.hasRemoteCache()){
			n++;
		}
		for(int j = 0;j<smpNodeCfg.cacheConfigsPT.size();j++){
			if(smpNodeCfg.cacheConfigsPT.get(j).hasVictimCache()){
				n++;
			}
		}
		for(int i = 0;i<smpNodeCfg.cpuCachesToPT.size();i++){
			if(smpNodeCfg.cpuCachesToPT.get(i).hasExclusiveCache()){
				n++;
			}
		}
		for(int j = 0;j<smpNodeCfg.getIndexInMemCacheCfgs(numCPUTo, cacheIndex);j++){
			n++;
			if(smpNodeCfg.cacheConfigsMem.get(j).hasVictimCache()){
				n++;
			}
		}
		return access(n,smpIndexFrom,numCPUFrom, startTime);
	}
	

	public int accessPTVictimCache(int smpIndexFrom, int numCPUFrom, int smpIndexTo, int numCPUTo, int cacheIndex, int startTime){
		int n = getNumberSyncObjectsUntil(smpIndexTo);
		n++;
		SMPNodeConfig smpNodeCfg=Configuration.getInstance().smpNodeConfigs.get(smpIndexTo);
		if(smpNodeCfg.hasRemoteCache()){
			n++;
		}
		for(int i = 0;i<smpNodeCfg.getIndexInPTCacheCfgs(numCPUTo, cacheIndex);i++){
			n++;
			if(smpNodeCfg.cacheConfigsPT.get(i).hasVictimCache()){
				n++;
			}
		}
		n++;
		return access(n,smpIndexFrom,numCPUFrom, startTime);
		
	}
	

	public int accessMemVictimCache(int smpIndexFrom, int numCPUFrom, int smpIndexTo, int numCPUTo, int cacheIndex, int startTime){
		int n = getNumberSyncObjectsUntil(smpIndexTo);
		SMPNodeConfig smpNodeCfg=Configuration.getInstance().smpNodeConfigs.get(smpIndexTo);
		n+=1+smpNodeCfg.cacheConfigsPT.size();
		if(smpNodeCfg.hasRemoteCache()){
			n++;
		}
		for(int j = 0;j<smpNodeCfg.cacheConfigsPT.size();j++){
			if(smpNodeCfg.cacheConfigsPT.get(j).hasVictimCache()){
				n++;
			}
		}
		for(int i = 0;i<smpNodeCfg.cpuCachesToPT.size();i++){
			if(smpNodeCfg.cpuCachesToPT.get(i).hasExclusiveCache()){
				n++;
			}
		}
		for(int j = 0;j<smpNodeCfg.getIndexInMemCacheCfgs(numCPUTo, cacheIndex);j++){
			n++;
			if(smpNodeCfg.cacheConfigsMem.get(j).hasVictimCache()){
				n++;
			}
		}
		n++;
		return access(n,smpIndexFrom,numCPUFrom, startTime);
		
	}
	
	public int accessPTExcBufferCache(int smpIndexFrom, int numCPUFrom, int smpIndexTo, int numCPUTo, int startTime){
		int n = getNumberSyncObjectsUntil(smpIndexTo);
		SMPNodeConfig smpNodeCfg=Configuration.getInstance().smpNodeConfigs.get(smpIndexTo);
		n+=1+smpNodeCfg.cacheConfigsPT.size();
		if(smpNodeCfg.hasRemoteCache()){
			n++;
		}
		for(int j = 0;j<smpNodeCfg.cacheConfigsPT.size();j++){
			if(smpNodeCfg.cacheConfigsPT.get(j).hasVictimCache()){
				n++;
			}
		}
		for(int i = 0;i<numCPUTo;i++){
			if(smpNodeCfg.cpuCachesToPT.get(i).hasExclusiveCache()){
				n++;
			}
		}
		return access(n,smpIndexFrom,numCPUFrom, startTime);
	}

	
	public int accessMemExcBufferCache(int smpIndexFrom, int numCPUFrom, int smpIndexTo, int numCPUTo, int startTime){
		int n = getNumberSyncObjectsUntil(smpIndexTo);
		SMPNodeConfig smpNodeCfg=Configuration.getInstance().smpNodeConfigs.get(smpIndexTo);
		n+=1 + smpNodeCfg.cacheConfigsPT.size() +smpNodeCfg.cacheConfigsMem.size();
		if(smpNodeCfg.hasRemoteCache()){
			n++;
		}
		for(int j = 0;j<smpNodeCfg.cacheConfigsPT.size();j++){
			if(smpNodeCfg.cacheConfigsPT.get(j).hasVictimCache()){
				n++;
			}
		}
		for(int i = 0;i<smpNodeCfg.cpuCachesToPT.size();i++){
			if(smpNodeCfg.cpuCachesToPT.get(i).hasExclusiveCache()){
				n++;
			}
		}

		for(int j = 0;j<smpNodeCfg.cacheConfigsMem.size();j++){
			if(smpNodeCfg.cacheConfigsMem.get(j).hasVictimCache()){
				n++;
			}
		}
		for(int i = 0;i<numCPUTo;i++){
			if(smpNodeCfg.cpuCachesToMem.get(i).hasExclusiveCache()){
				n++;
			}
		}
		return access(n,smpIndexFrom,numCPUFrom, startTime);
	}
	
	
	
	private int access(int arrayIndex, int smpNodeIndexFrom, int numCPUFrom, int startTime){
		int i;
		for(i = 0;i<syncObjs[arrayIndex].size() && startTime>syncObjs[arrayIndex].get(i).startTime;i++){
		}
		//must be put after i-1 and before i
		Sync so = new Sync();
		so.fromNumCPU = numCPUFrom;
		so.fromSMPNodeIndex = smpNodeIndexFrom;
		so.startTime = startTime;
		syncObjs[arrayIndex].add(i,so);
		int delay=0;
		if(i>0 && startTime<syncObjs[arrayIndex].get(i-1).startTime + duration[arrayIndex]){
			delay = syncObjs[arrayIndex].get(i-1).startTime + duration[arrayIndex]-startTime;
			delayAllAfter(smpNodeIndexFrom, numCPUFrom, startTime, delay, new ArrayList<P2>());
		}

		if(i<syncObjs[arrayIndex].size()){
			//check current elem i 
			if(syncObjs[arrayIndex].get(i).startTime<startTime + duration[arrayIndex]){
				delay+=startTime + duration[arrayIndex]-syncObjs[arrayIndex].get(i).startTime;
				
			}
			if(delay>0){
				delayInArray(arrayIndex, i+1, delay, new ArrayList<P2>());
			}
		}
		return so.startTime + duration[arrayIndex];
	}
	
	
	
	public String getMessages(String filter){
		StringBuffer sb = new StringBuffer();
		SyncMessage  sm;
		Pattern pattern = null;
		if(filter!=null && !filter.trim().equals("")){
			pattern = Pattern.compile(filter);
		}
		for(int i = 0;i<syncMsgs.size();i++){
			sm = syncMsgs.get(i);
			if(pattern==null || pattern.matcher(sm.msg).find()){
				sb.append("\n[");
				sb.append(sm.startTime);
				sb.append("][");
				sb.append(sm.fromSMPNodeIndex);
				sb.append("][");
				sb.append(sm.fromNumCPU);
				sb.append("] : ");
				sb.append(sm.msg);
			}
		}
		return sb.toString();
	}
	
	public String getSyncObjMessages(){
		StringBuffer sb = new StringBuffer();
		Sync s;
		for(int i = 0;i<syncObjs.length;i++){
			sb.append("\n-------------------------\n");
			sb.append(Messages.getText("access_to"));
			sb.append(" ");
			sb.append(names[i]);
			sb.append(" - Duration : ");
			sb.append(duration[i]);
			for(int j = 0;j<syncObjs[i].size();j++){
				s = syncObjs[i].get(j);
				sb.append("\n[");
				sb.append(s.startTime);
				sb.append("]");
				sb.append(s.fromSMPNodeIndex);
				sb.append(" , ");
				sb.append(s.fromNumCPU);
				
			}
		}
		return sb.toString();
	}

	public void reset(){
		for(int i = 0;i<syncObjs.length;i++){
			syncObjs[i].clear();
		}
		syncMsgs.clear();
	}
	
	class Sync{
		
		int startTime;
		int fromSMPNodeIndex;
		int fromNumCPU;
		
	}
	
	class SyncMessage extends Sync{
		static final short DEBUG_1 = 0;
		static final short DEBUG_2 = 1;
		static final short DEBUG_3 = 2;
		static final short DEBUG_4 = 3;
		
		
		String msg;
		short debugType;
	}
	
}
	
	
	
	
