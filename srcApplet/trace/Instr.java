package trace;

import messages.Messages;


public class Instr {
	
	public static final short MODIFDATA = 0;
	public static final short READDATA = 1;
	public static final short FETCHINSTR = 2;


	public Instr(){}
	
	public Instr(short type, String addressBinaryRepr) {
		this.type = type;
		this.va = addressBinaryRepr;
	}

	public short type;

	public String va;

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getLabelText(type));
		sb.append(" ");
		sb.append(Integer.parseInt(va, 2));
		sb.append("(");
		sb.append(va);
		sb.append(")");
		return sb.toString();
	}
	
	private String getLabelText(short type){
		switch(type){
		case MODIFDATA:
			return Messages.getText("instruction.modif_data_type");
		case READDATA:
			return Messages.getText("instruction.read_data_type");
		case FETCHINSTR:
			return Messages.getText("instruction.fetch_instr_type");
		}
		return Messages.getText("instruction.unimpl_type");
	}
	
	
	
}
