package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import pt.ulisboa.tecnico.cnv.mss.MSS;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers.RegressionCTFParser;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers.RegressionDataParser;

public class CaptureTheFlagEstimator {

  private final AtomicInteger requestCount = new AtomicInteger(0);
  private final int REQUEST_LIMIT = 100; // limit to 40 requests until the model is trained again
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  public MSS mss = new MSS();
  private final RegressionCTFParser parser = new RegressionCTFParser();
  private PolynomialRegression estimationFunction = new PolynomialRegression(parser);

  public double estimateCost(
      int gridSize, int numBlueAgents, int numRedAgents, char flagPlacementType) {
    Optional<Double> estimatedCost =
        checkDatabase(gridSize, numBlueAgents, numRedAgents, flagPlacementType);
    if (estimatedCost.isPresent()) {
      System.out.println("Estimated cost from database: " + estimatedCost.get());
      return estimatedCost.get();
    }

    if (requestCount.get() >= REQUEST_LIMIT) {
      Optional<RegressionDataParser.Points> records = getLastRecordsFromDB();

      if (records.isEmpty()) {
        System.out.println("No records found in the database to train the model.");
        return -1; // TODO throw an exception
      }
      requestCount.set(0);
      // TODO print records
      lock.writeLock().lock();
      this.estimationFunction =
          new PolynomialRegression(records.get().getInputs(), records.get().getOutputs());
      lock.writeLock().unlock();

    }

    double[] inputFeatures =
        new double[] {
          gridSize, numBlueAgents, numRedAgents, parser.resolveFlagPlacementType(flagPlacementType)
        };
    requestCount.incrementAndGet();

    lock.readLock().lock();
    double estimation = estimationFunction.estimate(inputFeatures);
    lock.readLock().unlock();

    return estimation;
  }

  public Optional<Double> checkDatabase(
      int gridSize, int numBlueAgents, int numRedAgents, char flagPlacementType) {
    // Check if the request is already in the database
    // If it is, return the estimated cost
    Map<String, AttributeValue> read =
        mss.readFromCaptureTheFlag(
            gridSize, numBlueAgents, numRedAgents, String.valueOf(flagPlacementType));
    if (read != null && read.containsKey("Cost")) {
      double cost = Double.parseDouble(read.get("Cost").getN());
      return Optional.of(cost);
    }

    return Optional.empty();
  }

  public Optional<RegressionDataParser.Points> getLastRecordsFromDB() {
    List<Map<String, AttributeValue>> lastRecords = mss.getLastXFromCaptureTheFlag(REQUEST_LIMIT);
    return parser.parseDB(lastRecords);
  }
}
