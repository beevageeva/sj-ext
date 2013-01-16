package global;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Helper {

	public void loadFileStream(InputStream is, OutputStream dOut)
			throws FileNotFoundException, IOException {
		byte[] byteBuff = null;
		try {
			int numBytes = 0;
			byteBuff = new byte[1024];
			while (-1 != (numBytes = is.read(byteBuff))) {
				dOut.write(byteBuff, 0, numBytes);
			}
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
			byteBuff = null;
		}
	}
	
	public static String convertDecimalToBinary(int iNumber, int numberOfBits){
		String bin = "";
		while (iNumber>0) {
			if (iNumber%2!=0) {
				bin = "1"+bin;
			} else {
				bin = "0"+bin;
			}
			iNumber = (int) Math.floor(iNumber/2);
		}
		// left pad with zeros
		if(bin.length() > numberOfBits){
			Logger.log("string does not fit : binary repr  length = " + bin.length() + " , numberBits = " + numberOfBits);
			return  null;
		}
		while (bin.length()<numberOfBits) {
			bin = "0"+bin;
		}
		return bin;
	}
	
	public static int getIntValueFromBinaryRepr(int dec,int ind1, int ind2, int numBits ){
		String br = convertDecimalToBinary(dec, numBits);
		return Integer.parseInt(br.substring(ind1, ind2), 2);
	}
	
	public static int getPowerOf2(int n){
		int k = 0;
		while(n!=1){
			n/=2;
			k++;
		}
		return k;
	}


}
