import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CustomerAgent extends Agent {

	private final int[][] timeMatrix;	//Maybe replace with CopyOnWriteArrayList
	Map<Contract, Integer> evaluatedTimes = new ConcurrentHashMap<>();
	/*
	Map<Contract, Integer> evaluatedTimes = Collections.synchronizedMap(new LinkedHashMap<Contract, Integer>(){
		@Override
        protected boolean removeEldestEntry(Map.Entry<Contract, Integer> eldest) {
			return size() > 10_000_000;
		}
	});

	 */

	public CustomerAgent(File file) throws FileNotFoundException {

		Scanner scanner = new Scanner(file);
		int jobs = scanner.nextInt();
		int machines = scanner.nextInt();
		timeMatrix = new int[jobs][machines];
		for (int i = 0; i < timeMatrix.length; i++) {
			for (int j = 0; j < timeMatrix[i].length; j++) {
				int x = scanner.nextInt();
				timeMatrix[i][j] = x;
			}
		}

		scanner.close();

	}

	public boolean vote(Contract contract, Contract proposal) {
		int timeContract = evaluateNEW(contract);
		int timeProposal = evaluateNEW(proposal);
		if (timeProposal < timeContract)
			return true;
		else
			return false;
	}

	public boolean[] voteLoop(Contract[] contracts, final int acceptanceAmount) {
		// Fill the array
		List<AgentIndexContract> temp = new ArrayList<>();
		for (int i = 0; i < contracts.length; i++) {
			temp.add(new AgentIndexContract(i, contracts[i]));
		}

		//Calculate using multiprocessing
		Stream<AgentIndexContract> stream = temp.parallelStream();
		stream.forEach(i -> {
			i.setCost(evaluateNEW(i));
		});

		// Sort it
		temp.sort(Comparator.comparingInt(a -> a.getCost()));

		boolean[] result = new boolean[contracts.length];
		for (int i = 0; i < acceptanceAmount; i++) {
			result[temp.get(i).getIndex()] = true;
		}

		if (getGlobal_best() == null || getGlobal_best().getCost() > temp.getFirst().getCost()) {
			setGlobal_best(temp.getFirst());
		}

		setRound_best(temp.getFirst());
		if (((long) evaluatedTimes.size()*Verhandlung.ContractObjectMemSizeBytes) > ((double)Verhandlung.maxMemBytes*0.3)){
			evaluatedTimes.clear();
			System.out.println("Clear times Cache");
		}

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
	public int voteEnd(Contract[] contracts) {
		//calculation of times of given contracts
		Map<Integer, Integer> times = new LinkedHashMap<>();
		for (int i = 0; i < contracts.length; i++) {
			times.put(i, evaluateNEW(contracts[i]));
		}
		//sort the times
		Map<Integer, Integer> sortedTimes = sortByValue(times);
		//find the index/key of the highest/worst time in the times map of ln 109
		int foundKey = 0;
		for (Map.Entry<Integer, Integer> entry : times.entrySet()) {
			if(entry.getValue().equals(sortedTimes.get(contracts.length-1))) {
				foundKey = entry.getKey();
				break;
			}
		}
		return foundKey;
	}


	public int getContractSize() {
		return timeMatrix.length;
	}

	public AgentIndexContract printUtility(Contract contract) {
		AgentIndexContract newContract = new AgentIndexContract(0, contract);
		newContract.setCost(evaluateNEW(contract));
		return newContract;
	}
	
	private int evaluateNEW(Contract solution) {
		if (evaluatedTimes.containsKey(solution)) {
			return evaluatedTimes.get(solution);
		}

		int[] solutionArr = solution.getContract();

		int anzM = timeMatrix[0].length;

		if(timeMatrix.length != solution.getContractSize())System.out.println("Fehler in ");
		int[][] start = new int[timeMatrix.length][timeMatrix[0].length];

		//Arrays.stream(start).forEach(a -> Arrays.fill(a, 0));	//should be irrelevant!

		int job = solutionArr[0];
		for(int m=1;m<anzM;m++) {
			start[job][m] = start[job][m-1] + timeMatrix[job][m-1];
		}

		for(int j=1;j<solutionArr.length;j++) {
			int delay             = 0;
			int vorg              = solutionArr[j-1];
			job                   = solutionArr[j];
			boolean delayErhoehen;
			do {
				delayErhoehen = false;
				start[job][0] = start[vorg][0] + timeMatrix[vorg][0] + delay;
				for(int m=1;m<anzM;m++) {
					start[job][m] = start[job][m-1] + timeMatrix[job][m-1];
					if(start[job][m] < start[vorg][m]+timeMatrix[vorg][m]) {
						delayErhoehen = true;
						delay++;
						break;
					}
				}
			}while(delayErhoehen);
		}
		int last = solutionArr[solutionArr.length-1];


//		for(int j=0;j<solution.length;j++) {
//			for(int m=0;m<anzM;m++) {
//				System.out.print(start[j][m] + "\t");
//			}
//			System.out.println();
//		}

		int timeValue = start[last][anzM-1]+timeMatrix[last][anzM-1];
		evaluatedTimes.put(solution, timeValue);
		return (timeValue);

	}

	
}
