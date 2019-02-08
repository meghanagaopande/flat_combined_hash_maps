import HashMaps.*;
import Threads.TestThread;


import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

public class HashMapTest {
    private static final String COARSE = "CoarseHashSet";
    private static final String FINE = "StripedHashSet";
    private static final String LOCKFREE = "LockFreeHashSet";
    private static final String FCSINGLE = "FlatCombinedHashSetSingleCombiner";
    private static final String FCREFINABLE = "FlatCombinedHashSetRefinable";
    private static final String FCSTRIPED = "FlatCombinedHashSetStriped";
    private static final String FCLOCKFREE = "FlatCombinedHashSetLockfree";

    public static void main(String[] args) throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String HashClass = (args.length==0 ? FCREFINABLE : args[0]);

        final int THREAD_COUNT = (args.length==0 ? 4 : Integer.parseInt(args[1]));

        final int DURATION = (args.length==0 ? 5 : Integer.parseInt(args[2]));

        long start,end;
        long throughput = 0;
        long max = 0;

        final int SIZE = (args.length==0 ? 1024 : Integer.parseInt(args[3]));
        final double CONTAINS = (args.length==0 ? 0.5 : Double.parseDouble(args[4]));

        for(int warmup = 0; warmup < 20; warmup++) {
            HashMapInterface<Integer> H;
            AtomicBoolean keep_running = new AtomicBoolean(true);

            if (HashClass.equals("FlatCombinedHashSetSingleCombiner") || HashClass.equals("FlatCombinedHashSetRefinable") || HashClass.equals("FlatCombinedHashSetStriped") ||HashClass.equals("FlatCombinedHashSetLockfree")) {
                H = (HashMapInterface<Integer>) Class.forName("HashMaps." + HashClass).getConstructor(int.class, int.class).newInstance(SIZE, THREAD_COUNT);

            } else {
                H = (HashMapInterface<Integer>) Class.forName("HashMaps." + HashClass).getConstructor(int.class).newInstance(SIZE);
            }


            final TestThread[] threads = new TestThread[THREAD_COUNT];


            for (int t = 0; t < THREAD_COUNT; t++)
                threads[t] = new TestThread(H, keep_running, CONTAINS);


            start = System.currentTimeMillis();
            for (int t = 0; t < THREAD_COUNT; t++) {
                threads[t].start();
            }

            do {
                end = System.currentTimeMillis();
            }
            while ((end - start) < (DURATION * 1000));

            keep_running.set(false);

            int operations = 0;
            for (int t = 0; t < THREAD_COUNT; t++) {

                //threads[t].join();
                operations += threads[t].operations;
            }


            throughput = (operations) / DURATION;
            if(throughput > max)
            {
              max = throughput;
            }
            //System.out.println(throughput);
        }

        System.out.println(max);

    }
}
