import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class ParallelConvexHull {
    private final int n;
    private int nrOfThrds;
    private final int nrOfElmnts;
    private final int[] x;
    private final int[] y;
    private IntList[] intLists;
    private final IntList points;
    private final CyclicBarrier cb;

    /**
     * Constructor
     * @param n number of points
     * @param k number of threads
     * @param x x-coordinates
     * @param y y-coordinates
     * @param points is an Intlist of points
     */
    public ParallelConvexHull(int n, int k, int[] x, int[] y, IntList points){
        this.n = n;
        this.x = x;
        this.y = y;
        this.points = points;

        // if given k is 0 then use the number of available processors or use customized k based on n
        if (k == 0)
            nrOfThrds = Runtime.getRuntime().availableProcessors();
        else
            nrOfThrds = k;

        nrOfElmnts = n / nrOfThrds;
        cb = new CyclicBarrier(nrOfThrds + 1);
    }


    /**
     * Creates and starts threads. Then creates a convex hull from the resulted points.
     * @return a convex hull. It's quickHull() should be called to get big hull.
     */
    public ConvexHull work(){
        intLists = new IntList[nrOfThrds];

        int i;
        for (i = 0; i < nrOfThrds - 1; i++) {
            new Thread(new Worker(i, i * nrOfElmnts, (i+1) * nrOfElmnts)).start();
        }
        new Thread(new Worker(i, (nrOfThrds - 1) * nrOfElmnts, points.size())).start();

        try {
            cb.await();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        int size = 0;
        for (IntList list : intLists){
            size += list.size();
        }
        // calculating the size of the IntList to avoid creating larger and larger list each time it becomes full
        IntList finalIntList = new IntList(size);
        int index = 0;
        int v = 0;
        // Gather the final points which should be used to find the big hull
        for (IntList intList : intLists) {
//            System.out.print("id: " + v++);
//            intList.print();
            for (int l = 0; l < intList.size(); l++) {
                finalIntList.add(intList.get(l), index++);
            }
        }

        // You should call the quickHull() on the returned convexHull to get the big hull.
        return new ConvexHull(n, x, y, finalIntList);
    }


    private class Worker implements Runnable {
        int MAX_X, MIN_X;
        int end, start;
        int id;
        IntList points;

        Worker(int id, int start, int end){
            this.id = id;
            this.start = start;
            this.end = end;
            points = new IntList();
        }


        @Override
        public void run() {
            intLists[id] = quickHull();

            try {
                cb.await();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        IntList quickHull() {
            /* Initialize Max and Min to avoid them being initialized by their default value which is 0. */
            MIN_X = start;
            MAX_X = start;

            /* Find any two points we know are on the line. Here we choose the points
            with the maximum and minimum x coordinates */
            for (int i = start; i < end; i++) {
                if (x[i] > x[MAX_X])
                    MAX_X = i;
                else if (x[i] < x[MIN_X])
                    MIN_X = i;

                points.add(i);
            }
            /* Create our list in which we store the points in the convex hull */
            IntList convexHull = new IntList();

            /*
            Here we start our recursive steps.
              1. First we add the point with the largest x coordinate to the convex hull
              2. Then we find all the points to the left of the line MIN_X -> MAX_X
              3. Then we add the point with the smallest x coordinate to the convex hull
              4. Lastly, we find all the points to the left of the line MAX_X -> MIN_X
            */
            convexHull.add(MAX_X);
            findPointsToLeft(MIN_X, MAX_X, points, convexHull);
            convexHull.add(MIN_X);
            findPointsToLeft(MAX_X, MIN_X, points, convexHull);

            return convexHull;
        }


        /*
        This method does two things:
          1. Finds all the points to the left of the line point1 --> point2 and
             stores them in 'pointsToLeft'. This 'pointsToLeft' is then sent in
             as points to the next recursive call. This is done to decrease the
             number of points we have to look through.

          2. Finds the point 'maxPoint' furthest to the left of the line
             point1 --> point2. This 'maxPoint' is part of the convex hull.
        */
        void findPointsToLeft(int point1, int point2, IntList points, IntList convexHull) {
            int a = y[point1] - y[point2];
            int b = x[point2] - x[point1];
            int c = (y[point2] * x[point1]) - (y[point1] * x[point2]);

            int maxDistance = 0;
            int maxPoint = -1;

            /* Use to store all the points with a positive distance */
            IntList pointsToLeft = new IntList();

            int p, d;
            for (int i = 0; i < points.size(); i++) {
                /* Getting the index of the point */
                p = points.get(i);

                /* Calculating the 'distance' to the line point1 --> point2.
              The actual distance is (ax + by + c ) / squareroot(a^2 + b^2).
              However, the denominator of the fraction only scales down the distance,
              and since we are only interested in the distance relative to the other
              points, we can exclude that calculation. */
                d = a * x[p] + b * y[p] + c;

                if (d >= 0) {
                    pointsToLeft.add(p);

                    if (d > maxDistance) {
                        maxDistance = d;
                        maxPoint = p;
                    }
                }
            } // end for loop

            if (pointsToLeft.size() == 0) {
                List<Integer> onLinePoints = new LinkedList<>();

                for (int i = 0; i < points.size(); i++) {
                    p = points.get(i);
                    d = a * x[p] + b * y[p] + c;

                    /* if this point is on the outer line and the point is not the start or end point
                     * then add it to the convex hull */
                    if ( d == 0 && p != point1 && p != point2)
                        onLinePoints.add(p);
                }

                while (!onLinePoints.isEmpty()) {
                    if (onLinePoints.size() == 1)
                        convexHull.add(onLinePoints.remove(0));
                    else
                        convexHull.add(getMin(onLinePoints, point2));
                }
            }

            /* Only continuing the recursion if we find a point to the left of the line */
            if (maxPoint >= 0) {
                findPointsToLeft(maxPoint, point2, pointsToLeft, convexHull);
                convexHull.add(maxPoint);
                findPointsToLeft(point1, maxPoint, pointsToLeft, convexHull);
            }
        }


        private int getMin(List<Integer> onLinePoints, int point2) {
            double d1, d2;
            int p;
            int xPow2, yPow2;
            p = onLinePoints.get(0);
            xPow2 = (x[point2] - x[p]) * (x[point2] - x[p]);
            yPow2 = (y[point2] - y[p]) * (y[point2] - y[p]);
            d1 = xPow2 + yPow2;

            int ind = 0;
            for (int i = 1; i < onLinePoints.size(); i++) {
                p = onLinePoints.get(i);
                xPow2 = (x[point2] - x[p]) * (x[point2] - x[p]);
                yPow2 = (y[point2] - y[p]) * (y[point2] - y[p]);
                d2 =  xPow2 + yPow2;
                if (d2 < d1) {
                    d1 = d2;
                    ind = i;
                }
            }

            return onLinePoints.remove(ind);
        }
    }

}
