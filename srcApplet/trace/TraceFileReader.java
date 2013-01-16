package trace;

import global.Helper;
import global.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class TraceFileReader {


	public static List<Instr> readInstructions(InputStream is, int virtualAddressNBits)
			throws IOException {
		List<Instr> instructions = new ArrayList<Instr>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		int i1, i2;
		int value;
		String type;
		int i = 0;
		Instr instr;
		String addressString;
		while ((line = reader.readLine()) != null) {
			i++;
			// add1
			i1 = 0;
			while (Character.isWhitespace(line.charAt(i1))) {
				i1++;
			}
			i2 = i1;
			while (Character.isLetterOrDigit(line.charAt(i2))) {
				i2++;
			}
			addressString = line.substring(i1, i2);
			if (addressString.length() * 4 > virtualAddressNBits) {
				Logger.log("address of line " + i + " must be in  "
						+ virtualAddressNBits + " b , will be trunc");
				addressString = addressString.substring(addressString.length()
						- virtualAddressNBits / 4);
			}
			else if(addressString.length() * 4 < virtualAddressNBits){
				for(int d = 0 ; d<virtualAddressNBits/4 ; d++){
					addressString = "0"+addressString; 
				}
			}
			try {
				value = Integer.parseInt(addressString, 16);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				Logger.log("line " + i + " of file ");
				value = -1;
			}
			instr = new Instr();
			instructions.add(instr);
			instr.va = Helper.convertDecimalToBinary(value,
					virtualAddressNBits);
			// type
			i1 = i2;
			while (Character.isWhitespace(line.charAt(i1))) {
				i1++;
			}
			i2 = i1;
			while (Character.isLetterOrDigit(line.charAt(i2))) {
				i2++;
			}
			type = line.substring(i1, i2);
			if (type.equalsIgnoreCase("FETCH")) {
				instr.type = Instr.FETCHINSTR;
			} else if (type.equalsIgnoreCase("MEMREAD")) {
				instr.type = Instr.READDATA;
			} else if (type.equalsIgnoreCase("MEMWRITE")) {
				instr.type = Instr.MODIFDATA;
			} else {
				Logger.log("line " + i + " of file ");
			}
			// time not used
			i1 = i2;
			while (Character.isWhitespace(line.charAt(i1))) {
				i1++;
			}
			i2 = i1;
			while (i2 < line.length()
					&& Character.isLetterOrDigit(line.charAt(i2))) {
				i2++;
			}
			try {
				value = Integer.parseInt(line.substring(i1, i2), 10);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				Logger.log("line " + i + " of file ");
				value = -1;
				break;
			}

		}
		return instructions;
	}

}
