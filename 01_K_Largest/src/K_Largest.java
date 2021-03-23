
import java.util.concurrent.CyclicBarrier;

public class K_Largest {
    private final int K;
    private final int numOfThreads;
    private final int[] array;
    private final int numOfElements;
    private final CyclicBarrier cyclicBarrier;
    private int tk;


    public K_Largest(int[] array, int K){
        this.array = array;
        this.K = K;
        int cores = Runtime.getRuntime().availableProcessors();
        int parArrLen = (array.length - K * 2) - ((array.length - K * 2) % cores); // 912

        numOfElements = parArrLen / cores; // 76
        tk = K;
        if (parArrLen < 1) numOfThreads = 0;
        else if (numOfElements < 2) {           // there are < 2 elements for each core
            tk = numOfElements;
            numOfThreads = 1;
        }
        else if (numOfElements < K) { // numOfElement is how many elements each thread handles
            tk = numOfElements;
            numOfThreads = cores;
        }
        else numOfThreads = cores;

        cyclicBarrier = new CyclicBarrier( numOfThreads + 1);
    }

    class Worker implements Runnable{
        int start, end;
        Worker(int start,int end){
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            sort();
            try { cyclicBarrier.await(); }
            catch (Exception e){e.printStackTrace();}
        }

        void sort(){
            int k, j, temp;
            for (int i = start; i < start + tk; i++){            // Sort first K
                if (array[i+1] > array[i]){
                    j = i;
                    while (j > start && array[j+1] > array[j]) {
                        temp = array[j];
                        array[j] = array[j + 1];
                        array[j + 1] = temp;
                        j--;
                    }
                }
            }
            k = start + tk;
            for (int i = k; i < end; i++){                      // Find larger than K - 1 and replace
                if (array[i] > array[k - 1]) {
                    temp = array[i];
                    array[i] = array[k - 1];
                    j = k - 1;
                    while (j > start && array[j - 1] < temp) {
                        array[j] = array[j - 1];
                        j--;
                    }
                    array[j] = temp;
                }
            }

        }
    }


    void init(){
        if (array.length < K) return;         // if array length is <= K then array is already sorted. return
        int start, end;

        for (int i = 0; i < numOfThreads-1; i++){
            start = K + (i * numOfElements);
            end = K + ((i+1) * numOfElements);
            new Thread(new Worker(start, end)).start();
        }
        start = K + (numOfThreads-1) * numOfElements;
        end = array.length;
        new Thread(new Worker(start, end)).start();     // Sort last part

        sortFirstKs(); // While other threads are sorting, main sorts first K

        try { cyclicBarrier.await(); }
        catch (Exception e){ e.printStackTrace(); }

        for (int i = 0; i < numOfThreads; i++){
            start = K + (i * numOfElements);
            reorganize(start);
        }
    }

    private void reorganize(int start) {
        int temp;
        for (int i = start; i < start + tk; i++){
            if (array[i] > array[K-1]){
                temp = array[K-1];
                array[K-1] = array[i];
                array[i] = temp;
                sortFirstKs();
            }
        }
    }
    void sortFirstKs() {
        int j, temp;
        for (int i = 1; i < K; i++){
            if (array[i] > array[i - 1]){
                j = i;
                while (j > 0 && array[j] > array[j-1]) {
                    temp = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = temp;
                    j--;
                }
            }
        }
    }
}
