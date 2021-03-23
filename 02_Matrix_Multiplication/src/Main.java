import java.util.Arrays;
import java.util.Random;

public class Main {
    private static double[][] times;

    public static void main(String[] args) {
        int[] matrixSizes = {100, 200, 500, 1000};

        double[][] speedups = getSpeedups(matrixSizes);
        printSpeedups(speedups, matrixSizes);
        int nrOfAlg = 6;
        double[][] comparison = compareToFastest(matrixSizes.length, nrOfAlg);
        printComparison(comparison, matrixSizes);
    }


    private static void printComparison(double[][] comparison, int[] matrixSizes) {
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("\nSpeedups for Matrix: %d x %d (Every Algorithm/Fastest Algorithm):%n", matrixSizes[i], matrixSizes[i]);
            for (int j = 0; j < comparison[i].length; j++){
                switch (j) {
                    case 0: System.out.printf("\tClassic              :\t%.4f%n", comparison[i][j]);  break;
                    case 1: System.out.printf("\tA-transposed         :\t%.4f%n", comparison[i][j]);  break;
                    case 2: System.out.printf("\tB-Transposed         :\t%.4f%n", comparison[i][j]);  break;
                    case 3: System.out.printf("\tClassic      Parallel:\t%.4f%n", comparison[i][j]);  break;
                    case 4: System.out.printf("\tA-Transposed Parallel:\t%.4f%n", comparison[i][j]);  break;
                    case 5: System.out.printf("\tB-Transposed Parallel:\t%.4f%n", comparison[i][j]);  break;
                    default: break;
                }
            }
        }
    }


    /**
     * Comparing each algorithm to the best and returning the speedups.
     * @param n number matrix sizes
     * @param m number of algorithms
     * @return 2d array
     */
    private static double[][] compareToFastest(int n, int m){
        // order: [ [e x 6] x 4 ],  4 = matrix sizes and 6 = nr of algorithms.
        double[][] comparisons = new double[n][m];
        for (int i = 0; i < n; i++){
            comparisons[i] = compare(times[i]);
        }
        return comparisons;
    }


    private static double[] compare(double[] mTimes) {
        int fastestIndex = 0;
        double[] comparison = new double[mTimes.length];
        for (int i = 1; i < mTimes.length; i++)                     // find best time
            if (mTimes[i] < mTimes[fastestIndex]) fastestIndex = i;
        for (int i = 0; i < mTimes.length; i++)
            comparison[i] = mTimes[i] / mTimes[fastestIndex];
        return comparison;
    }


    /**
     * Calculates and returns speedups for each array size in a shape of n x m array,
     * where n = number of array sizes, and m = array of speedups for array size n.
     * @param matrixSizes different matrix sizes
     * @return 2d array containing speedups for each array size.
     */
    private static double[][] getSpeedups(int[] matrixSizes) {
        int seed = 42;
        int nrOfAlg = 6;
        Workers workers;
        double[][] a, b;
        double[][] speedups = new double[matrixSizes.length][nrOfAlg/2];
        times = new double[matrixSizes.length][nrOfAlg];
        double[] medianTimes;

        for (int i = 0; i < matrixSizes.length; i++) {
            a = Oblig2Precode.generateMatrixA(seed, matrixSizes[i]);
            b = Oblig2Precode.generateMatrixB(seed, matrixSizes[i]);
            workers = new Workers(a, b);

            medianTimes = getMedianTimes(a, b, workers);   // updates global medianTimes variable
            times[i] = medianTimes;
            speedups[i] = speedup(medianTimes);
        }
        // order:[ speedupsFor100x100, ... , speedupsFor1000x1000 ]
        return speedups;
    }


    private static double[] getMedianTimes(double[][] a, double[][] b, Workers workers){
        int repeat = 7;
        int nrOfAlg = 6;
        double[][] times = new double[nrOfAlg][repeat];
        // order: [timeClassic, timeRotateA, timeRotateB, timeClassicPar, timeRotateAPar, timeRotateBPar]
        double[] medianTimes = new double[nrOfAlg];

        for (int i = 0; i < nrOfAlg; i++){
            for (int j = 0; j < repeat; j++) {
                times[i][j] = timeAlgorithm(i, a, b, workers);
            }
        }
        for (int i = 0; i < nrOfAlg; i++){
            medianTimes[i] = getMedian(times[i]);
        }
        return medianTimes;
    }


    private static double timeAlgorithm(int choice, double[][] a, double[][] b, Workers workers){
        double start, end;
        double[][] c = new double[a.length][a.length];

        start = System.nanoTime();
        switch (choice){
            case 0: multiplyWithoutRotation(a, b, c); break;
            case 1: multiplyByRotatingA(a, b, c);     break;
            case 2: multiplyByRotatingB(a, b, c);     break;
            case 3: workers.runWorker(0);      break;   // Classic parallel
            case 4: workers.runWorker(1);      break;   // A Transposed parallel
            default: workers.runWorker(2);     break;   // B Transposed parallel
        }
        end = System.nanoTime();
        return  (end - start) / 1000000; // milli
    }


    private static double getMedian(double[] times){
        Arrays.sort(times);
        return times[times.length / 2];
    }


    private static double[] speedup(double[] medianTimes){
        int repeat = medianTimes.length / 2;
        double[] speedups = new double[repeat];
        for (int i = 0; i < repeat; i++){
            speedups[i] = medianTimes[i] / medianTimes[i+3];
        }
        return speedups;
    }


    private static void printSpeedups(double[][] speedups, int[] matrixSizes) {
        for (int i = 0; i < matrixSizes.length; i++){   // for each array size
            System.out.printf("\nSpeedups for Matrix: %d x %d (all results are Sequential/Parallel):%n", matrixSizes[i], matrixSizes[i]);
            for (int j = 0; j < speedups[i].length; j++){
                switch (j) {
                    case 0: System.out.printf("\tClassic      vs Classic\t\t:\t\t%.4f%n", speedups[i][j]);  break;
                    case 1: System.out.printf("\tA-transposed vs A-Transposed:\t\t%.4f%n", speedups[i][j]); break;
                    case 2: System.out.printf("\tB-Transposed vs B-Transposed:\t\t%.4f%n", speedups[i][j]); break;
                    default: break;
                }
            }
        }
    }



    /**
     * This is what multiplyByRotatingA & multiplyByRotatingB functions use to rotate the A & B matrix.
     * Flip rotate the matrix, where lower left corner is flipped with upper right.
     * In other words, every value is swapped unless the values in (upper lef to lower right) diagonal.
     * Assumes all rows have same length.
     * @param matrix 2d array
     * @return rotated matrix
     */
    private static double[][] transpose(double[][] matrix){
        int size = matrix.length;
        double[][] rotatedMatrix = new double[size][size];

        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                rotatedMatrix[i][j] = matrix[j][i];
            }
        }
        return rotatedMatrix;
    }



    /**
     * Rotates B and multiplies a and b.
     * For simplicity this function assumes a and b have the same length (nr of rows).
     * @param a first matrix
     * @param b second matrix
     */
    private static void multiplyByRotatingA(double[][] a, double[][] b, double[][] c){
        int size = a.length;
        double[][] _a = transpose(a);
        double sum;

        // multiply each row of a, with all rows in rotated b
        for (int i = 0; i < size; i++){
            for (int j = 0;j < size; j++) {
                sum = 0;
                for (int k = 0; k < size; k++){
                    sum += _a[k][i] * b[k][j];
                }
                c[i][j] = sum;
            }
        }
    }


    /**
     * Rotates B and multiplies a and b.
     * For simplicity this function assumes a and b have the same length (nr of rows).
     * @param a first matrix
     * @param b second matrix
     */
    private static void multiplyByRotatingB(double[][] a, double[][] b, double[][] c){
        int size = a.length;
        double[][] _b = transpose(b);
        double sum;

        // multiply each row of a, with all rows in rotated b
        for (int i = 0; i < size; i++){
            for (int j = 0;j < size; j++) {
                sum = 0;
                for (int k = 0; k < size; k++){
                    sum += a[i][k] * _b[j][k];
                }
                c[i][j] = sum;
            }
        }
    }


    /** For simplicity this function assumes a and b have the same length (nr of rows).
     * @param a first matrix
     * @param b second matrix
     */
    private static void multiplyWithoutRotation(double[][] a, double[][] b, double[][] c){
        int size = a.length;
        double sum;
        // multiply each row of a, with all rows in rotated b
        for (int i = 0; i < size; i++){
            for (int j = 0;j < size; j++) {
                sum = 0;
                for (int k = 0; k < size; k++){
                    sum += a[i][k] * b[k][j];
                }
                c[i][j] = sum;
            }
        }
    }


    private static boolean checkResultCorrection(){
        return false;
    }

