package runtime.model;



public class P3 extends P2{
	int z;
	
	public P3(){}
	public P3(int x , int y , int z){
		super(x , y);
		this.z = z;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof P3)){
			return false;
		}
		P3 p3 = (P3)obj;
		return x==p3.x && y == p3.y && z==p3.z; 
	}
	
}