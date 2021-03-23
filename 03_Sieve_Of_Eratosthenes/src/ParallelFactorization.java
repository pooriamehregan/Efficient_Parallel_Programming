import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class ParallelFactorization {
    private int nrOfThreads;
    private int start, end;
    private final int[] primes;
    private final CyclicBarrier cb;
    ReentrantLock reentrantLock = new ReentrantLock();
    private ArrayList<Long> factors;

    public ParallelFactorization(int n, int k){

        SieveOfEratosthenes soe = new SieveOfEratosthenes((int) Math.sqrt(n));
        this.primes = soe.getPrimes();

//        ParallelSieve ps = new ParallelSieve((int) Math.sqrt(n), k);
//        this.primes = toIntArray(ps.work());

        if (k < 1) nrOfThreads = Runtime.getRuntime().availableProcessors();
        else nrOfThreads = k;
        if (primes.length <= nrOfThreads) nrOfThreads = primes.length;

        cb = new CyclicBarrier(nrOfThreads + 1);
    }

    private int[] toIntArray(ArrayList<Long> a) {
        int[] arr = new int[a.size()];
        for (int i = 0; i < arr.length; i++){
            arr[i] =  a.get(i).intValue();
        }
        return arr;
    }

    private class Worker implements Runnable{
        private final long n;
        Worker(long n){ this.n = n; }

        @Override
        public void run() {
            ArrayList<Long> localFactors = new ArrayList<>();
            int[] localPrimes = new int[2];
            dealNextPrimes(localPrimes);
            while (localPrimes[0] != -1){
                localFactors.addAll(factorize(localPrimes));
                if (localPrimes[1] == -1) break;
                dealNextPrimes(localPrimes);
            }
            addToGloblFactors(localFactors);

            try {cb.await();}
            catch (Exception exception) {exception.printStackTrace();}
        }

        private ArrayList<Long> factorize(int[] localPrimes) {
            // What is being divided is called the dividend, which is divided by the divisor
            long dividend = n;
            long divisor = localPrimes[0];
            ArrayList<Long> localFactors = new ArrayList<>();
            int i = 0;
            while (divisor <= Math.sqrt(n)) {
                if ((dividend % divisor) != 0) {          // if dividend is not divisible by divisor
                    i++;
                    if (i == localPrimes.length) {
                        if (end - 1 == start && dividend > 1) localFactors.add(dividend);    // TODO: maybe check if divisor is the last prime
                        break;
                    }
                    if (localPrimes[i] != -1) divisor = localPrimes[i];
                    continue;
                }
                dividend = dividend / divisor;
                localFactors.add(divisor);
            }
            return localFactors;
        }
    }

    /**
     * Adds the founded local factors to the global array list of all factors
     * @param localFactors are the long arraylist of local factors.
     */
    private void addToGloblFactors(ArrayList<Long> localFactors) {
        reentrantLock.lock();
        try {
            factors.addAll(localFactors);
        }
        catch (Exception e) {e.printStackTrace();}
        finally {reentrantLock.unlock();}
    }

    /**
     * This is the public method which is called by other classes to start the factorization.
     * @param n the long number to be factorized
     * @return unik prime factors which result into n when multiplied together.
     */
    public ArrayList<Long> work(long n){
        start = 0;
        end = primes.length - 1;
        factors = new ArrayList<>();

        for (int i = 0; i < nrOfThreads; i++){
            new Thread(new Worker(n)).start();
        }

        try {cb.await();}
        catch (Exception exception) {exception.printStackTrace();}

        long p = getProducts(factors);
        long f = n / p;
        if (f > 1) factors.add(f);

        return factors;
    }

    /**
     * Deals primes to thread, like a card dealer.
     */
    private void dealNextPrimes(int[] localPrimes){
        reentrantLock.lock();
        try {
            if (end == start) {
                localPrimes[0] = -1;
                localPrimes[1] = -1;
            }
            else if (end - start == 1) {
                localPrimes[0] = primes[++start];
                localPrimes[1] = -1;
            }
            else {  // giving each thread 2 primes, one from start of the prime array, and one from end
                localPrimes[0] = primes[start++];
                localPrimes[1] = primes[end--];
            }
        }
        catch (Exception e) {e.printStackTrace();}
        finally {reentrantLock.unlock();}
    }

    /**
     * Multiplies founded factors with each other and returns the result
     * @param localFactors Long values to be multiplied
     * @return result of multiplication
     */
    private long getProducts(ArrayList<Long> localFactors) {
        if (localFactors.isEmpty()) return 1;

        long result = localFactors.get(0);
        for (int i = 1; i < localFactors.size(); i++) {
            result *= localFactors.get(i);
        }
        return result;
    }

}
