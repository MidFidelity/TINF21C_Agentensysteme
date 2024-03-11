public abstract class Agent {

	public abstract boolean vote(int[] contract, int[] proposal);
	public abstract void    printUtility(int[] contract);
	public abstract int     getContractSize();

	public abstract boolean[] voteLoop (int[][] contracts, int acceptanceAmount);
	public abstract int voteEnd (int[][] contracts);

}
