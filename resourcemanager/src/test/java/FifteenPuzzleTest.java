// No package declaration since the file is in the default package

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.ulisboa.tecnico.cnv.mss.MSS;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators.FifteenPuzzleEstimator;

public class FifteenPuzzleTest {
    
    private FifteenPuzzleEstimator estimator;
    
    @BeforeEach
    public void setUp() throws Exception {
        estimator = new FifteenPuzzleEstimator();
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

    @Test
    public void testEstimateCostWithExistingData() {

        estimator.mss.insertIntoFifteenPuzzle(4, 5, 20);
        estimator.mss.insertIntoFifteenPuzzle(4, 10, 15);
        estimator.mss.insertIntoFifteenPuzzle(5, 2, 25);
        // Assuming the database has some data, we can test the estimation

        //estimator.mss.readFromFifteenPuzzle(4, 5);
        
        double estimatedCost = estimator.estimateCost(50,70);
        System.out.println("Estimated Cost: " + estimatedCost);
    }
    
   
    
   
}