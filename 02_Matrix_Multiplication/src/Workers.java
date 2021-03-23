import java.util.concurrent.CyclicBarrier;

public class Workers {
    private final double[][] a, b, c;
    private double[][] _a, _b;         // Transposed version of a and b
    private final int nrThreads;
    private final int nrOfComponents;
    private final CyclicBarrier cb;


    public Workers(double[][] a, double[][] b){
        this.a = a;
        this.b = b;
        this.c = new double[a.length][a.length];
        int cores = Runtime.getRuntime().availableProcessors();
        // Decide number of threads in a way that it is never assigned to zero, for instance when nr of cores < array length.
        if (cores > a.length) {
            nrOfComponents = 1;
            if (a.length > 2) nrThreads = a.length;         // if array len > 2, then give each thread a row to handle
            else nrThreads = 1;                             // else let only one thread take care of the 2 x 2 array
        }
        else {                                              // nrOfCores < a.length
            nrOfComponents = a.length / cores;
            nrThreads = cores;
        }
        cb = new CyclicBarrier(nrThreads + 1);
    }



    private class MatrixMultiplier implements Runnable{
        double[][] d;
        int sRow, eRow, id, choice;
        MatrixMultiplier(int startRow, int endRow, int id, int choice){
            d = new double[endRow - startRow + 1][a.length];
            sRow = startRow;
            eRow = endRow;
            this.id = id;
            this.choice = choice;
        }
        @Override
        public void run() {
            if (choice == 0) runClassicAlgorithm();
            else if (choice == 1) runTransposedA();
            else if (choice == 2) runTransposedB();

            writeToGlobalArray();

            try {cb.await();}
            catch (Exception exception) {exception.printStackTrace();}
        }

        private void runClassicAlgorithm(){
            double sum;
            // multiply each row of a, with all rows in rotated b
            for (int i = sRow; i < eRow; i++){
                for (int j = 0;j < a.length; j++) {
                    sum = 0;
                    for (int k = 0; k < a.length; k++){
                        sum += a[i][k] * b[k][j];
                    }
                    d[i-sRow][j] = sum;
                }
            }
        }

        private void runTransposedA(){
            double sum;
            // multiply each row of a, with all rows in rotated b
            for (int i = sRow; i < eRow; i++){
                for (int j = 0;j < a.length; j++) {
                    sum = 0;
                    for (int k = 0; k < a.length; k++){
                        sum += _a[k][i] * b[k][j];
                    }
                    d[i-sRow][j] = sum;
                }
            }
        }

        private void runTransposedB(){
            double sum;
            // multiply each row of a, with all rows in rotated b
            for (int i = sRow; i < eRow; i++){
                for (int j = 0;j < a.length; j++) {
                    sum = 0;
                    for (int k = 0; k < a.length; k++){
                        sum += a[i][k] * _b[j][k];
                    }
                    d[i-sRow][j] = sum;
                }
            }
        }

        private void writeToGlobalArray(){
            for (int i = sRow; i < eRow; i++){
                System.arraycopy(d[i - sRow], 0, c[i], 0, a.length);
            }
        }
    }



    /**
     * Creates and runs appropriate worker based on param choice.
     * @param choice 1 for A transposed, and 2 for B transposed
     * @return multiplication result, 2D matrix
     */
    public double[][] runWorker(int choice){
        int s, e, id = 0, workerChoice = 0;   // 0 == classic algorithm, no transposing
        if (choice == 1) {
            _a = transpose(a);
            workerChoice = 1;
        }
        else if (choice == 2) {
            _b = transpose(b);
            workerChoice = 2;
        }

        for (int i = 0; i < nrThreads - 1; i++){
            s = i * nrOfComponents;
            e = (i+1) * nrOfComponents;
            new Thread(new MatrixMultiplier(s, e, ++id, workerChoice)).start();
        }
        s = (nrThreads - 1) * nrOfComponents;
        e = a.length;
        new Thread(new MatrixMultiplier(s, e, ++id, workerChoice)).start();

        try { cb.await(); }
        catch (Exception exception) { exception.printStackTrace(); }
        return c;
    }


    /**
     * rotate the A & B matrix, to _A and _B
     * Flip rotate the matrix, where lower left corner is flipped with upper right.
     * In other words, every value is swapped unless the values in (upper lef to lower right) diagonal.
     * Assumes all rows have same length.
     * @param matrix 2d array
     * @return rotated matrix
     */
    private double[][] transpose(double[][] matrix){
        int size = matrix.length;
        double[][] rotatedMatrix = new double[size][size];

        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                rotatedMatrix[i][j] = matrix[j][i];
            }
        }
        return rotatedMatrix;
    }

}