import java.util.Arrays;

public class Contract {
    private int[] contract;

    public int[] getContract() {
        return contract;
    }

    public void setContract(int[] contract) {
        this.contract = contract;
    }

    public Contract(int[] contract){
        this.contract = contract;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contract contract1 = (Contract) o;
        return Arrays.equals(contract, contract1.contract);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(contract);
    }
}
