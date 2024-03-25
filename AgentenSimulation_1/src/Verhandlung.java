import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;


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

    private static final int generationsSize = 500;
    private static final int maxGenerations = 2000;

    private static final double infillRate = 0.05;
    private static final double mutationRate = 0.15;

    private static final double minAcceptacneRate = 0.1;
    private static final double acceptanceRateGrowth = 1.5;

    public static void main(String[] args) {
        Contract[] generation;
        Agent agA, agB;
        Mediator med;
        int currentAcceptanceAmount = (int) (generationsSize * 0.77);//0.77
        int currentInfill = (int) (generationsSize * infillRate);
        int mutationAmount = (int) (generationsSize * mutationRate);


        try {
            agA = new SupplierAgent(new File("../data/daten3ASupplier_200.txt"));
            agB = new CustomerAgent(new File("../data/daten4BCustomer_200_5.txt"));
            med = new Mediator(agA.getContractSize(), agB.getContractSize(), generationsSize);

            //Verhandlung initialisieren
            generation = med.initContract();

            for (int currentGeneration = 0; currentGeneration < maxGenerations; currentGeneration++) {
                currentAcceptanceAmount = Math.max((int) (generationsSize * minAcceptacneRate), (int) (generationsSize * (1 - (((double) currentGeneration / maxGenerations)) * acceptanceRateGrowth)));
                //mutationAmount = Math.min((int) (generationsSize * ), (int) (generationsSize * (((double) currentGeneration / maxGenerations))));
                //currentInfill = Math.min((int) (generationsSize * 0.7), (int) ((generationsSize * (((double) currentGeneration / maxGenerations)))*0.3));


                long startTime = System.nanoTime();
                boolean[] voteA = agA.voteLoop(generation, currentAcceptanceAmount);
                boolean[] voteB = agB.voteLoop(generation, currentAcceptanceAmount);
                long voteTime = System.nanoTime() - startTime;
                ArrayList<Contract> intersect = new ArrayList<>();
                ArrayList<Contract> singleVote = new ArrayList<>();


                for (int i = 0; i < generationsSize; i++) {
                    if (voteA[i] && voteB[i]) {
                        intersect.add(generation[i]);
                    } else if (voteA[i] || voteB[i]) {
                        singleVote.add(generation[i]);
                    }
                }
                int intersectSize = intersect.size();
                 /*
                for (int i = 0; i < generationsSize; i++) {
                    if (voteB[i]) {
                        intersect.add(generation[i]);
                    } else if (voteA[i]) {
                        singleVote.add(generation[i]);

                    }
                }
                 */

                Contract[] newGeneration = new Contract[generationsSize];
                if (intersect.isEmpty()) {
                    //TODO
                    throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
                }
                Collections.shuffle(intersect);
                Collections.shuffle(singleVote);

                Contract printIntersect = intersect.getFirst();

                int currentNewGenerationCount = 0;
                //first use intersect (both want it)
                while (currentNewGenerationCount < (generationsSize - currentInfill) && intersect.size() >= 2) {
                    Contract parent1 = intersect.removeLast();
                    Contract parent2 = intersect.removeLast();
                    Contract[] childs = Crossover.cxOrdered(parent1, parent2);


                    newGeneration[currentNewGenerationCount] = childs[0];
                    newGeneration[currentNewGenerationCount + 1] = childs[1];
                    newGeneration[currentNewGenerationCount + 2] = parent1;
                    newGeneration[currentNewGenerationCount + 3] = parent2;

                    currentNewGenerationCount += 4;
                }

                List<Contract> newGenerationList = new ArrayList<>();
                newGenerationList.addAll(List.of(Arrays.copyOfRange(newGeneration, 0, currentNewGenerationCount)));

                newGenerationList = newGenerationList.stream().unordered().parallel().distinct().collect(Collectors.toCollection(ArrayList::new));

                while (newGenerationList.size() < generationsSize) {
                    //If not enough contract fill with random

                    newGenerationList.addAll(Arrays.stream(med.getRandomContracts(generationsSize - newGenerationList.size())).toList());
                    //System.arraycopy(newGeneration, currentNewGenerationCount, newRandom, 0, newRandom.length);

                    newGenerationList = newGenerationList.stream().unordered().parallel().distinct().collect(Collectors.toCollection(ArrayList::new));
                }

                newGeneration = newGenerationList.toArray(Contract[]::new);

                // Mutate
                Random rand = new Random();
                for (int i = 0; i < mutationAmount; i++) {
                    int contractIndexToMutate = rand.nextInt(generationsSize);
                    newGeneration[contractIndexToMutate].mutate();
                }

                //reevaluate
                generation = newGeneration;

                System.out.printf("%4d: Best A: %5d \t Best B: %5d \t AccAmount: %4d \t Intersect: %4d \t Contract: %5d %5d",
                        currentGeneration,
                        agA.getRound_best().getCost(), agB.getRound_best().getCost(),
                        currentAcceptanceAmount, intersectSize,
                        agA.printUtility(printIntersect).getCost(), agB.printUtility(printIntersect).getCost());

                long mediatorTime = System.nanoTime() - startTime - voteTime;
                System.out.println("   Time:" + voteTime + "  " + mediatorTime);
            }

            System.out.println("----------");
            System.out.println("Changing to Terminal Phase");
            System.out.println("----------");

            boolean[] voteA = agA.voteLoop(generation, currentAcceptanceAmount);
            boolean[] voteB = agB.voteLoop(generation, currentAcceptanceAmount);
            ArrayList<Contract> intersect = new ArrayList<>();

            for (int i = 0; i < generationsSize; i++) {
                if (voteA[i] && voteB[i]) {
                    intersect.add(generation[i]);
                }
            }

            Contract[] temp = new Contract[intersect.size()];
            generation = intersect.toArray(temp);

            while (generation.length > 1) {
                int remove;
                if (generation.length % 2 == 0) {
                    remove = agA.voteEnd(generation);
                } else {
                    remove = agB.voteEnd(generation);
                }

                Contract[] newGeneration = new Contract[generation.length - 1];
                System.arraycopy(generation, 0, newGeneration, 0, remove);
                System.arraycopy(generation, remove + 1,
                        newGeneration, remove,
                        generation.length - remove - 1);
                generation = newGeneration;
            }

            System.out.print("""
                    ----------
                    Config
                    ----------
                    """);
            System.out.printf("""
                            Config:
                            MaxGenerations:    %d \t GenerationSize:       %d
                            InfillRate:        %.3f \t MutationRate:         %.3f
                            MinAcceptacneRate: %.3f \t AcceptanceRateGrowth: %.3f
                            
                            """,
                    maxGenerations, generationsSize,
                    infillRate, mutationRate,
                    minAcceptacneRate, acceptanceRateGrowth
            );
            System.out.print("""
                    ----------
                    Result
                    ----------
                    """);
            System.out.printf("""
                                                
                            Best Cost A: %d
                            Best Cost B: %d
                            Sum: %d

                            Picked Contract:
                            A: %d \t B: %d
                            Sum: %d
                            """,
                    agA.getGlobal_best().getCost(),
                    agB.getGlobal_best().getCost(),
                    agA.getGlobal_best().getCost() + agB.getGlobal_best().getCost(),
                    agA.printUtility(generation[0]).getCost(),
                    agB.printUtility(generation[0]).getCost(),
                    agA.printUtility(generation[0]).getCost() + agB.printUtility(generation[0]).getCost()
            );


        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void ausgabe(Agent a1, Agent a2, int i, Contract contract) {
        System.out.print(i + " -> ");
        a1.printUtility(contract);
        System.out.print("  ");
        a2.printUtility(contract);
        System.out.println();
    }

}