import java.util.*;

public class Crossover {
    public static SplittableRandom rand = new SplittableRandom();

    public static Contract[] cxOrdered(Contract parent1, Contract parent2) {
        int[] child1 = new int[parent1.getContractSize()];
        int[] child2 = new int[parent2.getContractSize()];
        assert child1.length == child2.length;

        int[] parentContract1 = parent1.getContract();
        int[] parentContract2 = parent2.getContract();

        //Random rand = new Random();
        int firstPoint = rand.nextInt((parent1.getContractSize() - 2));
        int secondPoint = rand.nextInt((firstPoint + 1), parent1.getContractSize());

        //swap children part
        System.arraycopy(parentContract1, firstPoint, child2, firstPoint, (secondPoint - firstPoint));
        System.arraycopy(parentContract2, firstPoint, child1, firstPoint, (secondPoint - firstPoint));

        //Create newly ordered List from Parent 1 excuding coppied elements
        List<Integer> list1 = new ArrayList<>(Arrays.stream(parentContract1).boxed().toList());
        for (int i = firstPoint; i < secondPoint; i++) {
            boolean removed = list1.remove(Integer.valueOf(parentContract2[i]));
        }
        //fill first part of child array
        for (int i = 0; i < firstPoint; i++) {
            child1[i] = list1.removeFirst();
        }
        //fill second part of child array
        for (int i = secondPoint; i < child1.length; i++) {
            child1[i] = list1.removeFirst();
        }

        //Create newly ordered List from Parent 2 excuding coppied elements
        List<Integer> list2 = new ArrayList<>(Arrays.stream(parentContract2).boxed().toList());
        for (int i = firstPoint; i < secondPoint; i++) {
            boolean removed = list2.remove(Integer.valueOf(parentContract1[i]));
        }
        //fill first part of child array
        for (int i = 0; i < firstPoint; i++) {
            child2[i] = list2.removeFirst();
        }
        //fill second part of child array
        for (int i = secondPoint; i < child1.length; i++) {
            child2[i] = list2.removeFirst();
        }

        return new Contract[]{new Contract(child1), new Contract(child2)};
    }

    public static Contract[] cxPartiallyMapped(Contract parent1, Contract parent2) {
        int[] parentContract1 = parent1.getContract();
        int[] parentContract2 = parent2.getContract();

        int[] child1 = new int[parentContract1.length];
        Arrays.fill(child1, -1);
        int[] child2 = new int[parentContract2.length];
        Arrays.fill(child2, -1);
        assert child1.length == child2.length;

        int length = child1.length;

        int start_index = rand.nextInt(length - 2);
        int end_index = rand.nextInt(start_index + 1, length+1);

        System.arraycopy(parentContract1, start_index, child2, start_index, end_index - start_index);
        System.arraycopy(parentContract2, start_index, child1, start_index, end_index - start_index);

        ArrayList<Integer> lChild1 = new ArrayList<>(Arrays.stream(child1).boxed().toList());
        ArrayList<Integer> lChild2 = new ArrayList<>(Arrays.stream(child2).boxed().toList());

        for (int i = 0; i < length; i++) {
            Integer target = parentContract1[i];
            int target_index = lChild1.indexOf(target);

            if (target_index != -1 ) {
                if(lChild1.get(i) == -1)lChild1.set(i, findReplacement(lChild1, lChild2, target_index));
            } else {
                if(lChild1.get(i) == -1)lChild1.set(i, parentContract1[i]);
            }

        }

        for (int i = 0; i < length; i++) {
            Integer target = parentContract2[i];
            int target_index = lChild2.indexOf(target);

            if (target_index != -1) {
                if(lChild2.get(i) == -1)lChild2.set(i, findReplacement(lChild2, lChild1, target_index));
            } else {
                if(lChild2.get(i) == -1)lChild2.set(i, parentContract2[i]);
            }
        }

        return new Contract[]{new Contract(lChild1), new Contract(lChild2)};
    }

    private static Integer findReplacement(ArrayList<Integer> lChild1, ArrayList<Integer> lChild2, int index) {
        Integer target = lChild2.get(index);
        int target_index;
        if ((target_index = lChild1.indexOf(target)) != -1) {
            return findReplacement(lChild1, lChild2, target_index);
        } else {
            return target;
        }
    }

}
