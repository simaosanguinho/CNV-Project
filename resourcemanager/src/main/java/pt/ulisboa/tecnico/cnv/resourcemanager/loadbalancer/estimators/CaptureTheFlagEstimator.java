package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pt.ulisboa.tecnico.cnv.mss.MSS;

public class CaptureTheFlagEstimator {

    private final int REQUEST_LIMIT = 40; // limit to 40 requests until the model is trained again

    private PolynomialRegression estimationFunction;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public double estimateCost(int gridSize, int numBlueAgents, int numRedAgents, char flagPlacementType) {
        Optional<Double> estimatedCost = checkDatabase(gridSize, numBlueAgents, numRedAgents, flagPlacementType);
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

            requestCount.set(0);
        }

        double[] inputFeatures = new double[] {
            gridSize,
            numBlueAgents,
            numRedAgents,
            flagPlacementType == 'A' ? 1 : (flagPlacementType == 'B' ? 2 : 3)
        };
        requestCount.incrementAndGet();

        lock.readLock().lock();
        double estimation = estimationFunction.estimate(inputFeatures);
        lock.readLock().unlock();

        return estimation;
    }

    public Optional<Double> checkDatabase(int gridSize, int numBlueAgents, int numRedAgents, char flagPlacementType) {
        // Check if the request is already in the database
        // If it is, return the estimated cost
        // If not, add the request to the database and return null
        //TODO
        return Optional.empty();
    }

    public void getLastRecordsFromDB() {
        MSS mss = MSS.getInstance();
    }

}
