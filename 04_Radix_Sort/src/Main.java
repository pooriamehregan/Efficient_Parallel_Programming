import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int n, seed, useBits;

        if (args.length > 0) {
            try {
                n = Integer.parseInt(args[0]);
                seed = Integer.parseInt(args[1]);
                useBits = Integer.parseInt(args[2]);
                executeUserCommand(n, seed, useBits);
            } catch (Exception e) {
                System.out.println("Correct usage is: java Main <n> <seed> <useBits>");
            }
        }
        else {
            int[] values = {1000, 10000, 100000, 1000000, 10000000, 100000000};
            long[][] timings = executeAutomaticCommand(values);
            double[] speedups = getSpeedups(timings);
            writeTimesToFile(values, timings, speedups);
        }


        /* The algorithm does automatically check correctness, but the line below
        * can be used to do a manual check */
        // params: a, b, n, seed, useBits
        // checkCorrectness(null, null, 1000, 2021, 8);
    }


    private static void checkCorrectness(int[] a, int[] b, int n, int seed, int useBits) {
        if (a == null || b == null){
            a = Oblig4Precode.generateArray(n, seed);
            b = a.clone();
        }

        RadixSort rs = new RadixSort(useBits);
        a = rs.radixSort(a);

        ParallelRadixSort prs = new ParallelRadixSort(useBits);
        prs.radixSort(b);

        // Quick check to see if sorted (takes a few seconds at high n's)
        int[] arraysort = Oblig4Precode.generateArray(n, seed);
        Arrays.sort(arraysort);

        boolean A_Equals_B = Arrays.equals(arraysort, a) && Arrays.equals(arraysort, b);
        if (A_Equals_B) System.out.println("\nArrays are correctly sorted!");
        else System.out.println("\nArrays are NOT correctly sorted!");
    }

    /**
     * Executes user defined command
     * @param n array size
     * @param seed seed to create a random array
     * @param useBits are number of bits to sort on
     * @return median timings as an array of length 2, [timingSeq, timingPar]
     */
    private static long[] executeUserCommand(int n, int seed, int useBits) {
        long[][] timings = getTimings(n, seed, useBits);
        long[] medians = getMedianTimings(timings);
        printOnConsole(n, medians);
        return medians;
    }


    private static long[][] executeAutomaticCommand(int[] values) {
        // Predefined values to be used
        int seed = 2021;
        int useBits = 8;

        long[][] timings = new long[values.length][2];

        for (int i = 0; i < values.length; i++) {
            timings[i] = executeUserCommand(values[i], seed, useBits);
        }
        return timings;
    }


    private static long[][] getTimings(int n, int seed, int useBits){
        long start;
        int[] a = {}, b = {};
        long[][] timings = new long[2][7];

        for (int i = 0; i < 7; i++){
            a = Oblig4Precode.generateArray(n, seed);
            b = a.clone();

            start = System.currentTimeMillis();
            RadixSort rs = new RadixSort(useBits);
            rs.radixSort(a);
            timings[0][i] = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            ParallelRadixSort prs = new ParallelRadixSort(useBits);
            prs.radixSort(b);
            timings[1][i] = System.currentTimeMillis() - start;
        }

        /* Saving some results using Pre-code, check out the "out" folder */
        Oblig4Precode.saveResults(Oblig4Precode.Algorithm.SEQ, seed, a);
        Oblig4Precode.saveResults(Oblig4Precode.Algorithm.PAR, seed, b);

        checkCorrectness(a, b, n, seed, useBits);

        return timings;
    }


    private static long[] getMedianTimings(long[][] timings) {
        long[] medians = new long[2];
        Arrays.sort(timings[0]);
        medians[0] = timings[0][timings.length / 2];

        Arrays.sort(timings[1]);
        medians[1] = timings[1][timings.length / 2];

        return medians;
    }


    private static void printOnConsole(int n, long[] medians) {
        String out = String.format("N = %,d%n" +
                        "\tSequential          : %,d millis%n" +
                        "\tParallel            : %,d millis%n" +
                        "\tSpeedup             : %.2f millis%n",
                        n, medians[0], medians[1], ((double) medians[0]/medians[1]));
        System.out.print(out);
    }


    private static double[] getSpeedups (long[][] timings){
        double[] speedups = new double[timings.length];
        for (int i = 0; i < timings.length; i++){
            speedups[i] = (double) timings[i][0] / timings[i][1];
        }
        return speedups;
    }


    private static void writeTimesToFile(int[] values, long[][] timings, double[] speedups) {
        Path path = Paths.get("04_Radix_Sort"+ File.separator + "out" + File.separator + "timings.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.flush();

            for (int i = 0; i < values.length; i++) {
                writer.write(String.format("N = %,d%n" +
                                "\tSequential          : %,d millis%n" +
                                "\tParallel            : %,d millis%n" +
                                "\tSpeedup             : %.2f millis%n%n",
                                values[i], timings[i][0], timings[i][1], speedups[i]));
            }

            writer.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
