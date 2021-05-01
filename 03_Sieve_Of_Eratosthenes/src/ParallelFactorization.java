import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class ParallelFactorization {
    private int nrOfThreads;
    private int start, end;
    private final int[] primes;
    private final CyclicBarrier cb;
    ReentrantLock reentrantLock = new ReentrantLock();
    ReentrantLock dealerLock = new ReentrantLock();
    private ArrayList<Long> factors;

    public ParallelFactorization(int n, int k){
        ParallelSieve soe = new ParallelSieve(n, k);
        this.primes = soe.work();

        if (k < 1) nrOfThreads = Runtime.getRuntime().availableProcessors();
        else nrOfThreads = k;
        if (primes.length < nrOfThreads) nrOfThreads = primes.length;

        cb = new CyclicBarrier(nrOfThreads + 1);
    }

    private class Worker implements Runnable{
        private final long n;

        Worker(long n){ this.n = n; }

        @Override
        public void run() {
            ArrayList<Long> localFactors = factorize();
            addToGlobalFactors(localFactors);

            try {cb.await();}
            catch (Exception exception) {exception.printStackTrace();}
        }

        private ArrayList<Long> factorize() {
            // What is being divided is called the dividend, which is divided by the divisor
            long dividend = n;
            long divisor;

            ArrayList<Long> localFactors = new ArrayList<>();

            long[] localPrimes = new long[3];
            dealNextPrimes(localPrimes);

            int i;
            while (localPrimes[0] != -1) {
                divisor = localPrimes[0];
                i = 1;
                while (true) {
                    if ((dividend % divisor) == 0) {          // if dividend is divisible by divisor
                        dividend = dividend / divisor;
                        localFactors.add(divisor);
                    } else {
                        if (i == 1) {
                            if (localPrimes[1] == -1) {
                                // if this is the last prime, add it
                                if (localPrimes[0] == primes[primes.length - 1])
                                    localFactors.add(dividend);
                                break;
                            } else {
                                divisor = localPrimes[1];
                                i = 2;
                            }
                        } else {
                            break;
                        }
                    }
                }
                dealNextPrimes(localPrimes);
            }
            return localFactors;
        }
    }

    /**
     * Adds the founded local factors to the global array list of all factors
     * @param localFactors are the long arraylist of local factors.
     */
    private void addToGlobalFactors(ArrayList<Long> localFactors) {
        reentrantLock.lock();
        try { factors.addAll(localFactors); }
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
    private void dealNextPrimes(long[] localPrimes){
        dealerLock.lock();
        try {
            int remains = end - start;
            localPrimes[2] = remains;

            if (remains == 0) {
                localPrimes[0] = -1;
                localPrimes[1] = -1;
            }
            else if (remains == 1) {
                localPrimes[0] = primes[++start];
                localPrimes[1] = -1;
            }
            else {  // giving each thre[ primes, one from start of the prime array, and one from end
                localPrimes[0] = primes[start++];
                localPrimes[1] = primes[end--];
            }
        }
        catch (Exception e) {e.printStackTrace();}
        finally {dealerLock.unlock();}
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
