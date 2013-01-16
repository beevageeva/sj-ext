package runtime.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import view.model.cache.CacheModel;



import config.model.Configuration;
import config.model.PageTableConfig;

public class RuntimeModel {

	private static RuntimeModel runtimeModel = null;
	public List<SMPNodeModel> smpNodeModels = new ArrayList<SMPNodeModel>();
	public PageTableModel pageTableModel;
	public Sched sched = new Sched();
	public ProcessMgrImpl procMgr = new ProcessMgrImpl();
	public StrLogger logger = new StrLogger();
	public CacheDirectory cacheDir = new CacheDirectory();
	

	
	
	private RuntimeModel(){
		for(int i = 0 ;i<Configuration.getInstance().smpNodeConfigs.size();i++){
			smpNodeModels.add(new SMPNodeModel(i));
		}
		if(Configuration.getInstance().pageTableConfig.getMappingType() == PageTableConfig.DIRECT_MAPPED_TYPE){
			pageTableModel = new DirectMappedPageTableModel() ;
		}
		else if(Configuration.getInstance().pageTableConfig.getMappingType() == PageTableConfig.INVERSE_MAPPED_TYPE){
			pageTableModel = new InverseMappedPageTableModel() ;
		}
		
	}
	
	public static RuntimeModel getInstance(){
		if(runtimeModel == null){
			runtimeModel = new RuntimeModel();
		}
		return runtimeModel;
	}
	
	public static void reset(){
		runtimeModel=null;
	}
	
	public P2 getCacheModelIndices(CacheModel cm){
		for(int i = 0 ;i<Configuration.getInstance().smpNodeConfigs.size();i++){
			for(int j = 0;j<Configuration.getInstance().smpNodeConfigs.get(i).cpuCachesToMem.size();j++){
				if(Configuration.getInstance().smpNodeConfigs.get(i).cpuCachesToMem.get(j).equals(cm)){
					return new P2(i,j);
				}
			}
		}
		return null;
	}

	
	

	
}
