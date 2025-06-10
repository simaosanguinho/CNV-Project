package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.xspec.M;

import pt.ulisboa.tecnico.cnv.mss.MSS;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers.RegressionDataParser;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers.RegressionPuzzleParser;

public class FifteenPuzzleEstimator {

    private final int REQUEST_LIMIT = 3; // limit to 40 requests until the model is trained again
    public MSS mss = new MSS();
    private final AtomicInteger requestCount = new AtomicInteger(3);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final RegressionPuzzleParser parser = new RegressionPuzzleParser();
    private PolynomialRegression estimationFunction = new PolynomialRegression(parser);

    public double estimateCost(int size, int shuffles) {
        Optional<Double> estimatedCost = checkDatabase(size, shuffles);
        if (estimatedCost.isPresent()) {
            return estimatedCost.get();
        }

        if (requestCount.get() >= REQUEST_LIMIT) {
            Optional<RegressionDataParser.Points> records = getLastRecordsFromDB();

            if (records.isEmpty()) {
                System.out.println("No records found in the database to train the model.");
                return -1; // TODO -> throw an exception
            }

            // TODO -> print records

            // print records content
            lock.writeLock().lock();
            this.estimationFunction = new PolynomialRegression(records.get().getInputs(), records.get().getOutputs());
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

    public Optional<RegressionDataParser.Points> getLastRecordsFromDB() {
       List<Map<String, AttributeValue>> lastRecords = mss.getLastXFromFifteenPuzzle(REQUEST_LIMIT);
       return parser.parseDB(lastRecords);
    }
}
