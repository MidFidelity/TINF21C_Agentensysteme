import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class AustrailianSupplierAgent extends Agent {

    private final int[][] costMatrix;
    Map<Contract, Integer> evaluatedCosts = new ConcurrentHashMap<>();
    /*
    Map<Contract, Integer> evaluatedCosts = Collections.synchronizedMap(new LinkedHashMap<Contract, Integer>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Contract, Integer> eldest) {
            return size() > 10_000_000;
        }
    });
    /*
    ConcurrentSkipListMap<String, String> cache = new ConcurrentSkipListMap<>();

// Check if max size is reached before inserting something in it. Make some room for new entry.
while (cache.size() >= maxSize) {
    cache.pollFirstEntry();
}
     */


    public AustrailianSupplierAgent(File file) throws FileNotFoundException {

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

    public boolean vote(Contract contract, Contract proposal) {
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
    public boolean[] voteLoop(Contract[] contracts, final int acceptanceAmount) {
        // Fill the list
        List<AgentIndexContract> temp = new ArrayList<>();
        for (int i = 0; i < contracts.length; i++) {
            temp.add(new AgentIndexContract(i, contracts[i]));
        }

        //Calculate using multiprocessing
        Stream<AgentIndexContract> stream = temp.parallelStream();
        stream.forEach(i -> {
            i.setCost(evaluate(i));
        });

        // Sort it
        temp.sort(Comparator.comparingInt(a -> a.getCost()));

        boolean[] result = new boolean[contracts.length];
        for (int i = 0; i < acceptanceAmount; i++) {
            result[temp.get(result.length-i-1).getIndex()] = true;
        }

        if (getGlobal_best() == null || getGlobal_best().getCost() > temp.getFirst().getCost()) {
            setGlobal_best(temp.getFirst());
        }

        setRound_best(temp.getFirst());
        if (((long) evaluatedCosts.size()*Verhandlung.ContractObjectMemSizeBytes) > ((double)Verhandlung.maxMemBytes*0.3)){
            evaluatedCosts.clear();
            System.out.println("Clear Costs Cache");
        }

        return result;

/*
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

 */
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        list = list.reversed();


        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @Override
    public int voteEnd(Contract[] contracts) {
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
            if (entry.getValue().equals(sortedCosts.get(contracts.length - 1))) {
                foundKey = entry.getKey();
                break;
            }
        }
        return foundKey;
    }

    public AgentIndexContract printUtility(Contract contract) {
        AgentIndexContract newContract = new AgentIndexContract(0, contract);
        newContract.setCost(evaluate(contract));
        return newContract;
    }


    private int evaluate(Contract contract) {
        if (evaluatedCosts.containsKey(contract)) {
            return evaluatedCosts.get(contract);
        }
        int[] contractArr = contract.getContract();
        int result = 0;
        for (int i = 0; i < contractArr.length - 1; i++) {
            int zeile = contractArr[i];
            int spalte = contractArr[i + 1];
            result += costMatrix[zeile][spalte];
        }
        evaluatedCosts.put(contract, result);
        return result;

    }

}