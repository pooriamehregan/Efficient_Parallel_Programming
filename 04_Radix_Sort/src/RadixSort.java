
class RadixSort {

  // The number of bits used to represent a single digit
  int useBits;
  int[] a, b, digitFrequencies, digitPointers;


  RadixSort(int useBits) {
    this.useBits = useBits;
  }

  // Counting sort. A stable sorting algorithm.
  private void countingSort(int mask, int shift) {
    // STEP B : Count the number of occurrences of each digit in a specific position.
    digitFrequencies = new int[mask + 1];
    for (int num : a)
      digitFrequencies[(num >> shift) & mask]++;

    // STEP C : Find the start position of each digit in array B.
    digitPointers = new int[mask + 1];
    for (int i = 0; i < digitFrequencies.length - 1; i++)
      digitPointers[i + 1] = digitPointers[i] + digitFrequencies[i];

    // STEP D : Place the numbers in array A, in the correct places of array B
    for (int num : a)
      b[digitPointers[(num >> shift) & mask]++] = num;
  }

  // Radix sort. Uses counting sort for each position.
  int[] radixSort(int[] unsortedArray) {

    a = unsortedArray;
    b = new int[a.length];

    // STEP A : Find the maximum value.
    int max = a[0];

    for (int num : a)
      if (num > max)
        max = num;


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

    // Performing the counting sort on each position
    for (int i = 0; i < numOfPositions; i++) {
      countingSort(mask, shift);
      shift += useBits;

      // Setting array a to be the array to be sorted again
      int[] temp = a;
      a = b;
      b = temp;
    }

    return a;
  }
}
