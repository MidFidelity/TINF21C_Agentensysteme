public class AgentIndexContract extends Contract {

    private final int index;

    private int cost;

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getIndex() {
        return index;
    }

    public AgentIndexContract(int index, int[] contract) {
        super(contract);
        this.index = index;
    }

    public AgentIndexContract(int index, Contract contract) {
        super(contract.getContract());
        this.index = index;
    }
}
