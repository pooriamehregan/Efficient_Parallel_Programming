import java.util.concurrent.CyclicBarrier;

public class CountingSort {
    private int nrOfThreads, mask, shift, nrOfElementsPrThread;
    private int[] a, b;
    private int[][] pointers, frequencies;
    private CyclicBarrier cb;

    public void countSort(int[] a, int[] b, int mask, int shift){
        init(a, b, mask, shift);
        count();
        transport();
    }

    private void init(int[] a, int[] b, int mask, int shift){
        this.a = a; this.b = b;
        this.mask = mask; this.shift = shift;


        nrOfThreads = Runtime.getRuntime().availableProcessors();
        if (nrOfThreads > a.length) nrOfThreads = a.length;

        /* optimization for nr of thread on my machine with 12 cores,
         *  not sure if it works on other machines */
        //else if (a.length < 10000000) nrOfThreads /= 2;

        pointers = new int[nrOfThreads][mask + 1];
        frequencies = new int[nrOfThreads][mask + 1];
        nrOfElementsPrThread = a.length / nrOfThreads;

        cb = new CyclicBarrier(nrOfThreads + 1);
    }


    private void count(){
        int i = 0;
        for (; i < nrOfThreads - 1; i++){
            new Thread(new Counter(i, i * nrOfElementsPrThread, (i+1) * nrOfElementsPrThread)).start();
        }
        new Thread(new Counter(i, (nrOfThreads-1) * nrOfElementsPrThread, a.length)).start();

        try { cb.await(); }
        catch (Exception exception) {exception.printStackTrace();}

        for (i = 0; i < pointers[0].length; i++){
            if (i != 0) pointers[0][i] = pointers[pointers.length-1][i-1] + frequencies[frequencies.length-1][i-1];
            for (int j = 0; j < pointers.length-1; j++){
                pointers[j + 1][i] = pointers[j][i] + frequencies[j][i];
            }
        }

    }


    private void transport(){
        int i;
        for (i = 0; i < nrOfThreads - 1; i++){
            new Thread(new Transporter(i, i * nrOfElementsPrThread, (i+1) * nrOfElementsPrThread)).start();
        }
        new Thread(new Transporter(i,(nrOfThreads-1) * nrOfElementsPrThread, a.length)).start();

        try { cb.await(); }
        catch (Exception exception) {exception.printStackTrace();}
    }


    private class Counter implements Runnable {
        int start, end, id;
        int[] localDigitFrequencies;
        Counter(int id, int start, int end){
            this.start = start; this.end = end; this.id = id;
            localDigitFrequencies = new int[mask + 1];
        }

        @Override
        public void run() {
            for (int i = start; i < end; i++)
                localDigitFrequencies[( a[i] >> shift) & mask]++;
            frequencies[id] = localDigitFrequencies;

            try { cb.await(); }
            catch (Exception exception) {exception.printStackTrace();}
        }
    }


    private class Transporter implements Runnable {
        int id, start, end;
        int[] localDigitPointers;
        Transporter(int id, int start, int end){
            this.id = id; this.start = start; this.end = end;
            localDigitPointers = pointers[id];
        }

        @Override
        public void run() {
            int num;
            for (int i = start; i < end; i++){
                num = a[i];
                b[localDigitPointers[(num >> shift) & mask]++] = num;
            }

            try { cb.await(); }
            catch (Exception exception) {exception.printStackTrace();}
        }
    }


                                    /* THIS is the part where part c is also parallelized but not d */
//
//
//    private class Counter implements Runnable {
//        int start, end, id;
//        int[] localDigitFrequencies;
//        int[] localDigitPointers;
//        Counter(int id, int start, int end){
//            this.start = start; this.end = end; this.id = id;
//            localDigitFrequencies = new int[mask + 1];
//            localDigitPointers = new int[mask + 1];
//        }
//
//        @Override
//        public void run() {
//            for (int i = start; i < end; i++) {
//                localDigitFrequencies[( a[i] >> shift) & mask]++;
//            }
//
//            for (int i = 0; i < localDigitFrequencies.length - 1; i++)
//                localDigitPointers[i + 1] = localDigitPointers[i] + localDigitFrequencies[i];
//            pointers[id] = localDigitPointers;
//
//            try { cb.await(); }
//            catch (Exception exception) {exception.printStackTrace();}
//        }
//    }
//
//
//    private class Transporter implements Runnable {
//        int start, end;
//        Transporter(int start, int end){
//            this.start = start; this.end = end;
//        }
//
//        @Override
//        public void run() {
//            int num;
//            for (int i = 0; i < a.length; i++){
//                num = (a[i] >> shift) & mask;
//                if (num >= start && num < end){
//                    b[digitPointers[num]++] = a[i];
//                }
//            }
//
//            try { cb.await(); }
//            catch (Exception exception) {exception.printStackTrace();}
//        }
//    }

}
