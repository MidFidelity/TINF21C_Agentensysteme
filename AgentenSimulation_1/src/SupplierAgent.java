import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SupplierAgent extends Agent {

    private int[][] costMatrix;

    public SupplierAgent(File file) throws FileNotFoundException {

        Scanner scanner = new Scanner(file);
        int dim = scanner.nextInt();
        costMatrix = new int[dim][dim];
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                int x = scanner.nextInt();
                costMatrix[i][j] = x;
            }
        }
        scanner.close();
    }

    public boolean vote(int[] contract, int[] proposal) {
        int costContract = evaluate(contract);
        int costProposal = evaluate(proposal);
        if (costProposal < costContract)
            return true;
        else
            return false;
    }

    public int getContractSize() {
        return costMatrix.length;
    }

    @Override
    public boolean[] voteLoop(int[][] contracts, final int acceptanceAmount) {
        Map<Integer, Integer> costs = new LinkedHashMap<>();
        for (int i = 0; i < contracts.length; i++) {
            costs.put(i, evaluate(contracts[i]));
        }

        costs = sortByValue(costs);

        boolean[] result = new boolean[contracts.length];
        AtomicInteger count = new AtomicInteger();

        costs.forEach((integer, integer2) -> {
            if (count.get() < acceptanceAmount) {
                result[integer] = true;
                count.getAndIncrement();
            }
        });

        return result;
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @Override
    public int voteEnd(int[][] contracts) {
        //TODO
        return 0;
    }

    public void printUtility(int[] contract) {
        System.out.print(evaluate(contract));
    }


    private int evaluate(int[] contract) {

        int result = 0;
        for (int i = 0; i < contract.length - 1; i++) {
            int zeile = contract[i];
            int spalte = contract[i + 1];
            result += costMatrix[zeile][spalte];
        }

        return result;
    }

}