
import java.util.Arrays;
import java.util.Random;


public class Main {

    /**
     * Finding and moving k largest elements to the front of an array, in a sorted (decent) order.
     * Example: k = 3, a=[2, 4, 6, 8, 1] ---> a.sorted = [8, 6, 4, 1, 2]. the order of two last items isn't important.
     * It assumes that there are more than k elements in the array.
     * @param a int array of length h.
     * @param k number of largest ints sorted (Decent)
     */
    private void insertSort (int[] a, int k) {
        int j, temp;
        // First step is to sort the first k elements
        for (int i = 1; i < k; i++) {
            j = i;
            while (j > 0 && a[j] > a[j-1]){
                temp = a[j -1];
                a[j-1] = a[j];
                a[j] = temp;
                j--;
            }
        }
        // second step is to start from the k and go up to n-1 elements,
        // in each iteration .
        for (int i = k ; i < a.length; i++) {
            // if you find a grater value than the smallest of first K elements,
            // change their location and put the founded element in right place so that first K elements
            // are still sorted from largest to smallest.
            if (a[i] > a[k-1]){  // found an int greater than
                temp = a[i];
                a[i] = a[k-1];
                a[k-1] = a[i];
                j = k-1;
                while (j > 0 && a[j-1] < temp){
                    a[j] = a[j-1];
                    j--;
                }
                a[j] = temp;
            }
        }
    } // end insertSort

    public int[] randomArray(int n, int seed) {
        Random r = new Random(seed);
        int[] a = new int[n];

        for (int i = 0; i < a.length; i++) {
            a[i] = r.nextInt(n);
        }
        return a;
    }

    private boolean kLargestAreEquel(int[] a, int[] b, int[] c, int k){
        for (int i = 0; i < k; i++) {
            if (a[a.length - i - 1] != b[i]) {
                System.out.println("a[i]:" + a[a.length - i - 1] + "\tb[i]" + b[i]);
                return false;
            }

            if (a[a.length - i - 1] != c[i]) {
                System.out.println("a[i]:" + a[a.length - i - 1] + "\tc[i]" + c[i]);
                return false;
            }
        }
        return true;
    }

    private void reverse(int[] validData){
        for(int i = 0; i < validData.length / 2; i++)
        {
            int temp = validData[i];
            validData[i] = validData[validData.length - i - 1];
            validData[validData.length - i - 1] = temp;
        }
    }

    private double time(int[] array, int k, int methode){
        long start, end;
        K_Largest p = null;
        if (methode == 3) p = new K_Largest(array, k);

        start = System.nanoTime();
        if (methode == 1) Arrays.sort(array);
        else if (methode == 2) insertSort(array, k);
        else p.init();
        end = System.nanoTime();

        return  (end - start) / 1000000.0;
    }

    private void printInfo(int[][] array, double[][][] times) {
        // Prints time info
        String s;
        for (int x = 0; x < 3; x++){
            System.out.println();
            for (int i = 0; i < array[0].length; i++) {
                System.out.printf("A%d:\tN: %-9d", x+1, array[0][i]);
                for (int j = 0; j < array[1].length; j++) {
                    s = (j > 0) ? "\t\t\t" : "\t";
                    System.out.printf("%sK: %-3d\ttime: %-9.4f%n", s, array[1][j], times[x][i][j]);
                }
            }
        }
    }
    
    private void test(int seed, int[] n_array, int[] k_array) {
        int[] array1 = new int[0], array2 = new int[0], array3 = new int[0];
        double[][] times_a1 = new double[n_array.length][k_array.length];
        double[][] times_a2 = new double[n_array.length][k_array.length];
        double[][] times_a3 = new double[n_array.length][k_array.length];
        double[] median_a1 = new double[7];
        double[] median_a2 = new double[7];
        double[] median_a3 = new double[7];

        System.out.println();
        for (int i = 0; i < n_array.length; i++) {
            for (int j = 0; j < k_array.length; j++) {
                for (int x = 0; x < median_a1.length; x++) {
                    array1 = randomArray(n_array[i], seed);
                    array2 = Arrays.copyOf(array1, array1.length);
                    array3 = Arrays.copyOf(array1, array1.length);
                    median_a1[x] = time(array1, k_array[j], 1);
                    median_a2[x] = time(array2, k_array[j], 2);
                    median_a3[x] = time(array3, k_array[j], 3);
                }
                if (kLargestAreEquel(array1, array2, array3, k_array[j]))
                     System.out.printf("Array1 == Array2 == Array3\tN: %-9d\tK: %-3d%n", n_array[i], k_array[j]);
                else System.out.printf("Arrays are not equal!\tN: %-9d\tK: %-3d%n", n_array[i], k_array[j]);

                Arrays.sort(median_a1);
                Arrays.sort(median_a2);
                Arrays.sort(median_a3);
                times_a1[i][j] = median_a1[median_a1.length / 2];
                times_a2[i][j] = median_a2[median_a2.length / 2];
                times_a3[i][j] = median_a3[median_a3.length / 2];
            }
        }
        printInfo(new int[][]{n_array, k_array}, new double[][][]{times_a1, times_a2, times_a3});
    }



    /**
     * @param args lenght 2:
     *             args[0] = n, which is length of the array to be sorted
     */
    public static void main(String [] args) {
        Main m = new Main();
        int seed = 100;

        if (args.length == 2) {                                                         // Take input from user
            int n = Integer.parseInt(args[0]);
            int k = Integer.parseInt(args[1]);
            m.test(seed, new int[]{n}, new int[]{k});
            return;
        }
        System.out.println("\nToo few Arguments were given! Running automated test!");  // or run automated tests, if no input were given, for n 1000 to 100 000 000
        int[] nArray = new int[6];
        int i = 1000, j = 0;
        while (i <= 100000000){
            nArray[j] = i;
            i *= 10;
            j++;
        }
        m.test(seed, nArray, new int[]{20, 100});
    }
}
