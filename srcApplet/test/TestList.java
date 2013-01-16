package test;

import java.util.ArrayList;
import java.util.List;

public class TestList {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		List<String> list = new ArrayList<String>();
		for(int i = 0;i<10;i++){
			list.add(String.valueOf(i));
		}
		printList(list);
		list.remove(3);
		list.remove(5-1);
		list.remove(7-2);
		printList(list);
	}
	
	private static void printList(List<String> l){
		System.out.println("SIZE = "+l.size());
		for(int i = 0;i<l.size();i++){
			System.out.println(l.get(i)+" ");
		}
	}

}