//    /** Left rotate the matrix. Assumes all rows have same length.
//     * @param matrix 2d array
//     * @return rotated matrix
//     */
//    private static int[][] lRotate(int[][] matrix){
//        int nrRows = matrix.length;
//        int nrCols = matrix[0].length;
//        int[][] rotatedMatrix = new int[nrRows][nrCols];
//
//        for (int i = 0; i < nrCols; i++){
//            for (int j = 0; j < nrRows; j++){
//                rotatedMatrix[i][j] = matrix[j][i];
//            }
//        }
//        return rotatedMatrix;
//    }
//
//
//    /** Right rotate the matrix. Assumes all rows have same length.
//     * @param matrix 2d array
//     * @return rotated matrix
//     */
//    private static int[][] rRotate(int[][] matrix){
//        int nrRows = matrix.length;
//        int nrCols = matrix[0].length;
//        int[][] rotatedMatrix = new int[nrRows][nrCols];
//
//        for (int i = 0; i < nrCols; i++){
//            int k = 0;
//            for (int j = nrRows-1; j >= 0; j--){
//                rotatedMatrix[i][k] = matrix[j][i];
//                k++;
//            }
//        }
//        return rotatedMatrix;
//    }
//
//    private static double[][] createMatrix(int rowLen, int colLen){
//        Random r = new Random();
//        double[][] matrix = new double[rowLen][colLen];
//
//        for (int i = 0; i < rowLen; i++){
//            for (int j = 0; j < colLen; j++){
//                matrix[i][j] = r.nextDouble();
//            }
//        }
//        return matrix;
//    }
//    private static void printMatrix(double[][] matrix){
//        System.out.println();
//        for (double[] el : matrix) {
//            System.out.println(Arrays.toString(el));
//        }
//    }
}
