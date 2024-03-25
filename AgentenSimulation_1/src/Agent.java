public abstract class Agent {

	public abstract boolean vote(Contract contract, Contract proposal);
	public abstract void    printUtility(Contract contract);
	public abstract int     getContractSize();

	public abstract boolean[] voteLoop (Contract[] contracts, int acceptanceAmount);
	public abstract int voteEnd (Contract[] contracts);

}
