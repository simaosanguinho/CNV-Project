package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.xspec.M;

import pt.ulisboa.tecnico.cnv.mss.MSS;

public class FifteenPuzzleEstimator {

    private final int REQUEST_LIMIT = 3; // limit to 40 requests until the model is trained again
    public MSS mss = new MSS();
    private PolynomialRegression estimationFunction;
    private final AtomicInteger requestCount = new AtomicInteger(3);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public double estimateCost(int size, int shuffles) {
        Optional<Double> estimatedCost = checkDatabase(size, shuffles);
        if (estimatedCost.isPresent()) {
            return estimatedCost.get();
        }

        if (requestCount.get() >= REQUEST_LIMIT) {
            // TODO ->  need te return type from the mss
            double[][] records = getLastRecordsFromDB();
            if (records == null || records.length == 0) {
                System.out.println("No records found in the database to train the model.");
                return -1; // or throw an exception
            }
            double[][] inputs = new double[records.length][2];
            double[] outputs = new double[records.length];
            for (int i = 0; i < records.length; i++) {
                inputs[i][0] = records[i][0];
                inputs[i][1] = records[i][1];
                outputs[i] = records[i][2];
            }

            System.out.println("Training model with the following records:");
            for (double[] record : records) {
                System.out.printf("Size: %.2f, Shuffles: %.2f, Cost: %.2f%n",
                        record[0], record[1], record[2]);
            }

            // print records content
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
        Map<String, AttributeValue> item = mss.readFromFifteenPuzzle(size, shuffles);
        if (item != null && item.containsKey("Cost")) {
            double cost = Double.parseDouble(item.get("Cost").getN());
            return Optional.of(cost);
        }
        return Optional.empty();
    }

    public double[][] getLastRecordsFromDB() {
       List<Map<String, AttributeValue>> lastRecords = mss.getLastXFromFifteenPuzzle(REQUEST_LIMIT);
       if (lastRecords == null || lastRecords.isEmpty()) {
           System.out.println("No records found in the database to train the model.");
           return null;
       }

       return lastRecords.stream()
               .map(item -> new double[] {
                   Double.parseDouble(item.get("Size").getN()),
                   Double.parseDouble(item.get("Shuffles").getN()),
                   Double.parseDouble(item.get("Cost").getN()),
               })
               .toArray(double[][]::new);
        
    }
}
