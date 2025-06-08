import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

/**
 * This sample demonstrates how to perform a few simple operations with the
 * Amazon DynamoDB service.
 */
public class MSS {

    // Get from environment variable AWS_REGION
    private static String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");

    private static AmazonDynamoDB dynamoDB;

    public static void main(String[] args) throws Exception {
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        try {
            String tableName = "CaptureTheFlag";


            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            System.out.println("Table Description: " + tableDescription);

            /* // Add an item
            dynamoDB.putItem(new PutItemRequest(tableName, newItem("Bill & Ted's Excellent Adventure", 1989, "****", "James", "Sara")));

            // Add another item
            dynamoDB.putItem(new PutItemRequest(tableName, newItem("Airplane", 1980, "*****", "James", "Billy Bob")));

            // Scan items for movies with a year attribute greater than 1985
            HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
            Condition condition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withN("1985"));
            scanFilter.put("year", condition);
            ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
            ScanResult scanResult = dynamoDB.scan(scanRequest);
            System.out.println("Result: " + scanResult); */
            // Insert into CaptureTheFlag
            insertIntoCaptureTheFlag(10, 5, 5, "A", 20);

            readFromCaptureTheFlag(10, 5, 5, "A");

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    /* WRITE */
    private static void insertIntoCaptureTheFlag(int gridSize, int numBlue, int numRed, String flagType, int Cost) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("GridSize", new AttributeValue().withN(String.valueOf(gridSize)));
        item.put("CompositeKey", new AttributeValue("Blue=" + numBlue + "_Red=" + numRed + "_Flag=" + flagType));
        item.put("NumBlueAgents", new AttributeValue().withN(String.valueOf(numBlue)));
        item.put("NumRedAgents", new AttributeValue().withN(String.valueOf(numRed)));
        item.put("FlagPlacementType", new AttributeValue(flagType));
        item.put("CreatedAt", new AttributeValue().withS(Instant.now().toString()));
        item.put("Cost", new AttributeValue().withN(String.valueOf(Cost)));

        dynamoDB.putItem(new PutItemRequest("CaptureTheFlag", item));
        System.out.println("Inserted item into CaptureTheFlag");
    }

    private static void insertIntoFifteenPuzzle(int size, int shuffles, int cost) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("Size", new AttributeValue().withN(String.valueOf(size)));
        item.put("Shuffles", new AttributeValue().withN(String.valueOf(shuffles)));
        item.put("CreatedAt", new AttributeValue().withS(Instant.now().toString()));
        item.put("Cost", new AttributeValue().withN(String.valueOf(cost)));

        dynamoDB.putItem(new PutItemRequest("FifteenPuzzle", item));
        System.out.println("Inserted item into FifteenPuzzle");
    }

    private static void insertIntoGameOfLife(String mapFilename, int iterations, String checksum, int cost) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("MapFilename", new AttributeValue(mapFilename));
        item.put("Iterations", new AttributeValue().withN(String.valueOf(iterations)));
        item.put("ResultChecksum", new AttributeValue(checksum));
        item.put("CreatedAt", new AttributeValue().withS(Instant.now().toString()));
        item.put("Cost", new AttributeValue().withN(String.valueOf(cost)));

        dynamoDB.putItem(new PutItemRequest("GameOfLife", item));
        System.out.println("Inserted item into GameOfLife");
    }


    /* READ */
    private static void readFromCaptureTheFlag(int gridSize, int numBlue, int numRed, String flagType) {
    String compositeKey = "Blue=" + numBlue + "_Red=" + numRed + "_Flag=" + flagType;

    Map<String, AttributeValue> key = new HashMap<>();
    key.put("GridSize", new AttributeValue().withN(String.valueOf(gridSize)));
    key.put("CompositeKey", new AttributeValue(compositeKey));

    GetItemRequest request = new GetItemRequest()
        .withTableName("CaptureTheFlag")
        .withKey(key);

    GetItemResult result = dynamoDB.getItem(request);
    System.out.println("Read CaptureTheFlag Item: " + result.getItem());
}


    private static void readFromFifteenPuzzle(int size, int shuffles) {
    String compositeKey = "Shuffles=" + shuffles;

    Map<String, AttributeValue> key = new HashMap<>();
    key.put("Size", new AttributeValue().withN(String.valueOf(size)));
    key.put("CompositeKey", new AttributeValue(compositeKey));

    GetItemRequest request = new GetItemRequest()
        .withTableName("FifteenPuzzle")
        .withKey(key);

    GetItemResult result = dynamoDB.getItem(request);
    System.out.println("Read FifteenPuzzle Item: " + result.getItem());
}


    private static void readFromGameOfLife(String mapFilename, int iterations) {
    String compositeKey = "Iterations=" + iterations;

    Map<String, AttributeValue> key = new HashMap<>();
    key.put("MapFilename", new AttributeValue(mapFilename));
    key.put("CompositeKey", new AttributeValue(compositeKey));

    GetItemRequest request = new GetItemRequest()
        .withTableName("GameOfLife")
        .withKey(key);

    GetItemResult result = dynamoDB.getItem(request);
    System.out.println("Read GameOfLife Item: " + result.getItem());
}

}
