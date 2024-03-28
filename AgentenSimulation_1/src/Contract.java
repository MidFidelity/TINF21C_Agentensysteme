import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

public class Contract {
    private int[] contract;
    private static final SplittableRandom rand = new SplittableRandom();


    public Contract(int[] contract) {
        this.contract = contract;
    }

    public Contract(List<Integer> contract) {
        this.contract = contract.stream().mapToInt(i -> i).toArray();
    }

    public Contract(int size) {
        this.contract = new int[size];
    }

    public int[] getContract() {
        return contract;
    }

    public int getContractSize() {
        return contract.length;
    }

    public void setContract(int[] contract) {
        this.contract = contract;
    }

    public void mutate() {
        int[] contractArr = this.contract;

        int start = rand.nextInt((contractArr.length - 1));
        int end = start + 1;
        int temp = contractArr[start];
        contractArr[start] = contractArr[end];
        contractArr[end] = temp;
    }

    public Contract[] crossover(Contract contract){
        return Crossover.cxOrdered(this, contract);
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
