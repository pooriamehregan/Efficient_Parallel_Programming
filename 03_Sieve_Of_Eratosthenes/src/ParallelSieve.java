import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collector;

public class ParallelSieve {
    /**
     * Declaring all the global variables
     *
     */
    private final int n;
    private final int k;
    private int nrOfThreads;
    private int[] primes;
    private byte[] odds;
    //private ArrayList<Long> allPrimes;
    private int[][] allPrimes;
    private int[] totalPrimes;
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private CyclicBarrier cb;

    private int startIndex;
    private int endIndex;
    private int bytesPerThread;


    /**
     * Constructor that initializes the global variables
     * @param n Prime numbers up until (and including if prime) 'n' is found
     */
    public ParallelSieve(int n, int k) {
        this.n = n;
        this.k = k;
    }


    private void init(){
        odds = new byte[n/16 + 1];

        // deciding nr of threads
        if (k < 1) nrOfThreads = Runtime.getRuntime().availableProcessors();
        else nrOfThreads = k;
        if (nrOfThreads > odds.length){
            bytesPerThread = 1;
            nrOfThreads = odds.length;
        }
        else bytesPerThread = odds.length / nrOfThreads;

        allPrimes = new int[nrOfThreads+1][];

        // Produce primes < sqrt(N), the small table
        SieveOfEratosthenes soe = new SieveOfEratosthenes((int) Math.sqrt(n));
        primes = soe.getPrimes();
        allPrimes[0] = primes;

        int biggestPrime = primes[primes.length - 1];
        startIndex = getIndex(biggestPrime) + 1;
        endIndex = getIndex(n);

        cb = new CyclicBarrier(nrOfThreads + 1);
    }


    private class Worker implements Runnable{
        int start, end, id;
        int localNumOfPrimes = 0;
        int primeIndex = 0;

        Worker(int id, int start, int end){
            this.start = start;
            this.end = end;
            this.id = id;
        }

        @Override
        public void run() {
            sieve();
            collectPrimes();

            try { cb.await(); }
            catch (Exception exception){exception.printStackTrace();}
        }


        /**
         * Performs the Sieve Of Eratosthenes
         */
        private void sieve() {
            int nextIndex;
            int prime = nextPrime(primes[1]); // ignore first prime which is 2

            while (prime != -1) {
                nextIndex = getStart(start, prime); // start index
                if (nextIndex != -1) traverse(nextIndex, prime);
                prime = nextPrime(prime);
            }
        }


        /**
         * finds the index of number x that we should start to jump from: x += p * 2
         * @param index is the start index for this thread
         * @param prime is the current prime
         * @return index that we should start traversing from
         */
        private int getStart(int index, int prime){
            int pSquared = prime * prime;
            int currentNr = getNum(index);

            if (currentNr >= pSquared)
                while ((index < end) && (getNum(index) % prime != 0)) index++;
            else {
                if (pSquared > 2 * end) index = - 1;
                else index = getIndex(prime * prime);
            }
            return index;
        }


        int getNum(int bitIndex){
            return (bitIndex * 2 + 1);
        }


        /**
         * Marks the number 'num' as a composite number (non-prime)
         * @param num The number to be marked non-prime.
         */
        private void mark(int num) {
            int bitIndex = (num % 16) / 2;
            int byteIndex = num / 16;
            odds[byteIndex] |= (1 << bitIndex);
        }


        /**
         * Finds the next prime in the sequence. If there are no more left, it
         * simply returns -1.
         * @param  prev The last prime that has been used to mark all non-primes.
         * @return      The next prime or -1 if there are no more primes.
         */
        private int nextPrime(int prev) {
            if (prev == primes[primes.length-1]) return -1;
            return primes[++primeIndex]; // increment prime index (skipping first prime which is 2), and return next prime
        }


        /**
         * Marks all odd number multiples of 'prime', starting from prime * prime.
         * @param prime The prime used to mark the composite numbers.
         */
        private void traverse(int start, int prime) {
            for (int i = getNum(start); i <= end * 2 && i <= n ; i += prime * 2){
                mark(i);
            }
        }


        /**
         * Iterates through the array to count the number of primes found,
         * creates an array of that size and populates the new array with the primes.
         * @return An array containing all the primes up to and including 'n'.
         */
        private void collectPrimes() {
            for (int i = getNum(start); i <= n && i <= end * 2; i += 2)
                if (isPrime(i))
                    localNumOfPrimes++;

            int[] primes = new int[localNumOfPrimes];
            int j = 0;

            for (int i = getNum(start); i <= n && i <= end * 2; i += 2)
                if (isPrime(i)) primes[j++] = i;

            allPrimes[id] = primes;
        }

        /**
         * Checks if a number is a prime number. If 'num' is prime, it returns true.
         * If 'num' is composite, it returns false.
         * @param  num The number to check.
         * @return     A boolean; true if prime, false if not.
         */
        private boolean isPrime(int num) {
            int bitIndex = (num % 16) / 2;
            int byteIndex = num / 16;
            return (odds[byteIndex] & (1 << bitIndex)) == 0;
        }

        /**
         * returns first index after biggest prime
         * @param num
         * @return
         */
        private int getIndex(int num) {
            return ((num/16) * 8) + ((num % 16) / 2);
        }

    }

    /**
     * This is the public method which is called by other classes to start the Parallel Sieve.
     * @return ArrayList<Long> which contains all founded primes.
     */
    public int[] work(){
        int s, e; // start index and end index
        init();

        int i;
        for (i = 0; i < nrOfThreads - 1; i++){
            if (i == 0) s = startIndex;
            else s = (i * bytesPerThread * 8) - 1;
            e = ((i+1) * bytesPerThread * 8) - 1;
            new Thread(new Worker(i+1, s, e)).start();
        }
        s = ((nrOfThreads - 1) * bytesPerThread * 8) - 1;
        e = endIndex;
        new Thread(new Worker(i+1, s, e)).start();

        try { cb.await();}
        catch (Exception exception){exception.printStackTrace();}


        int len = 0;
        for (i = 0; i < allPrimes.length; i++){
            len += allPrimes[i].length;
        }

        /* Converting the 2-D array allPrimes to 1-D array totalPrimes*/
        totalPrimes = new int[len];
        len = 0;
        for (i = 0; i < nrOfThreads - 1; i++){
            new Thread(new Collector(i, 1, len)).start();
            len += allPrimes[i].length;
        }
        new Thread(new Collector(i, 2, len)).start();

        try { cb.await(); }
        catch (Exception exception){exception.printStackTrace();}

        return totalPrimes;
    }

    /**
     * returns first index after biggest prime
     * @param num
     * @return
     */
    private int getIndex(int num) {
        return ((num/16) * 8) + ((num % 16) / 2);
    }


    private class Collector implements Runnable {
        int row; int nrOfRows; int start;

        Collector(int row, int nrOfRows, int start){
            this.row = row; this.nrOfRows = nrOfRows; this.start = start;
        }

        @Override
        public void run() {
            for (int i = 0; i < nrOfRows; i++){
                row += i;
                for (int j = 0; j < allPrimes[row].length; j++){
                    totalPrimes[start++] = allPrimes[row][j];
                }
            }

            try { cb.await(); }
            catch (Exception exception){exception.printStackTrace();}
        }
    }



}


