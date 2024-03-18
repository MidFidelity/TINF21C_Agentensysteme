import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SupplierAgent extends Agent {

    private int[][] costMatrix;
    HashMap<int[], Integer> evaluatedCosts = new HashMap<>();

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

        System.out.print(costs.entrySet().iterator().next().getValue());
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
        //calculation of costs of given contracts
        Map<Integer, Integer> costs = new LinkedHashMap<>();
        for (int i = 0; i < contracts.length; i++) {
            costs.put(i, evaluate(contracts[i]));
        }
        //sort the costs
        Map<Integer, Integer> sortedCosts = sortByValue(costs);
        //find the index/key of the highest/worst cost in the costs map of ln 75
        int foundKey = 0;
        for (Map.Entry<Integer, Integer> entry : costs.entrySet()) {
            if(entry.getValue().equals(sortedCosts.get(contracts.length-1))) {
                foundKey = entry.getKey();
                break;
            }
        }
        return foundKey;
    }

    public void printUtility(int[] contract) {
        System.out.print(evaluate(contract));
    }


    private int evaluate(int[] contract) {
        if(evaluatedCosts.containsKey(contract))
        {
            return evaluatedCosts.get(contract);
        }
        else {
            int result = 0;
            for (int i = 0; i < contract.length - 1; i++) {
                int zeile = contract[i];
                int spalte = contract[i + 1];
                result += costMatrix[zeile][spalte];
            }
            evaluatedCosts.put(contract, result);
            return result;
        }
    }

}