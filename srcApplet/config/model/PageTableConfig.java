package config.model;

public class PageTableConfig {

	public static final  short DIRECT_MAPPED_TYPE=0;
	public static final  short INVERSE_MAPPED_TYPE=1;
	public static final  short NO_MAPPING=2;
	
	private Object mappingConfig;
	public int accessTimeUnits = 1;
	
	public Object getMappingConfig(){
		return mappingConfig;
	}
	
	public PageTableConfig(){
		setMappingType(DIRECT_MAPPED_TYPE);
	}
	
	public void setMappingType(short type){
		if(type == DIRECT_MAPPED_TYPE){
			mappingConfig = new DirectMappedPageTableConfig();
		}
		else if(type == INVERSE_MAPPED_TYPE){
			mappingConfig = new InverseMappedPageTableConfig(); 
		}
		else{
			throw new UnsupportedOperationException("Try to set mapping Type to "+type);
		}
	}
	
	public short getMappingType(){
		if(mappingConfig==null){
			return NO_MAPPING;
		}
		else if(mappingConfig instanceof DirectMappedPageTableConfig){
			return DIRECT_MAPPED_TYPE;
		}
		else if(mappingConfig instanceof InverseMappedPageTableConfig){
			return INVERSE_MAPPED_TYPE;
		}
		return NO_MAPPING;

	}
	
	
	public 	class DirectMappedPageTableConfig{

		private int[] offsetsLength = new int[]{10,10};

		private boolean searchMethodTopDown;

		public int[] getOffsetsLength() {
			return offsetsLength;
		}

		public void setOffsetsLength(int[] offsetsLength) {
			this.offsetsLength = offsetsLength;
		}

		public boolean isSearchMethodTopDown() {
			return searchMethodTopDown;
		}

		public void setSearchMethodTopDown(boolean searchMethodTopDown) {
			this.searchMethodTopDown = searchMethodTopDown;
		}

		/*
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("Search Method : ");
			sb.append(searchMethodTopDown?"TOP-DOWN" : "BOTTOM-UP");
			sb.append("\noffsets lengths : ");
			for(int i = 0 ; i<offsetsLength.length-1;i++){
				sb.append(offsetsLength[i]);
				sb.append(",");
			}
			sb.append(offsetsLength[offsetsLength.length-1]);
			return sb.toString();
		}
		*/
		
	}

	public 	class InverseMappedPageTableConfig {
		private int hashAnchorSizeNBits = 1;
		
		public int getHashAnchorSizeNBits() {
			return hashAnchorSizeNBits;
		}

		public void setHashAnchorSizeNBits(int hashAnchorSizeNBits) {
			this.hashAnchorSizeNBits = hashAnchorSizeNBits;
		}

		/*
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("Hash anchor size = ");
			sb.append("2 ** ");
			sb.append(hashAnchorSizeNBits);
			return sb.toString();
		}
	*/
	}

	
}
