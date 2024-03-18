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

    public static int[][] cxPartiallyMapped(int[] parent1, int[] parent2) {
        int[] child1 = new int[parent1.length];
        int[] child2 = new int[parent2.length];
        assert child1.length == child2.length;

        int length = child1.length;

        Random rand = new Random();

        int start_index = rand.nextInt(length - 2);
        int end_index = rand.nextInt(start_index + 1, length);

        System.arraycopy(parent1, start_index, child2, start_index, end_index - start_index);
        System.arraycopy(parent2, start_index, child1, start_index, end_index - start_index);

        ArrayList<Integer> lChild1 = new ArrayList<>(Arrays.stream(child1).boxed().toList());
        ArrayList<Integer> lChild2 = new ArrayList<>(Arrays.stream(child2).boxed().toList());

        for (int i = 0; i < length; i++) {
            Integer target = parent1[i];
            int target_index = lChild2.indexOf(target);
            if (target_index != -1) {
                lChild2.set(i, findReplacement(lChild1, lChild2, target_index));
            } else {
                lChild2.set(i, parent1[i]);
            }
        }

        for (int i = 0; i < length; i++) {
            Integer target = parent2[i];
            int target_index = lChild1.indexOf(target);
            if (target_index != -1) {
                lChild1.set(i, findReplacement(lChild2, lChild1, target_index));
            } else {
                lChild1.set(i, parent2[i]);
            }
        }

        return new int[][]{lChild1.stream().mapToInt(i -> i).toArray(), lChild2.stream().mapToInt(i -> i).toArray()};
    }

    private static Integer findReplacement(ArrayList<Integer> lChild1, ArrayList<Integer> lChild2, int index) {
        Integer target = lChild1.get(index);
        int target_index;
        if ((target_index = lChild2.indexOf(target)) != -1) {
            return findReplacement(lChild2, lChild1, target_index);
        } else {
            return target;
        }
    }

}
