import java.util.concurrent.CyclicBarrier;

public class FindMax {
    private int nrOfThreads;
    private final int nrOfElements;
    private final int[] a;
    private final int[] localMaxes;
    private final CyclicBarrier cb;
    public FindMax(int[] a){
        this.a = a;
        nrOfThreads = Runtime.getRuntime().availableProcessors();

        if (nrOfThreads > a.length) nrOfThreads = a.length;
        else if (a.length < 30000000) nrOfThreads = 1;
        else if (a.length < 65000000) nrOfThreads = 2;

        nrOfElements = a.length / nrOfThreads;
        localMaxes = new int[nrOfThreads];
        cb = new CyclicBarrier(nrOfThreads + 1);
    }


    public int findMax(){
        int i;
        for (i = 0; i < nrOfThreads - 1; i++){
            new Thread(new Finder(i, i * nrOfElements, (i+1) * nrOfElements)).start();
        }
        new Thread(new Finder(i, (nrOfThreads-1) * nrOfElements, a.length)).start();

        try { cb.await(); }
        catch (Exception exception) {exception.printStackTrace();}

        int max = localMaxes[0];
        for (i = 1; i < localMaxes.length; i++){
            if (localMaxes[i] > max) max = localMaxes[i];
        }
        return max;
    }


    private class Finder implements Runnable {
        int start, end, id;
        public Finder(int id, int start, int end){
            this.start = start; this.end = end; this.id = id;
        }
        @Override
        public void run() {
            int max = a[start++];
            for (int i = start; i < end; i++){
                if (a[i] > max) max = a[i];
            }
            localMaxes[id] = max;

            try { cb.await(); }
            catch (Exception exception) {exception.printStackTrace();}
        }
    }
}
