import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class TheMain {
    static int seed = 2021;

    public static void main(String[] args) {

        // is used to start automatic program for different n values
        if (args.length == 0){
            int[] nVals = {100, 1000, 10000, 100000, 1000000, 10000000, 100000000};
            long[][] times = timeAlgorithms(nVals);
            saveTimes(times, nVals);
            System.out.println("INFO: Check timings.txt in materials folder.");
        }
        else if (args.length > 1 && args.length < 4){
            int k = 0;
            int n = Integer.parseInt(args[0]);
            seed = Integer.parseInt(args[1]);
            if (args.length == 3)
                k = Integer.parseInt(args[2]);
            runUserCommands(n, k);
        }
        else {
            System.out.println("Options to run the program:");
            System.out.println("1: give no argument to run automatic testing!");
            System.out.println("2: <n> <seed>");
            System.out.println("3: <n> <seed> <k>");
            System.out.println("where n is number of points and k is number of threads.");
            System.exit(1);
        }
    }

    /**
     * Runs the program with user specified commands
     * @param n number of points
     * @param k number of threads
     */
    private static void runUserCommands(int n, int k) {
        int[] x = new int[n];
        int[] y = new int[n];
        NPunkter17 np = new NPunkter17(n, seed);
        np.fyllArrayer(x, y);
        IntList points = np.lagIntList();

        /* SEQUENTIAL */
        long s1 = System.currentTimeMillis();
        ConvexHull ch = new ConvexHull(n, x, y, points);
        IntList convexHull = ch.quickHull();
        long time1 = (System.currentTimeMillis() - s1);
        System.out.printf("Sequential time : %,d milli seconds%n", time1);

        /* PARALLEL */
        long s2 = System.currentTimeMillis();
        ParallelConvexHull pch = new ParallelConvexHull(n, k, x, y, points);
        ConvexHull parConvexHull = pch.work();
        IntList finalIntList = parConvexHull.quickHull();
        long time2 = (System.currentTimeMillis() - s2);
        System.out.printf("Parallel time   : %,d milli seconds%n", time2 );

        System.out.printf("Speed up for N = %,d : %.2f%n" , n, ((double) time1 / time2));

        if (n <= 10000) {
            Oblig5Precode opSeq = new Oblig5Precode(ch, convexHull);

            // Writing par results to file
            Oblig5Precode opPar = new Oblig5Precode(parConvexHull, finalIntList);
            opPar.writeHullPoints();

            // Drawing both seq and par graph for n <= 1000 for the sake of comparison
            if (n <= 1000) {
                opSeq.drawGraph();
                opPar.drawGraph();
            }
            else
                System.out.println("\nINFO: Not drawing GUI graph for n > 1000!" +
                        "\nCheck materials folder for output!");
        }
        else
            System.out.println("\nINFO: Not saving the points for n > 10000!" +
                "\nTry with lower n values!");

    }


    private static long[][] timeAlgorithms(int[] nVals){
        // Timings for both seq and par algorithms
        long[][] times = new long[2][nVals.length];
        long[] medianTimes;
        for (int i = 0; i < nVals.length; i++) {
            System.out.printf("Timing seq and par for N: %,d%n", nVals[i]);
            medianTimes = multipleTimer(nVals[i]);
            times[0][i] = medianTimes[0];
            times[1][i] = medianTimes[1];
        }
        return times;
    }


    private static long[] multipleTimer(int nVal) {
        int[] x = new int[nVal];
        int[] y = new int[nVal];
        IntList points;
        NPunkter17 np = new NPunkter17(nVal, seed);
        np.fyllArrayer(x, y);
        points = np.lagIntList();

        long[][] times = new long[2][7];
        long s;
        for (int i = 0; i < 7; i++) {
            s = System.currentTimeMillis();
            ConvexHull ch = new ConvexHull(nVal, x, y, points);
            ch.quickHull();
            times[0][i] = System.currentTimeMillis() - s;

            s = System.currentTimeMillis();
            ParallelConvexHull pch = new ParallelConvexHull(nVal, 0, x, y, points);
            ConvexHull parConvexHull = pch.work();
            parConvexHull.quickHull();
            times[1][i] = System.currentTimeMillis() - s;
        }
        long[] medianTimes = new long[2];
        medianTimes[0] = medianTime(times[0]);
        medianTimes[1] = medianTime(times[1]);

        return medianTimes;
    }


    private static long medianTime(long[] times) {
        Arrays.sort(times);
        return times[times.length / 2];
    }


    private static void saveTimes(long[][] times, int[] nVals) {
        Path path = Paths.get(".."+ File.separator +"materials" + File.separator + "timings.txt");
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)){
            for (int i = 0; i < nVals.length; i++) {
                bufferedWriter.write(String.format("N: %,d%n" +
                        "Seq time: %,d milli seconds%n" +
                        "Par time: %,d milli seconds%n" +
                        "Speedup : %.2f%n%n",
                        nVals[i], times[0][i], times[1][i], ((double) times[0][i]/times[1][i])));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
