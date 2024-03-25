public class AgentIndexContract {
    final int index;
    int costs;
    final int[] contracts;

    public AgentIndexContract( int index, int[] contracts ) {
        this.index = index;
        this.contracts = contracts;
    }
}
