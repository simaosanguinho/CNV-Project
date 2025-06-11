package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;

public class CTFTest {

  private CaptureTheFlagEstimator estimator;

  @BeforeEach
  public void setUp() throws Exception {
    estimator = new CaptureTheFlagEstimator();
  }

  /*     @Test
  public void testCheckDatabaseReturnsCost() {
      // Set up test data in our test MSS
      Map<String, AttributeValue> fakeResponse = new HashMap<>();


      Optional<Double> result = estimator.checkDatabase(10, 2, 8, 'E');
      System.out.println("Result: " + result);

      assertTrue(result.isPresent());
      assertEquals(18.0, result.get());
  } */

  /*     @Test
  public void testCheckDatabaseReturnsEmpty() {
      // Don't set up any response, so it should return empty
      Optional<Double> result = estimator.checkDatabase(10, 3, 4, 'A');

      assertFalse(result.isPresent());
  } */

  /*     @Test
  public void testEstimateCostWithExistingData() {
      // Assuming the database has some data, we can test the estimation

      double estimatedCost = estimator.estimateCost(10, 5, 4, 'A');

      assertTrue(estimatedCost > 0);
  } */

}
