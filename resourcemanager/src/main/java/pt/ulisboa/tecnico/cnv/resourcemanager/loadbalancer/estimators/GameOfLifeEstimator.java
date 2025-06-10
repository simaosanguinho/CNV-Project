package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameOfLifeEstimator {

    private final int REQUEST_LIMIT = 40; // limit to 40 requests until the model is trained again

    private PolynomialRegression estimationFunction;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private Integer extractSize(String filename) {
        Pattern FILENAME_PATTERN = Pattern.compile("glider-(\\d+)-\\d+\\.json");
        Matcher matcher = FILENAME_PATTERN.matcher(filename);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    public double estimateCost(int iterations, String mapFilename) {
        Optional<Double> estimatedCost = checkDatabase(iterations, mapFilename);
        if (estimatedCost.isPresent()) {
            return estimatedCost.get();
        }

        if (requestCount.get() >= REQUEST_LIMIT) {
            // TODO ->  need te return type from the mss
            getLastRecordsFromDB();
            double[][] inputs = null;
            double[] outputs = null;

            this.lock.writeLock().lock();
            this.estimationFunction = new PolynomialRegression(inputs, outputs);
            this.lock.writeLock().unlock();

            this.requestCount.set(0);
        }

        Integer mapSize = extractSize(mapFilename);
        double[] inputFeatures = new double[] {
                iterations,
                mapSize
        };
        requestCount.incrementAndGet();

        this.lock.readLock().lock();
        double estimation = estimationFunction.estimate(inputFeatures);
        this.lock.readLock().unlock();

        return estimation;
    }

    public Optional<Double> checkDatabase(int iterations, String mapFilename) {
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
