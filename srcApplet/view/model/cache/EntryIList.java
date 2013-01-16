package view.model.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import runtime.model.P2;


public class EntryIList extends EntryI{
	private List<P2> qs;
	public int indexList = -1;
	
	public EntryIList(){
		qs = new ArrayList<P2>();
	}
	
	@Override
	public Object getValueAt(int col) {
		if(col==1){
			StringBuffer sb = new StringBuffer();
			for(int i = 0; i<qs.size()-1;i++){
				sb.append(qs.get(i).x+"/" + qs.get(i).y);
				if(i==indexList){
					sb.append("(*)");
				}
				sb.append(",");
			}
			sb.append(qs.get(qs.size()-1).x + "/" + qs.get(qs.size()-1).y);
			if(qs.size() -1 ==indexList){
				sb.append("(*)");
			}
			return sb.toString();
		}
		return super.getValueAt(col);
	}
	
	public int getIndexInVPNArray(int vpn, int pid){
		for(int i = 0;i<qs.size();i++){
			if(qs.get(i).x == vpn && qs.get(i).y ==pid){
				return i;
			}
		}
		return -1;
	}
	
	public P2 getVPNAtIndex(int index){
		return qs.get(index);
	}
	
	public void addVPNInArray(int vpn, int pid){
		P2 p2 = new P2();
		p2.x = vpn;
		p2.y = pid;
		qs.add(p2);
		
	}
	
	
	public void removeEntriesByPid(int pid){
		Iterator<P2> it = qs.iterator();
		P2 p2;
		while(it.hasNext()){
			p2 = it.next();
			if(p2.y == pid){
				qs.remove(p2);
			}
		}
	}
	

}
