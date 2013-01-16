package view.model.cache;


public class PidExtAddColumns extends PidAddColumns{

	@Override
	public boolean isEligible(EntryI entry, int key, int pid, short instrType) {
		return entry.p == pid;
	}

}
