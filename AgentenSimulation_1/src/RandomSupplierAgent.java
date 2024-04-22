import java.io.File;
import java.io.FileNotFoundException;
import java.util.SplittableRandom;

public class RandomSupplierAgent extends SupplierAgent {

    private final SplittableRandom rand = new SplittableRandom();

    public RandomSupplierAgent(File file) throws FileNotFoundException {
        super(file);
    }

    @Override
    public boolean[] voteLoop(Contract[] contracts, int acceptanceAmount) {
        super.voteLoop(contracts, acceptanceAmount);

        boolean[] votes = new boolean[contracts.length];
        int accepted = 0;

        while (accepted < acceptanceAmount) {
            int random = rand.nextInt(contracts.length);
            if (votes[random]) continue;
            votes[random] = true;
            accepted++;
        }

        return votes;
    }

    @Override
    public int voteEnd(Contract[] contracts) {
        super.voteEnd(contracts);

        return rand.nextInt(contracts.length);
    }
}
