package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import pt.ulisboa.tecnico.cnv.mss.MSS;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers.RegressionDataParser;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers.RegressionGOLParser;

public class GameOfLifeEstimator {

  private final int REQUEST_LIMIT = 40; // limit to 40 requests until the model is trained again
  public MSS mss = new MSS();
  private final AtomicInteger requestCount = new AtomicInteger(0);
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final RegressionGOLParser parser = new RegressionGOLParser();
  private PolynomialRegression estimationFunction = new PolynomialRegression(parser);

  public double estimateCost(int iterations, String mapFilename) {
    Optional<Double> estimatedCost = checkDatabase(iterations, mapFilename);
    if (estimatedCost.isPresent()) {
      return estimatedCost.get();
    }

    if (requestCount.get() >= REQUEST_LIMIT) {
      Optional<RegressionDataParser.Points> records = getLastRecordsFromDB();

      if (records.isEmpty()) {
        System.out.println("No records found in the database to train the model.");
        return -1; // TODO -> throw exception
      }

      // TODO print records

      this.lock.writeLock().lock();
      this.estimationFunction =
          new PolynomialRegression(records.get().getInputs(), records.get().getOutputs());
      this.lock.writeLock().unlock();

      this.requestCount.set(0);
    }

    Integer mapSize = parser.extractSize(mapFilename);
    double[] inputFeatures = new double[] {iterations, mapSize};
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

  public Optional<RegressionDataParser.Points> getLastRecordsFromDB() {
    List<Map<String, AttributeValue>> lastRecords = mss.getLastXFromGameOfLife(REQUEST_LIMIT);
    return parser.parseDB(lastRecords);
  }
}
