package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import pt.ulisboa.tecnico.cnv.mss.MSS;

public class CaptureTheFlagEstimator {

    private PolynomialRegression estimationFunction;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final int REQUEST_LIMIT = 3; // limit to 40 requests until the model is trained again
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public MSS mss = new MSS();

    public double estimateCost(int gridSize, int numBlueAgents, int numRedAgents, char flagPlacementType) {
        Optional<Double> estimatedCost = checkDatabase(gridSize, numBlueAgents, numRedAgents, flagPlacementType);
        if (estimatedCost.isPresent()) {
            System.out.println("Estimated cost from database: " + estimatedCost.get());
            return estimatedCost.get();
        }

        if (requestCount.get() >= REQUEST_LIMIT) {
            // TODO -> need te return type from the mss
            double[][] records = getLastRecordsFromDB();
            if (records == null || records.length == 0) {
                System.out.println("No records found in the database to train the model.");
                return -1; // or throw an exception
            }
            double[][] inputs = new double[records.length][4];
            double[] outputs = new double[records.length];
            for (int i = 0; i < records.length; i++) {
                inputs[i][0] = records[i][0];
                inputs[i][1] = records[i][1];
                inputs[i][2] = records[i][2];
                inputs[i][3] = records[i][3];
                outputs[i] = records[i][4];
            }

            // print records content
            System.out.println("Training model with the following records:");
            for (double[] record : records) {
                System.out.printf("GridSize: %.2f, NumBlueAgents: %.2f, NumRedAgents: %.2f, FlagPlacementType: %.2f, Cost: %.2f%n",
                        record[0], record[1], record[2], record[3], record[4]);
            }

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
        Map<String, AttributeValue> read = mss.readFromCaptureTheFlag(gridSize, numBlueAgents, numRedAgents,
                String.valueOf(flagPlacementType));
        if (read != null && read.containsKey("Cost")) {
            double cost = Double.parseDouble(read.get("Cost").getN());
            return Optional.of(cost);
        }

        return Optional.empty();
    }

    public double[][] getLastRecordsFromDB() {
        List<Map<String, AttributeValue>> lastRecords = mss.getLastXFromCaptureTheFlag(REQUEST_LIMIT);
        if (lastRecords.isEmpty()) {
            System.out.println("No records found in the database.");
            return null;
        }
        return lastRecords.stream()
                .map(record -> {
                    double gridSize = Double.parseDouble(record.get("GridSize").getN());
                    double numBlueAgents = Double.parseDouble(record.get("NumBlueAgents").getN());
                    double numRedAgents = Double.parseDouble(record.get("NumRedAgents").getN());
                    double flagPlacementType = record.get("FlagPlacementType").getS().charAt(0) == 'A' ? 1
                            : record.get("FlagPlacementType").getS().charAt(0) == 'B' ? 2 : 3;
                    double cost = Double.parseDouble(record.get("Cost").getN());
                    return new double[] { gridSize, numBlueAgents, numRedAgents, flagPlacementType, cost };
                })
                .toArray(double[][]::new);
    }

}
