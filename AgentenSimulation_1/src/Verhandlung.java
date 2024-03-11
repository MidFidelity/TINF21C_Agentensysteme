import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;


//SIMULATION!

/*
 * Was ist das "Problem" der nachfolgenden Verhandlung?
 * - Frühe Stagnation, da die Agenten frühzeitig neue Contracte ablehnen
 * - Verhandlung ist nur für wenige Agenten geeignet, da mit Anzahl Agenten auch die Stagnationsgefahr wächst
 * 
 * Aufgabe: Entwicklung und Anaylse einer effizienteren Verhandlung. Eine Verhandlung ist effizienter, wenn
 * eine frühe Stagnation vermieden wird und eine sozial-effiziente Gesamtlösung gefunden werden kann.
 * 
 * Ideen:
 * - Agenten müssen auch Verschlechterungen akzeptieren bzw. zustimmen, die einzuhaltende Mindestakzeptanzrate wird vom Mediator vorgegeben
 * - Agenten schlagen alternative Kontrakte vor
 * - Agenten konstruieren gemeinsam einen Kontrakt
 * - In jeder Runde werden mehrere alternative Kontrakte vorgeschlagen
 * - Alternative Konstruktionsmechanismen für Kontrakte
 * - Ausgleichszahlungen der Agenten (nur möglich, wenn beide Agenten eine monetaere Zielfunktion haben
 * 
 */


public class Verhandlung {	

		private static final int generationsSize = 100;

		public static void main(String[] args) {
			int[][] generation;
			Agent agA, agB;
			Mediator med;
			int maxGenerations;
			int currentAcceptanceAmount=60;

			
			try{
				agA = new SupplierAgent(new File("../data/daten3ASupplier_200.txt"));
				agB = new CustomerAgent(new File("../data/daten4BCustomer_200_5.txt"));
				med = new Mediator(agA.getContractSize(), agB.getContractSize(), generationsSize);
				
				//Verhandlung initialisieren
				generation = med.initContract();

				boolean[] voteA = agA.voteLoop(generation, currentAcceptanceAmount);
				boolean[] voteB = agB.voteLoop(generation, currentAcceptanceAmount);
				boolean[] intersect = new boolean[currentAcceptanceAmount];

				for (int i = 0; i < currentAcceptanceAmount; i++) {
					intersect[i] = voteA[i] && voteB[i];
				}


				
			}
			catch(FileNotFoundException e){
				System.out.println(e.getMessage());
			}
		}
		
		public static void ausgabe(Agent a1, Agent a2, int i, int[] contract){
			System.out.print(i + " -> " );
			a1.printUtility(contract);
			System.out.print("  ");
			a2.printUtility(contract);
			System.out.println();
		}

}