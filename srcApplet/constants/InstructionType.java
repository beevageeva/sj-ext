package constants;

public class InstructionType {

	public static final short WRITE_DATA_TYPE=0;
	public static final short READ_DATA_TYPE=1;
	public static final short FETCH_INSTR_TYPE=2;
	
	public static String getString(short type){
		switch(type){
		case WRITE_DATA_TYPE:
			return "WRITE";
		case READ_DATA_TYPE:
			return "READ";
		case FETCH_INSTR_TYPE:
			return "FETCH";
		}
		return null;
		
	}
	
}
