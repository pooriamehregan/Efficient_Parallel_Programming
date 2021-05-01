public class ParallelRadixSort {
    // The number of bits used to represent a single digit
    int useBits;
    int[] a, b;


    ParallelRadixSort(int useBits) {
        this.useBits = useBits;
    }

    // Radix sort. Uses counting sort for each position.
    void radixSort(int[] unsortedArray) {
        a = unsortedArray;
        b = new int[a.length];

        FindMax fm = new FindMax(a);
        int max = fm.findMax();
        //int max = a[0];

        // Substep: Finding number of bits that is needed to represent max value
        int numBitsMax = 1;
        while (max >= (1L << numBitsMax))
            numBitsMax++;

        // Substep: Finding the number of positions needed to represent the max value
        int numOfPositions = numBitsMax / useBits;
        if (numBitsMax % useBits != 0) numOfPositions++;

        // Substep: If useBits is larger than numBitsMax,
        // set useBits equal to numBitsMax to save space.
        if (numBitsMax < useBits) useBits = numBitsMax;

        // Substep: Creating the mask and initialising the shift variable,
        // both of whom are used to extract the digits.
        int mask = (1 << useBits) - 1;
        int shift = 0;

        CountingSort cs =  new CountingSort();

        // Performing the counting sort on each position
        for (int i = 0; i <= numOfPositions; i++) {
            cs.countSort(a, b, mask, shift);
            shift += useBits;
            // Setting array a to be the array to be sorted again
            int[] temp = a;
            a = b;
            b = temp;
        }
    }
}
