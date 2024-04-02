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

    private static final int generationsSize = 500_000;
    private static final int maxGenerations = 4000;

    private static final double infillRate = 0.05;
    private static final double mutationRate = 0.5;

    private static final double minAcceptacneRate = 0.25;
    private static final double maxAcceptacneRate = 0.7;
    private static final double acceptanceRateGrowth = maxAcceptacneRate-minAcceptacneRate;

    public static void main(String[] args) {
        final long completeRuntimeStart = System.nanoTime();
        Contract[] generation;
        Agent agA, agB;
        Mediator med;
        int currentAcceptanceAmount = (int) (generationsSize * 0.77);//0.77
        //int currentInfill = (int) (generationsSize * infillRate);
        int currentInfill = 0;
        int mutationAmount = (int) (generationsSize * mutationRate);
        SplittableRandom rand = new SplittableRandom();


        try (ExecutorService executor = Executors.newFixedThreadPool(20)){
            agA = new SupplierAgent(new File("../data/daten3ASupplier_200.txt"));
            agB = new CustomerAgent(new File("../data/daten4BCustomer_200_5.txt"));
            med = new Mediator(agA.getContractSize(), agB.getContractSize(), generationsSize);

            //Verhandlung initialisieren
            generation = med.initContract();

            for (int currentGeneration = 0; currentGeneration < maxGenerations; currentGeneration++) {
                currentAcceptanceAmount = Math.min(
                        (int) ((double) generationsSize * maxAcceptacneRate),
                        Math.max(
                                (int) (generationsSize * minAcceptacneRate),
                                (int) (generationsSize * (
                                        (1 - (((double) currentGeneration / maxGenerations)))
                                                *acceptanceRateGrowth+minAcceptacneRate)
                                )
                        ));
                mutationAmount = Math.min((int) (generationsSize * mutationRate), (int) (generationsSize * (((double) currentGeneration / maxGenerations))));
                //currentInfill = Math.min((int) (generationsSize * 0.7), (int) ((generationsSize * (((double) currentGeneration / maxGenerations)))*0.3));


                long startTime = System.nanoTime();


//                boolean[] voteA = agA.voteLoop(generation, currentAcceptanceAmount);
//                boolean[] voteB = agB.voteLoop(generation, currentAcceptanceAmount);

                int finalCurrentAcceptanceAmount = currentAcceptanceAmount;
                Contract[] finalGeneration = generation;
                CompletableFuture<boolean[]> voteAFuture = CompletableFuture.supplyAsync(()->agA.voteLoop(finalGeneration, finalCurrentAcceptanceAmount), executor);
                CompletableFuture<boolean[]> voteBFuture = CompletableFuture.supplyAsync(()->agB.voteLoop(finalGeneration, finalCurrentAcceptanceAmount), executor);

                CompletableFuture.allOf(voteAFuture, voteBFuture).join();

                boolean[] voteA = voteAFuture.get();
                boolean[] voteB = voteBFuture.get();

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
                int RandomInfillIntersect = 0;
                for (int i = lastRoundRandomBegin; i < generationsSize; i++) {
                    if (voteA[i] && voteB[i]) {
                        RandomInfillIntersect++;
                    }
                }
                System.out.println(RandomInfillIntersect);
                 /*
                for (int i = 0; i < generationsSize; i++) {
                    if (voteB[i]) {
                        intersect.add(generation[i]);
                    } else if (voteA[i]) {
                        singleVote.add(generation[i]);

                    }
                }
                 */

                //Contract[] newGeneration = new Contract[generationsSize];
                if (intersect.isEmpty()) {
                    //TODO
                    throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
                }
                Collections.shuffle(intersect);
                Collections.shuffle(singleVote);

                Contract printIntersect = intersect.getFirst();

                //int currentNewGenerationCount = 0;
                //use intersect (both want it)
//                SplittableRandom rand = new SplittableRandom();
//
//                List<Contract[]> cartesianProduct = new LinkedList<>();
//                for (int i = 0; i < intersect.size(); i++) {
//                    for (Contract contract : intersect) {
//                        cartesianProduct.add(new Contract[]{intersect.get(i), contract});
//                    }
//                }
//                Collections.shuffle(cartesianProduct);

                // Determine the number of available processors (cores)

                // Create an ExecutorService with a fixed thread pool using all available processors

//                Set<Contract> newGenerationHashSet = ConcurrentHashMap.newKeySet();
//                int whileCount = 0;
//                while (newGenerationHashSet.size()<generationsSize && whileCount<10000){
//                    executor.execute(() -> {
//                        Contract[] parents = cartesianProduct.get(newGenerationHashSet.size() % cartesianProduct.size());
//                        Contract[] childs = Crossover.cxOrdered(parents[0], parents[1]);
//
//                        newGenerationHashSet.add(childs[0]);
//                        newGenerationHashSet.add(childs[1]);
//
//                    });
//                    whileCount++;
//                }
                //System.out.println(whileCount);

                Set<Contract> newGenerationHashSet = new HashSet<>();
                int whileCount = 0;
                while (newGenerationHashSet.size()<generationsSize && whileCount<generationsSize){
                    Contract parent1 = intersect.get(rand.nextInt(intersect.size()));
                    Contract parent2 = intersect.get(rand.nextInt(intersect.size()));
                    Contract[] childs = parent1.crossover(parent2);
                    newGenerationHashSet.add(childs[0]);
                    newGenerationHashSet.add(childs[1]);
                    whileCount++;
                }

                //if not enough contract after 10.000 try just fill with random - just for the sake of runtime
                while (newGenerationHashSet.size() < generationsSize) {
                    newGenerationHashSet.addAll(Arrays.stream(med.getRandomContracts(generationsSize - newGenerationHashSet.size())).toList());
                }
                /*
                while (currentNewGenerationCount < (generationsSize - currentInfill)) {
                    Contract parent1 = intersect.get(rand.nextInt(intersect.size()));
                    Contract parent2 = intersect.get(rand.nextInt(intersect.size()));
                    Contract[] childs = Crossover.cxOrdered(parent1, parent2);


                    newGeneration[currentNewGenerationCount] = childs[0];
                    newGeneration[currentNewGenerationCount + 1] = childs[1];

                    currentNewGenerationCount += 2;
                }

                List<Contract> newGenerationList = new ArrayList<>();
                newGenerationList.addAll(List.of(Arrays.copyOfRange(newGeneration, 0, currentNewGenerationCount)));

                newGenerationList = newGenerationList.stream().unordered().parallel().distinct().collect(Collectors.toCollection(ArrayList::new));

                lastRoundRandomBegin = newGenerationList.size();

                while (newGenerationList.size() < generationsSize) {
                    //If not enough contract fill with random

                    newGenerationList.addAll(Arrays.stream(med.getRandomContracts(generationsSize - newGenerationList.size())).toList());
                    //System.arraycopy(newGeneration, currentNewGenerationCount, newRandom, 0, newRandom.length);

                    newGenerationList = newGenerationList.stream().unordered().parallel().distinct().collect(Collectors.toCollection(ArrayList::new));
                }

                 */

                Contract [] newGeneration = newGenerationHashSet.toArray(Contract[]::new);

                // Mutate
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