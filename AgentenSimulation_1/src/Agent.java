public abstract class Agent {

    private AgentIndexContract round_best;

    private AgentIndexContract global_best;

    public AgentIndexContract getRound_best() {
        return round_best;
    }

    public void setRound_best(AgentIndexContract round_best) {
        this.round_best = round_best;
    }

    public AgentIndexContract getGlobal_best() {
        return global_best;
    }

    public void setGlobal_best(AgentIndexContract global_best) {
        this.global_best = global_best;
    }

    public abstract boolean vote(Contract contract, Contract proposal);

    public abstract AgentIndexContract printUtility(Contract contract);

    public abstract int getContractSize();


    public abstract boolean[] voteLoop(Contract[] contracts, int acceptanceAmount);

    public abstract int voteEnd(Contract[] contracts);

}
