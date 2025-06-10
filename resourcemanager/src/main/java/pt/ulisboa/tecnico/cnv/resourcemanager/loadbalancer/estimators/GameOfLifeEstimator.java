package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.cnv.mss.MSS;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.xspec.M;
import pt.ulisboa.tecnico.cnv.mss.MSS;

import com.amazonaws.services.dynamodbv2.xspec.M;

public class GameOfLifeEstimator {

    private final int REQUEST_LIMIT = 40; // limit to 40 requests until the model is trained again
    public MSS mss = new MSS();
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
                System.out.printf("MapFilename: %.2f, Iterations: %.2f, Cost: %.2f%n",
                        record[0], record[1], record[2]);
            }
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
    Map<String, AttributeValue> item = mss.readFromGameOfLife(mapFilename, iterations);
    if (item != null && item.containsKey("Cost")) {
        double cost = Double.parseDouble(item.get("Cost").getN());
        return Optional.of(cost);
    }
        return Optional.empty();
    }

    public double[][] getLastRecordsFromDB() {
    List<Map<String, AttributeValue>> lastRecords = mss.getLastXFromGameOfLife(REQUEST_LIMIT);
       if (lastRecords == null || lastRecords.isEmpty()) {
           System.out.println("No records found in the database to train the model.");
           return null;
       }

       return lastRecords.stream()
               .map(item -> new double[] {
                   Double.parseDouble(item.get("MapFilename").getN()),
                   Double.parseDouble(item.get("Iterations").getN()),
                   Double.parseDouble(item.get("Cost").getN()),
               })
               .toArray(double[][]::new);
        
    }
}
