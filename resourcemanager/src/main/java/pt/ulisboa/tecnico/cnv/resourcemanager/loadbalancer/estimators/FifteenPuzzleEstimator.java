package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FifteenPuzzleEstimator {

    private final int REQUEST_LIMIT = 40; // limit to 40 requests until the model is trained again

    private PolynomialRegression estimationFunction;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public double estimateCost(int size, int shuffles) {
        Optional<Double> estimatedCost = checkDatabase(size, shuffles);
        if (estimatedCost.isPresent()) {
            return estimatedCost.get();
        }

        if (requestCount.get() >= REQUEST_LIMIT) {
            // TODO ->  need te return type from the mss
            getLastRecordsFromDB();
            double[][] inputs = null;
            double[] outputs = null;

            lock.writeLock().lock();
            this.estimationFunction = new PolynomialRegression(inputs, outputs);
            lock.writeLock().unlock();

            this.requestCount.set(0);
        }

        double[] inputFeatures = new double[] {
            size,
            shuffles
        };
        requestCount.incrementAndGet();

        lock.readLock().lock();
        double estimation = estimationFunction.estimate(inputFeatures);
        lock.readLock().unlock();

        return estimation;
    }

    public Optional<Double> checkDatabase(int size, int shuffles) {
        // Check if the request is already in the database
        // If it is, return the estimated cost
        // If not, add the request to the database and return null
        //TODO
        return Optional.empty();
    }

    public void getLastRecordsFromDB() {
        // TODO -> this will not return void
    }
}
