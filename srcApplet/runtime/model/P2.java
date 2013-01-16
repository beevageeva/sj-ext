package runtime.model;



public class P2{

	public static final int MAX_Y = 10;
	public int x;
	public int y;

	public P2(){}
	
	public P2(int x, int y ){
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof P2)){
			return false;
		}
		P2 p2 = (P2)obj;
		return x==p2.x && y == p2.y; 
	}
	
    @Override
	public int hashCode() {
    	return MAX_Y*x+y;
    }

	
}
