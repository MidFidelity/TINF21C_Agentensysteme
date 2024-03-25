public class AgentIndexContract extends Contract {
    final int index;
    int costs;

    public AgentIndexContract( int index, int[] contract ) {
        super(contract);
        this.index = index;
    }

    public AgentIndexContract( int index, Contract contract ) {
        super(contract.getContract());
        this.index = index;
    }
}
