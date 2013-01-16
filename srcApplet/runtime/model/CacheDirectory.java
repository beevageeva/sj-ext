package runtime.model;

import global.Helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import config.model.Configuration;

import view.model.cache.CacheListener;
import view.model.cache.CacheModel;

public class CacheDirectory extends AbstractTableModel{
	
	
	public static final short STATE_MODIFIED = 0;
	public static final short STATE_SHARED = 1;
	public static final short STATE_EXCLUSIVE = 2;
	public static final short STATE_OWNED = 3;
	public static final short STATE_INVALID = 4;
	
	
	
	

	class P2S1B1 extends P2 {
		short s;
		boolean b;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof P2S1B1) {
				P2S1B1 p2s1 = (P2S1B1) obj;
				return this.x == p2s1.x && this.y == p2s1.y && this.s == p2s1.s && this.b ==p2s1.b;
			}
			return false;
		}

	}
	
	
	public short getState(String binaryKey, int smpNodeIndex, int cacheIndex, boolean isVictimCache){
		List<P2S1B1> bcList = cacheDir.get(binaryKey);
		for(int i = 0;i<bcList.size();i++){
			if(bcList.get(i).x == smpNodeIndex &&  bcList.get(i).y == cacheIndex && bcList.get(i).b == isVictimCache){
				return bcList.get(i).s;
			}
		}
		return STATE_SHARED;
	}

	private Map<String, List<P2S1B1>> cacheDir = new HashMap<String, List<P2S1B1>>();

	public List<P2S1B1> lookup(String binaryKey, final short[] states) {
		List<P2S1B1> res = new ArrayList<P2S1B1>();
		List<P2S1B1> caches = cacheDir.get(binaryKey);
		P2S1B1 csp;
		boolean b;
		if (caches != null) {
			for (int i = 0; i < caches.size(); i++) {
				csp = caches.get(i);
				b = false;
				for (int j = 0; states != null && j < states.length && !b; j++) {
					if (states[i] == csp.s) {
						b = true;
					}
				}
				if (b || states == null) {
					res.add(csp);
				}
			}
		}
		return res;

	}

	public void put(final int smpNodeIndex, final int cacheIndex,
			final String binaryKey, final short state, boolean isVictimCache) {
		if (cacheDir.get(binaryKey) == null) {
			cacheDir.put(binaryKey, new ArrayList<P2S1B1>());
		}
		List<P2S1B1> plist = cacheDir.get(binaryKey);
		boolean found = false;
		for (int i = 0; i < plist.size() && !found; i++) {
			if (plist.get(i).x == smpNodeIndex && plist.get(i).y == cacheIndex) {
				plist.get(i).s = state;
			}
		}
		if (!found) {
			P2S1B1 p2s1 = new P2S1B1();
			p2s1.x = smpNodeIndex;
			p2s1.y = cacheIndex;
			p2s1.s = state;
			plist.add(p2s1);
			CacheModel cm = RuntimeModel.getInstance().smpNodeModels
					.get(smpNodeIndex).cacheModelsMem.get(cacheIndex);
			cm.addCacheListener(new CacheListener() {

				@Override
				public void entriesAccessed(int index, CacheModel cm) {
				}

				@Override
				public void entriesPut(int index, CacheModel cm) {

				}

				@Override
				public void entriesToBeEvicted(int index, CacheModel cm) {
					String cacheKeyString = Helper.convertDecimalToBinary(cm
							.getKeyAtIndex(index),
							Configuration.getInstance().smpNodeConfigs
									.get(smpNodeIndex).cacheConfigsMem.get(
									cacheIndex).getTotalNumberEntriesNBits());
					if (binaryKey.startsWith(cacheKeyString)) {
						List<P2S1B1> plist = cacheDir.get(binaryKey);
						boolean found = false;
						for (int i = 0; i < plist.size() && !found; i++) {
							if (plist.get(i).x == smpNodeIndex
									&& plist.get(i).y == cacheIndex) {
								plist.remove(i);
							}
						}

					}
				}
			});
		}
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public int getRowCount() {
		return cacheDir.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Iterator<String> it = cacheDir.keySet().iterator();
		int i = 0;
		String key = null;
		while(it.hasNext() && i<=rowIndex){
			key = it.next();
			i++;
		}
		if(key == null){
			return null;
		}
		if(columnIndex == 0){
			return key;
		}
		else if(columnIndex == 1){
			StringBuffer sb = new StringBuffer();
			List<P2S1B1> p2list = cacheDir.get(key);
			for(i = 0;i<p2list.size();i++){
				sb.append("[").append(p2list.get(i).x).append(",").append(p2list.get(i).y).append(",");
				switch(p2list.get(i).s){
				case STATE_INVALID:
					sb.append("I");
					break;	
				case STATE_SHARED:
					sb.append("S");
					break;	
				case STATE_EXCLUSIVE:
					sb.append("E");
					break;	
				case STATE_OWNED:
					sb.append("O");
					break;	
				case STATE_MODIFIED:
					sb.append("M");
					break;	
				}
				sb.append(",").append(p2list.get(i).b).append("] ");
			}
			return sb.toString();
		}
		return null;
	}

	@Override
	public String getColumnName(int column) {
		if(column == 0){
			return "binaryKey";
		}
		else if(column == 1){
			return "caches";
		}
		return null;
	}

}
