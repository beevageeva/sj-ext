package view.model.cache;



public interface AddColumns<E extends Entry> {

	
	public E createNewEntry();
	public String getColname(int col);
	public int getColCount();
	
	/**
	 * used when putting a new entry (there is no entry at this row)
	 * */
	public boolean isEligible(int row, int key, int pid, short instrType);
	
	/**
	 * used when looking for an entry to evict
	 * in some cases(such as local mem alloc)an entry acnnot be put in place of an entry
	 * of another process
	 * */
	public boolean isEligible(E entry, int key, int pid, short instrType);
	
	/**
	 * the value that will be sum when looking for an entry to evict
	 * it will evict the entry eligible with max of this sum value
	 * */
	public int getEvictionValue(E entry);
	
	public void setValues(E entry, int pid, short instrType, int ... keys);
	
	
	
}
