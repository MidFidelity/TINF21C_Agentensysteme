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
    //No fixed values - must be dependent on generationSize OR be percentage Value
    public static final int generationsSize = 40_000;
    private static final int maxGenerations = 2_000;

    private static final double infillRate = 0.05;
    private static final double mutationRateMin = 0.7;
    private static final double mutationRateMax = 1;//generationsSize * 0.00006;
    private static final double mutationRateTurningPoint = 0.6;


    private static final double minAcceptacneRate = 0.2;
    private static final double maxAcceptacneRate = 0.7;
    private static final double acceptanceRateGrowth = maxAcceptacneRate - minAcceptacneRate;
    private static final double accepanceRateOffset = 0.05;

    public static void main(String[] args) {
        final long completeRuntimeStart = System.nanoTime();
        Contract[] generation;
        Agent agA, agB;
        Mediator med;
        int currentAcceptanceAmount = (int) (generationsSize * 0.77);//0.77
        //double mutationRate = mutationRateStart;
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

                double mutationRate = getMutationRate(currentGeneration);
                //System.out.println(mutationRate);

                //double mutationRate = (((double) currentGeneration / maxGenerations)*mutationRateMin+mutationRateMin)*2;
                //currentInfill = Math.min((int) (generationsSize * 0.7), (int) ((generationsSize * (((double) currentGeneration / maxGenerations)))*0.3));


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
                ArrayList<Contract> singleIntersect = new ArrayList<>();

                if (knownBadContracts.size() > (maxGenerations * 0.1) * generationsSize) {
                    knownBadContracts.clear();
                    System.out.println("Known Bad Cleared");
                }
                for (int i = 0; i < generationsSize; i++) {
                    if (voteA[i] && voteB[i]) {
                        intersect.add(generation[i]);       //Steady State GA
                    } else if (voteA[i] || voteB[i]) {
                        singleIntersect.add(generation[i]);
                    } else {
                        knownBadContracts.add(generation[i]);
                    }
                }
                int intersectSize = intersect.size();

                //Get best 10% of intersect
                ArrayList<Contract> bestIntersect = new ArrayList<>();
                CompletableFuture<boolean[]> voteAFutureBest = CompletableFuture.supplyAsync(() -> agA.voteLoop(intersect.toArray(new Contract[intersectSize]), (int) (intersectSize*0.1)), executor);
                CompletableFuture<boolean[]> voteBFutureBest = CompletableFuture.supplyAsync(() -> agB.voteLoop(intersect.toArray(new Contract[intersectSize]), (int) (intersectSize*0.1)), executor);

                CompletableFuture.allOf(voteAFutureBest, voteBFutureBest).join();

                boolean[] voteABest = voteAFuture.get();
                boolean[] voteBBest = voteBFuture.get();
                for (int i = 0; i < generationsSize; i++) {
                    if (voteABest[i] && voteBBest[i]) {
                        bestIntersect.add(generation[i]);       //Steady State GA
                        bestIntersect.add(generation[i]);
                    } else if (voteABest[i] || voteBBest[i]) {
                        bestIntersect.add(generation[i]);
                    }
                }

                if (intersect.isEmpty()) {
                    //TODO
                    throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
                }
                Collections.shuffle(intersect);
                Collections.shuffle(singleIntersect);
                Contract printIntersect = intersect.getFirst();

                Set<Contract> newGenerationHashSet = ConcurrentHashMap.newKeySet();

                //reintroduce singleIntersect for CrossOver to reduce Selektionsdruck
                List<Contract> proportionalCrossOverSelektion = new ArrayList<>(intersect);
                proportionalCrossOverSelektion.addAll(bestIntersect);
                proportionalCrossOverSelektion.addAll(bestIntersect);
                /*
                if ((double) currentAcceptanceAmount /currentGeneration < 0.3) {
                    int intersectRepeat = (((int) ((double) singleIntersect.size() / intersectSize)) + 1) * 2;
                    for (int i = 0; i < intersectRepeat; i++) {
                        proportionalCrossOverSelektion.addAll(intersect);
                    }
                    proportionalCrossOverSelektion.addAll(singleIntersect);
                    //ratio in proportionalCrossOverSelektion intersect 2 : singleIntersect 1
                    //proportionalCrossOverSelektion.addAll(singleIntersect.subList(0, (intersectSize/2)));
                    //Not all single Intersects are kept - losing purpose?
                    // better add intersect multiple times?
                } else {

                    //newGenerationHashSet.addAll(intersect);
                }

                 */

                List<CompletableFuture<Void>> futures = new LinkedList<>();
                int whileCount = 0;
                while (newGenerationHashSet.size() < generationsSize && whileCount < generationsSize * 3) {
                    int innerWhileCount = 0;
                    int newGenerationHashSetStartsize = newGenerationHashSet.size();
                    while (newGenerationHashSet.size() < generationsSize && innerWhileCount < (generationsSize - newGenerationHashSetStartsize)) {
                        futures.add(
                                CompletableFuture.supplyAsync(() -> {
                                    Contract parent1 = proportionalCrossOverSelektion.get(rand.nextInt(proportionalCrossOverSelektion.size()));
                                    Contract parent2 = proportionalCrossOverSelektion.get(rand.nextInt(proportionalCrossOverSelektion.size()));
                                    Contract[] childs = parent1.crossover(parent2);

                                    /*
                                    if (mutationRate > rand.nextDouble(mutationRateMax)) {
                                        for (int i = 0; i < Math.round(mutationRate); i++) {
                                            childs[0].swapMutateRand();
                                            childs[1].swapMutateRand();
                                        }
                                    }

                                     */

                                    if (!knownBadContracts.contains(childs[0])) {
                                        newGenerationHashSet.add(childs[0]);
                                    }
                                    if (!knownBadContracts.contains(childs[1])) {
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
                //CompletableFuture.allOf(futures.toArray(new Future<?>[0]).get();

                //if not enough contract after 10.000 try just fill with random - just for the sake of runtime
                while (newGenerationHashSet.size() < generationsSize) {
                    newGenerationHashSet.addAll(Arrays.stream(med.getRandomContracts(generationsSize - newGenerationHashSet.size())).toList());
                }

                int mutationAmount = (int) (generationsSize * mutationRate);
                generation = newGenerationHashSet.toArray(Contract[]::new);

                // Mutate
                for (int i = 0; i < mutationAmount; i++) {
                    int contractIndexToMutate = rand.nextInt(generationsSize);
                    generation[contractIndexToMutate].swapMutateRand();
                }


                //reevaluate


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
                    infillRate, mutationRateMin,
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

    private static double getMutationRate(int currentGeneration) {
        double mutationRate;
        if (currentGeneration > mutationRateTurningPoint * maxGenerations) {
            //declining
            mutationRate = ((currentGeneration - maxGenerations * mutationRateTurningPoint) * -1 / (maxGenerations * (1 - mutationRateTurningPoint))) *
                    (mutationRateMax - mutationRateMin) + mutationRateMax;
        } else {
            //climbing
            mutationRate = ((double) currentGeneration / (maxGenerations * mutationRateTurningPoint)) *
                    (mutationRateMax - mutationRateMin) + mutationRateMin;
        }
        return mutationRate;
    }

    public static void ausgabe(Agent a1, Agent a2, int i, Contract contract) {
        System.out.print(i + " -> ");
        a1.printUtility(contract);
        System.out.print("  ");
        a2.printUtility(contract);
        System.out.println();
    }

}