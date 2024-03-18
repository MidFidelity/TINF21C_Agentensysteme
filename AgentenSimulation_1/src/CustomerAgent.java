import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomerAgent extends Agent {

	private int[][] timeMatrix;
	HashMap<int[], Integer> evaluatedTimes = new HashMap<>();

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

	public boolean vote(int[] contract, int[] proposal) {
		int timeContract = evaluateNEW(contract);
		int timeProposal = evaluateNEW(proposal);
		if (timeProposal < timeContract)
			return true;
		else
			return false;
	}

	public boolean[] voteLoop(int[][] contracts, final int acceptanceAmount) {
		Map<Integer, Integer> times = new LinkedHashMap<>();
		for (int i = 0; i < contracts.length; i++) {
			times.put(i, evaluateNEW(contracts[i]));
		}

		times = sortByValue(times);

		boolean[] result = new boolean[contracts.length];
		AtomicInteger count = new AtomicInteger();

		times.forEach((integer, integer2) -> {
			if (count.get() < acceptanceAmount) {
				result[integer] = true;
				count.getAndIncrement();
			}
		});

		System.out.print(costs.entrySet().iterator().next().getValue());
		return result;
	}

//	public boolean[] voteLoop (int[][] contracts, int acceptanceAmount){
//		int[][] clonedContracts = contracts;
//		boolean[] results = new boolean[contracts.length];
//		//iterate over all the rows and check if one contract is better than the other, if so the better contract moves to the left
//		//bubblesort (?)
//		for(int rowsA=0;rowsA<contracts.length;rowsA++) {
//			for(int rowsB=0;rowsB<contracts.length;rowsB++) {
//				if (vote(contracts[rowsB], contracts[rowsB + 1])) {
//					int[] temp = clonedContracts[rowsB];
//					clonedContracts[rowsB] = clonedContracts[rowsB + 1];
//					clonedContracts[rowsB + 1] = temp;
//				}
//				rowsB++;
//			}
//			rowsA++;
//		}
//		//after the best contracts have been moved to the top, an iteration (for the true/false determination) is needed to retain
//		//the order of the original contracts array
//		//e.g. if the array from our contracts[0] has been found among the first 60 arrays of our sorted/cloned array then -> write true for
//		//results[0], else write false; repeat for every row
//		for(int rows=0;rows<contracts.length;rows++) {
//			for(int rowsCloned=0;rowsCloned<acceptanceAmount;rowsCloned++) {
//				if(clonedContracts[rowsCloned] == contracts[rows]){
//					results[rows] = true;
//				}
//				rowsCloned++;
//			}
//			if(results[rows] != true){
//				results[rows] = false;
//			}
//			rows++;
//		}
//
//		return results;
//	}

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

	public void printUtility(int[] contract) {
		System.out.print(evaluateNEW(contract));
	}
	
	private int evaluateNEW(int[] solution) {
		if(evaluatedTimes.containsKey(solution))
		{
			return evaluatedTimes.get(solution);
		}
		else
		{
			int anzM = timeMatrix[0].length;

			if(timeMatrix.length != solution.length)System.out.println("Fehler in ");
			int[][] start = new int[timeMatrix.length][timeMatrix[0].length];

			for(int i=0;i<start.length;i++) {
				for(int j=0;j<start[i].length;j++) {
					start[i][j] = 0;
				}
			}

			int job = solution[0];
			for(int m=1;m<anzM;m++) {
				start[job][m] = start[job][m-1] + timeMatrix[job][m-1];
			}

			for(int j=1;j<solution.length;j++) {
				int delay             = 0;
				int vorg              = solution[j-1];
				job                   = solution[j];
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
			int last = solution[solution.length-1];


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

	
}
