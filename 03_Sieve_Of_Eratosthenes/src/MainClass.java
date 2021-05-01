import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainClass {
    private static SieveOfEratosthenes soe;
    private static ParallelSieve ps;
    private static ParallelFactorization pf;

    /**
     * Starts the program, and decides whether to automate the program or use user inputs.
     *
     * @param args could either have n and k, or be empty.
     */
    public static void main(String[] args) {
        int n;
        int k;
        if (args.length > 0) {
            try {
                n = Integer.parseInt(args[0]);
                k = Integer.parseInt(args[1]);
                if (n <= 16 || k < 0) throw new Exception();
            } catch (Exception e) {
                System.out.println("Correct use of program is: " +
                        "java SieveOfEratosthenes <n> <k>, where n >= 16 and k >= 0.");
                return;
            }
            executeUserAction(n, k);    // run user-defined action
        } else {                        // run automatic action
            int[] values = {2000000, 20000000, 200000000, 2000000000};
            long[][] timings = executeAutomaticAction(values);
            writeTimesToFile(values, timings);
        }
    }

    /**
     * Gets called when user gives no argument to the program.
     *
     * @param values are an int array of size for n values: 2 million, 20 million, 200 million, 2 billion.
     * @return a double array which includes timings for all 4 different n values.
     */
    private static long[][] executeAutomaticAction(int[] values) {
        long[][] timings = new long[values.length][values.length];
        for (int i = 0; i < values.length; i++) {
            timings[i] = executeUserAction(values[i], 0);
        }
        return timings;
    }

    /**
     * Gets called when user specifies n and k at command line.
     *
     * @param n factorization number.
     * @param k number of threads.
     * @return array of timings.
     */
    private static long[] executeUserAction(int n, int k) {
        soe = new SieveOfEratosthenes(n);
        ps = new ParallelSieve(n, k);
        pf = new ParallelFactorization(n, k);

        System.out.printf("%nTiming the 4 algorithms 7 times for %,d.%n", n);
        long[] times = getAlgorithmTimes(n);

        // find the 100 largest values less than n * n
        long nSquared = (long) n * (long) n;
        long[] largests = getLargest(nSquared);

        System.out.printf("Factorizing 100 largest values less than %,d.%n" , nSquared);
        ArrayList<ArrayList<Long>> factors = parFactorize(largests);

        System.out.printf("Printing factorizations for %,d to file.%n", n);
        printToFiles(n, largests, factors);

        return times;
    }


    /**
     * Times all 4 algorithms.
     *
     * @param n factorization number.
     * @return long array of timings for all 4 algorithms.
     */
    private static long[] getAlgorithmTimes(int n) {
        long[] times = new long[4];
        for (int i = 0; i < times.length; i++) {
            times[i] = getMedianTiming(i + 1, n);
        }
        return times;
    }

    /**
     * Times an algorithm decided by choice 7 times, and return the median time to take
     * advantage of jit compilation.
     *
     * @param choice the algorithm: 1 for seq sieve, 2 for par sieve, 3 for seq factor, 4 for par factor
     * @param n      the factorization number
     * @return the median time
     */
    private static long getMedianTiming(int choice, int n) {
        long start;
        long[] timings = new long[7];

        for (int i = 0; i < timings.length; i++) {
            start = System.nanoTime();
            switch (choice) {
                case 1: {
                    soe.getPrimes(); break;
                }
                case 2: {
                    ps.work(); break;
                }
                case 3: {
                    SieveOfEratosthenes.factor(soe.getPrimes(), n); break;
                }
                case 4: {
                    pf.work(n); break;
                }
                default:
                    System.out.println("Choice is not valid!"); break;
            }
            timings[i] = (System.nanoTime() - start) / 1000000;
        }

        return getMedian(timings);
    }

    /**
     * Sorts the given long array and returns the median number.
     *
     * @param timings long array of timings
     * @return a long which is the median number
     */
    private static long getMedian(long[] timings) {
        Arrays.sort(timings);
        return timings[timings.length / 2];
    }

    /**
     * @param n is a long number, resulted from n * n when we were finding primes with sieve.
     * @return returns 100 largest less than (n excluded).
     */
    static long[] getLargest(long n) {
        long[] largests = new long[100];
        int j = 0;
        for (long i = n - 100; i < n; i++) largests[j++] = i;
        return largests;
    }


    /**
     * @param largests long array of 100 largest numbers less than (exclusive) n
     * @return an array of arraylist, where each arraylist is factors of a number.
     */
    private static ArrayList<ArrayList<Long>> parFactorize(long[] largests) {
        ArrayList<ArrayList<Long>> factors = new ArrayList<>();

        for (long largest : largests)
            factors.add(pf.work(largest));

        return factors;
    }


    /**
     * @param largests long array of 100 largest numbers less than (exclusive) n
     * @return an array of arraylist, where each arraylist is factors of a number.
     */
    private static ArrayList<ArrayList<Long>> seqFactorize(long[] largests) {
        int[] primes = soe.getPrimes();
        ArrayList<ArrayList<Long>> factors = new ArrayList<>();

        for (long largest : largests)
            factors.add(SieveOfEratosthenes.factor(primes, largest));

        return factors;
    }

    /**
     * Uses precode (Oblig3Precode) to print factors to file.
     *
     * @param n        is the size of the array: 2 million, 20 million, 200 million, 2 billion
     * @param largests long array of 100 largest numbers less than (exclusive) n
     * @param factors  prime factors for each large number
     */
    private static void printToFiles(int n, long[] largests, ArrayList<ArrayList<Long>> factors) {
        Oblig3Precode o3p = new Oblig3Precode(n);
        for (int i = 0; i < largests.length; i++) {
            for (int j = 0; j < factors.get(i).size(); j++) {
                o3p.addFactor(largests[i], factors.get(i).get(j));
            }
        }
        o3p.writeFactors();
    }

    /**
     * @param timings [[]--> 4 * algorithmTiming ]--> 4 * differentInputSizes
     */
    private static void writeTimesToFile(int[] values, long[][] timings) {
        Path path = Paths.get(".."+File.separator+"materials"+ File.separator +"timings.txt");
//        Path path = Paths.get("materials"+ File.separator +"timings.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.flush();
            writer.write("Times in millisecond:\n");

            for (int i = 0; i < timings.length; i++) {
                writer.write(String.format("N = %,d%n" +
                                "\tSequential Sieve          : %d%n" +
                                "\tParallel Sieve            : %d%n" +
                                "\tSequential factorization  : %d%n" +
                                "\tParallel Factorization    : %d%n%n",
                        values[i], timings[i][0], timings[i][1], timings[i][2], timings[i][3]));
            }
            writer.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
