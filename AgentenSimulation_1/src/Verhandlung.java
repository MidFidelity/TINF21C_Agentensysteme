import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;


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
    public static final long maxMemBytes = Runtime.getRuntime().maxMemory();
    public static final int ContractObjectMemSizeBytes = 985;

    //Hyperparameter
    private static final int generationsSize = 500_000;
    private static final int maxGenerations = 3400;

    private static final double infillRate = 0.05;
    private static final double mutationRate = 0.5; //increases to 1 during runtime

    private static final double minAcceptacneRate = 0.02;
    private static final double maxAcceptacneRate = 0.7;
    private static final double acceptanceRateGrowth = maxAcceptacneRate - minAcceptacneRate;
    private static final double accepanceRateOffset = 0.05;

    public static void main(String[] args) {
        final long completeRuntimeStart = System.nanoTime();
        Contract[] generation;  //ein Contract Object ~971 bytes bei int[200]
        Agent agA, agB;
        Mediator med;
        int currentAcceptanceAmount = (int) (generationsSize * 0.77);//0.77
        //int currentInfill = (int) (generationsSize * infillRate);
        int currentInfill = 0;
        int mutationAmount = (int) (generationsSize * mutationRate);
        SplittableRandom rand = new SplittableRandom();

        int avpro = Runtime.getRuntime().availableProcessors();
        try (ExecutorService executor = Executors.newFixedThreadPool(avpro)) {
            agA = new SupplierAgent(new File("../data/daten3ASupplier_200.txt"));
            //agB = new SupplierAgent(new File("../data/daten3ASupplier_200.txt"));
            //agA = new CustomerAgent(new File("../data/daten4BCustomer_200_5.txt"));
            agB = new CustomerAgent(new File("../data/daten4BCustomer_200_5.txt"));
            med = new Mediator(agA.getContractSize(), agB.getContractSize(), generationsSize);

            //Verhandlung initialisieren
            generation = med.initContract();
            Set<Contract> knownBadContracts = ConcurrentHashMap.newKeySet();

            for (int currentGeneration = 0; currentGeneration < maxGenerations; currentGeneration++) {
                //Step1: Update Relevant Variables for usage of simulated annealing
                final double generationProgress = (double) currentGeneration / maxGenerations;
                currentAcceptanceAmount = Math.min(
                        (int) ((double) generationsSize * maxAcceptacneRate),
                        Math.max(
                                (int) (generationsSize * minAcceptacneRate),
                                (int) (generationsSize * (
                                        (1 - (((double) currentGeneration / maxGenerations)))
                                                * (acceptanceRateGrowth + accepanceRateOffset)
                                                + (minAcceptacneRate - accepanceRateOffset))
                                )
                        ));
                mutationAmount = Math.min((int) (generationsSize * mutationRate), (int) (generationsSize * (((double) currentGeneration / maxGenerations))));
                //currentInfill = Math.min((int) (generationsSize * 0.7), (int) ((generationsSize * (((double) currentGeneration / maxGenerations)))*0.3));

                if ((long)knownBadContracts.size() * ContractObjectMemSizeBytes > (double)maxMemBytes*0.3) {  //Max 30% of Heap Space
                    knownBadContracts.clear();  //Clearing Memmory
                    System.out.println("Known Bad Cleared");
                }


                /*Step 2: Let Agents vote on the generation
                    Return which Contracts are wanted by the Agents
                    Primarily using intersect to move forward -> Steady State GA
                    Filter out known bad contracts -> not shown again to agent -> more/ better contracts can be shown
                 */
                long startTime = System.nanoTime();

                int finalCurrentAcceptanceAmount = currentAcceptanceAmount;
                Contract[] finalGeneration = generation;
                CompletableFuture<boolean[]> voteAFuture = CompletableFuture.supplyAsync(() -> agA.voteLoop(finalGeneration, finalCurrentAcceptanceAmount), executor);
                CompletableFuture<boolean[]> voteBFuture = CompletableFuture.supplyAsync(() -> agB.voteLoop(finalGeneration, finalCurrentAcceptanceAmount), executor);

                CompletableFuture.allOf(voteAFuture, voteBFuture).join();
                boolean[] voteA = voteAFuture.get();
                boolean[] voteB = voteBFuture.get();

                long voteTime = System.nanoTime() - startTime;
                ArrayList<Contract> intersect = new ArrayList<>();

                int voteACount = 0;
                for (boolean vote : voteA) {
                    voteACount+=vote?1:0;
                }
                int voteBCount = 0;
                for (boolean vote : voteB) {
                    voteBCount+=vote?1:0;
                }

                System.out.println(voteACount + " " + voteBCount);

                for (int i = 0; i < generationsSize; i++) {
                    if (voteA[i] && voteB[i]) {
                        intersect.add(generation[i]);
                    } else if (!voteA[i] && !voteB[i]) {
                        knownBadContracts.add(generation[i]);   //Just store the ones no agents want
                    }
                }
                int intersectSize = intersect.size();

                if (intersect.isEmpty()) {
                    //TODO
                    throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
                }
                Contract printIntersect = intersect.getFirst();

                //Get best 10% of intersect -> can be used for a higher selection probability for these Contracts
                ArrayList<Contract> bestIntersect = new ArrayList<>();
                CompletableFuture<boolean[]> voteAFutureBest = CompletableFuture.supplyAsync(() -> agA.voteLoop(intersect.toArray(new Contract[intersectSize]), (int) (intersectSize*0.1)), executor);
                CompletableFuture<boolean[]> voteBFutureBest = CompletableFuture.supplyAsync(() -> agB.voteLoop(intersect.toArray(new Contract[intersectSize]), (int) (intersectSize*0.1)), executor);
                CompletableFuture.allOf(voteAFutureBest, voteBFutureBest).join();

                boolean[] voteABest = voteAFutureBest.get();
                boolean[] voteBBest = voteBFutureBest.get();
                for (int i = 0; i < intersectSize; i++) {
                    if (voteABest[i] && voteBBest[i]) {
                        bestIntersect.add(generation[i]);       //Steady State GA
                        bestIntersect.add(generation[i]);
                    } else if (voteABest[i] || voteBBest[i]) {
                        bestIntersect.add(generation[i]);
                    }
                }

                //List of all the valid Contracts that are used for Mating (with right Probabilities)
                List<Contract> proportionalCrossOverSelektion = new ArrayList<>(intersect);
                proportionalCrossOverSelektion.addAll(bestIntersect);
                //proportionalCrossOverSelektion.addAll(bestIntersect);
                Collections.shuffle(proportionalCrossOverSelektion);    //maybe not relevant since always two random Contracts are picked for mating

                /*
                Step 3: Create new Generation
                    - CrossOver
                    - Mutation
                 */

                Set<Contract> newGenerationHashSet = ConcurrentHashMap.newKeySet();
                if (generationProgress > 0.8) {
                    newGenerationHashSet.addAll(intersect);
                }

                List<CompletableFuture<Void>> futures = new LinkedList<>();
                int whileCount = 0;
                while (newGenerationHashSet.size() < generationsSize && whileCount < generationsSize * 3) {
                    int innerWhileCount = 0;
                    int newGenerationHashSetStartsize = newGenerationHashSet.size();
                    while (newGenerationHashSet.size() < generationsSize && innerWhileCount < (generationsSize-newGenerationHashSetStartsize)) {
                        futures.add(
                                CompletableFuture.supplyAsync(() -> {
                                    Contract parent1 = proportionalCrossOverSelektion.get(rand.nextInt(proportionalCrossOverSelektion.size()));
                                    Contract parent2 = proportionalCrossOverSelektion.get(rand.nextInt(proportionalCrossOverSelektion.size()));
                                    Contract[] childs = parent1.crossover(parent2);

                                    if(!knownBadContracts.contains(childs[0])){
                                        newGenerationHashSet.add(childs[0]);
                                    }
                                    if(!knownBadContracts.contains(childs[1])){
                                        newGenerationHashSet.add(childs[1]);
                                    }

                                    return null;
                                }, executor)
                        );
                        whileCount++;
                        innerWhileCount++;
                    }
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }

                //if not enough contract after (generationsSize * 3) try just fill with random - just for the sake of runtime
                while (newGenerationHashSet.size() < generationsSize) {
                    newGenerationHashSet.addAll(Arrays.stream(med.getRandomContracts(generationsSize - newGenerationHashSet.size())).toList());
                }

                //Overwrite old generation Array -> mutation will happen afterward to reduce memory footprint
                generation = newGenerationHashSet.toArray(Contract[]::new);

                // Mutate
                for (int i = 0; i < mutationAmount; i++) {
                    int contractIndexToMutate = rand.nextInt(generationsSize);
                    generation[contractIndexToMutate].swapMutateRand();
                }


                System.out.printf("%4d: Best A: %5d \t Best B: %5d \t AccAmount: %4d \t Intersect: %4d \t Contract: %5d %5d",
                        currentGeneration,
                        agA.getRound_best().getCost(), agB.getRound_best().getCost(),
                        currentAcceptanceAmount, intersectSize,
                        agA.printUtility(printIntersect).getCost(), agB.printUtility(printIntersect).getCost());

                long mediatorTime = System.nanoTime() - startTime - voteTime;
                System.out.println("   Time:" + voteTime + "  " + mediatorTime);
                System.out.flush();
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

            long CompleteRuntime = System.nanoTime() - completeRuntimeStart;

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


            // TimeUnit
            long CompleteRuntimeMinutes = TimeUnit.MINUTES.convert(CompleteRuntime, TimeUnit.NANOSECONDS);
            System.out.println("Complete Runtime: " + CompleteRuntimeMinutes + " min");

        } catch (FileNotFoundException | InterruptedException | ExecutionException e) {
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