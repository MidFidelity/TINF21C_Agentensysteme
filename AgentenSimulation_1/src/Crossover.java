import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Crossover {
    public static int[][] cxOrdered(int[] parent1, int[] parent2){
        int[] child1 = new int[parent1.length];
        int[] child2 = new int[parent2.length];
        assert child1.length == child2.length;

        Random rand = new Random();
        int firstPoint = rand.nextInt((parent1.length-2));
        int secondPoint = rand.nextInt((firstPoint+1), parent1.length);

        //swap children part
        System.arraycopy(parent1, firstPoint, child2, firstPoint, (secondPoint-firstPoint));
        System.arraycopy(parent2, firstPoint, child1, firstPoint, (secondPoint-firstPoint));

        //Create newly ordered List from Parent 1 excuding coppied elements
        List<Integer> list1 = new ArrayList<>(Arrays.stream(parent1).boxed().toList());
        for(int i = firstPoint; i<secondPoint; i++){
            boolean removed = list1.remove(Integer.valueOf(parent2[i]));
        }
        //fill first part of child array
        for(int i = 0; i<firstPoint; i++){
            child1[i] = list1.removeFirst();
        }
        //fill second part of child array
        for (int i = secondPoint; i < child1.length; i++){
            child1[i] = list1.removeFirst();
        }

        //Create newly ordered List from Parent 2 excuding coppied elements
        List<Integer> list2 = new ArrayList<>(Arrays.stream(parent2).boxed().toList());
        for(int i = firstPoint; i<secondPoint; i++){
            boolean removed = list2.remove(Integer.valueOf(parent1[i]));
        }
        //fill first part of child array
        for(int i = 0; i<firstPoint; i++){
            child2[i] = list2.removeFirst();
        }
        //fill second part of child array
        for (int i = secondPoint; i < child1.length; i++){
            child2[i] = list2.removeFirst();
        }

        return new int[][] {child1, child2};
    }

    public static int[][] cxPartiallyMapped(int[] parent1, int[] parent2){
        return null;
    }
}
