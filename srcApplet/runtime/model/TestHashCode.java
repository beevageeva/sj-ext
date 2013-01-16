package runtime.model;

import java.util.ArrayList;
import java.util.List;

public class TestHashCode {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Integer> l1 = new ArrayList<Integer>();
		l1.add(new Integer(1));
		l1.add(new Integer(1));
		List<Integer> l2 = new ArrayList<Integer>();
		l2.add(new Integer(962));
		System.out.println("l1 hashcode = "+l1.hashCode());
		System.out.println("l2 hashcode = "+l2.hashCode());
	}

}
