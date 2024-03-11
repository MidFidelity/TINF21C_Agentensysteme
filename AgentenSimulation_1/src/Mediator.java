import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Mediator {

	int contractSize;
	int generationSize;
	
	public Mediator(int contractSizeA, int contractSizeB, int generationSize) throws FileNotFoundException{
		if(contractSizeA != contractSizeB){
			throw new FileNotFoundException("Verhandlung kann nicht durchgefuehrt werden, da Problemdaten nicht kompatibel");
		}
		this.contractSize = contractSizeA;
		this.generationSize = generationSize;
	}
	
	public int[][] initContract(){
		int[][] contracts = new int[generationSize][contractSize];
		List<Integer> contract = IntStream.range(0,contractSize).boxed().collect(Collectors.toList());
		for (int i = 0; i <generationSize; i++) {
			Collections.shuffle(contract);
			contracts[i] = contract.stream().mapToInt(Integer::intValue).toArray();
		}

		return contracts;
	}

	public int[][] crossover(){

	}

	public int[][] mutate(){

	}

	public int[] constructProposal(int[] contract) {
		int[] proposal = new int[contractSize];
		for(int i=0;i<proposal.length;i++)proposal[i] = contract[i];
		int element = (int)((proposal.length-1)*Math.random());
		int wert1   = proposal[element];
		int wert2   = proposal[element+1];
		proposal[element]   = wert2;
		proposal[element+1] = wert1;
		return proposal;
	}
}
