import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.IntStream;


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

    private static final int generationsSize = 1000;
    private static final int maxGenerations = 3000;

    public static void main(String[] args) {
        int[][] generation;
        Agent agA, agB;
        Mediator med;
        int currentAcceptanceAmount = (int)(generationsSize * 0.77);
        int currentInfill = (int) (generationsSize*0.1);    //must be divisible by 4
        int mutationAmount = (int) (generationsSize*0.1); //equal 10%
        assert currentInfill % 4 == 0;


        try {
            agA = new SupplierAgent(new File("../data/daten3ASupplier_200.txt"));
            agB = new CustomerAgent(new File("../data/daten4BCustomer_200_5.txt"));
            med = new Mediator(agA.getContractSize(), agB.getContractSize(), generationsSize);

            //Verhandlung initialisieren
            generation = med.initContract();

            for (int currentGeneration = 0; currentGeneration < maxGenerations; currentGeneration++) {
                long startTime = System.nanoTime();
                System.out.print(currentGeneration + ": ");
                boolean[] voteA = agA.voteLoop(generation, currentAcceptanceAmount);
                System.out.print("  ");
                boolean[] voteB = agB.voteLoop(generation, currentAcceptanceAmount);
                System.out.print("  ");
                long voteTime = System.nanoTime() - startTime;
                ArrayList<int[]> intersect = new ArrayList<>();
                ArrayList<int[]> singleVote = new ArrayList<>();

                for (int i = 0; i < currentAcceptanceAmount; i++) {
                    if (voteA[i] && voteB[i]) {
                        intersect.add(generation[i]);
                    } else if (voteA[i] || voteB[i]) {
                        singleVote.add(generation[i]);
                    }
                }

                int[][] newGeneration = new int[generationsSize][med.contractSize];
                if (intersect.isEmpty()) {
                    //TODO
                    throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
                }
                Collections.shuffle(intersect);
                Collections.shuffle(singleVote);

                System.out.print(intersect.size());
                System.out.print("  One Intersect: ");
                agA.printUtility(intersect.getFirst());
                System.out.print("  ");
                agB.printUtility(intersect.getFirst());


                int currentNewGenerationCount = 0;
                //first use intersect (both want it)
                while (currentNewGenerationCount < (generationsSize - currentInfill) && intersect.size() >= 2) {
                    int[] parent1 = intersect.removeLast();
                    int[] parent2 = intersect.removeLast();
                    int[][] childs = Crossover.cxOrdered(parent1, parent2);

                    newGeneration[currentNewGenerationCount] = parent1;
                    newGeneration[currentNewGenerationCount + 1] = parent2;
                    newGeneration[currentNewGenerationCount + 2] = childs[0];
                    newGeneration[currentNewGenerationCount + 3] = childs[1];

                    currentNewGenerationCount += 4;
                }

                //use the one only one wants
                while (currentNewGenerationCount < (generationsSize-1) && singleVote.size() >= 2) {
                    int[] parent1 = singleVote.removeLast();
                    int[] parent2 = singleVote.removeLast();
                    int[][] childs = Crossover.cxOrdered(parent1, parent2);

                    newGeneration[currentNewGenerationCount] = childs[0];
                    newGeneration[currentNewGenerationCount + 1] = childs[1];

                    currentNewGenerationCount += 1;
                }


                //If not enough contract fill with random
                if (currentNewGenerationCount<generationsSize){
                    int[][] newRandom = med.getRandomContracts(generationsSize-currentNewGenerationCount);
                    //System.arraycopy(newGeneration, currentNewGenerationCount, newRandom, 0, newRandom.length);
                    System.arraycopy(newRandom, 0, newGeneration, currentNewGenerationCount, newRandom.length);
                }

                // Mutate
                Random rand = new Random();
                for (int i = 0; i < mutationAmount; i++) {
                    int contractIndexToMutate = rand.nextInt(generationsSize);
                    newGeneration[contractIndexToMutate] = med.mutation(newGeneration[contractIndexToMutate]);
                    currentNewGenerationCount++;
                }


                //reevaluate
                generation = newGeneration;

                long mediatorTime = System.nanoTime() - startTime - voteTime;
                System.out.println("   Time:" + voteTime + "  " + mediatorTime);
            }
            System.out.println("----------");
            System.out.println("Changing to Terminal Phase");
            System.out.println("----------");

            while (generation.length > 1){
                int remove;
                if(generation.length%2 == 0){
                    remove = agA.voteEnd(generation);
                }else {
                    remove = agB.voteEnd(generation);
                }

                int[][] newGeneration = new int[generation.length-1][med.contractSize];
                System.arraycopy(generation, 0, newGeneration, 0, remove);
                System.arraycopy(generation, remove + 1,
                        newGeneration, remove,
                        generation.length - remove - 1);
                generation = newGeneration;
            }
            System.out.println("Final Resolution:");
            agA.printUtility(generation[0]);
            System.out.print("  ");
            agB.printUtility(generation[0]);
            System.out.println();



        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void ausgabe(Agent a1, Agent a2, int i, int[] contract) {
        System.out.print(i + " -> ");
        a1.printUtility(contract);
        System.out.print("  ");
        a2.printUtility(contract);
        System.out.println();
    }

}