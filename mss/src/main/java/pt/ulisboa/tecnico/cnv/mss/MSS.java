package pt.ulisboa.tecnico.cnv.mss;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

/**
 * This sample demonstrates how to perform a few simple operations with the
 * Amazon DynamoDB service.
 */
public class MSS {

    // Get from environment variable AWS_REGION
    private static String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");

    private static AmazonDynamoDB dynamoDB;

    public MSS() {
        try {
            dynamoDB = AmazonDynamoDBClientBuilder.standard()
                    .withCredentials(new EnvironmentVariableCredentialsProvider())
                    .withRegion(AWS_REGION)
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to initialize DynamoDB client: " + e.getMessage());
        }
    }

    public static MSS getInstance() {
        return new MSS();
    }

    /*
     * public static void main(String[] args) throws Exception {
     * dynamoDB = AmazonDynamoDBClientBuilder.standard()
     * .withCredentials(new EnvironmentVariableCredentialsProvider())
     * .withRegion(AWS_REGION)
     * .build();
     *
     * try {
     * String tableName = "CaptureTheFlag";
     *
     *
     * // Describe our new table
     * DescribeTableRequest describeTableRequest = new
     * DescribeTableRequest().withTableName(tableName);
     * TableDescription tableDescription =
     * dynamoDB.describeTable(describeTableRequest).getTable();
     * System.out.println("Table Description: " + tableDescription);
     *
     * // USAGE Example
     * // Insert into CaptureTheFlag
     * /* insertIntoCaptureTheFlag(10, 5, 5, "A", 20);
     * insertIntoCaptureTheFlag(10, 3, 7, "B", 15);
     * insertIntoCaptureTheFlag(20, 10, 10, "C", 30);
     * insertIntoCaptureTheFlag(20, 5, 15, "D", 25);
     * insertIntoCaptureTheFlag(10, 2, 8, "E", 18);
     *
     *
     * readFromCaptureTheFlag(10, 5, 5, "A");
     *
     * System.out.println("Reading last 2 entries from CaptureTheFlag:");
     * getLastXFromCaptureTheFlag(2);
     */

    /* WRITE */
    public void insertIntoCaptureTheFlag(int gridSize, int numBlue, int numRed, String flagType, int Cost) {
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

    public void insertIntoFifteenPuzzle(int size, int shuffles, int cost) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("Size", new AttributeValue().withN(String.valueOf(size)));
        item.put("Shuffles", new AttributeValue().withN(String.valueOf(shuffles)));
        item.put("CreatedAt", new AttributeValue().withS(Instant.now().toString()));
        item.put("Cost", new AttributeValue().withN(String.valueOf(cost)));

        dynamoDB.putItem(new PutItemRequest("FifteenPuzzle", item));
        System.out.println("Inserted item into FifteenPuzzle");
    }

    public void insertIntoGameOfLife(String mapFilename, int iterations, String checksum, int cost) {
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
    public Map<String, AttributeValue> readFromCaptureTheFlag(int gridSize, int numBlue, int numRed, String flagType) {
        String compositeKey = "Blue=" + numBlue + "_Red=" + numRed + "_Flag=" + flagType;
        System.out.println("Reading CaptureTheFlag with composite key: " + compositeKey);
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("GridSize", new AttributeValue().withN(String.valueOf(gridSize)));
        key.put("CompositeKey", new AttributeValue(compositeKey));

        GetItemRequest request = new GetItemRequest()
                .withTableName("CaptureTheFlag")
                .withKey(key);

        GetItemResult result = dynamoDB.getItem(request);
        System.out.println("Read CaptureTheFlag Item: " + result.getItem());

        return result.getItem();
    }

    public Map<String, AttributeValue> readFromFifteenPuzzle(int size, int shuffles) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("Size", new AttributeValue().withN(String.valueOf(size)));
        key.put("Shuffles", new AttributeValue().withN(String.valueOf(shuffles)));

        GetItemRequest request = new GetItemRequest()
                .withTableName("FifteenPuzzle")
                .withKey(key);

        System.out.println("Reading FifteenPuzzle with Size: " + size + ", Shuffles: " + shuffles);

        GetItemResult result = dynamoDB.getItem(request);
        System.out.println("Read FifteenPuzzle Item: " + result.getItem());

        return result.getItem();
    }

    public Map<String, AttributeValue> readFromGameOfLife(String mapFilename, int iterations) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("MapFilename", new AttributeValue(mapFilename));
        key.put("Iterations", new AttributeValue().withN(String.valueOf(iterations)));

        GetItemRequest request = new GetItemRequest()
                .withTableName("GameOfLife")
                .withKey(key);

        GetItemResult result = dynamoDB.getItem(request);
        System.out.println("Read GameOfLife Item: " + result.getItem());

        return result.getItem();
    }

    public List<Map<String, AttributeValue>> getLastXFromCaptureTheFlag(int x) {
        ScanRequest scanRequest = new ScanRequest().withTableName("CaptureTheFlag");
        ScanResult result = dynamoDB.scan(scanRequest);
        List<Map<String, AttributeValue>> items = result.getItems() != null ? result.getItems() : new ArrayList<>();

        List<Map<String, AttributeValue>> lastX = items.stream()
                .filter(item -> item.containsKey("CreatedAt"))
                .sorted(Comparator
                        .comparing((Map<String, AttributeValue> item) -> Instant.parse(item.get("CreatedAt").getS()))
                        .reversed())
                .limit(x)
                .collect(Collectors.toList());

        System.out.println("Last " + x + " entries from CaptureTheFlag:");
        lastX.forEach(System.out::println);

        return lastX;
    }

    public List<Map<String, AttributeValue>> getLastXFromFifteenPuzzle(int x) {
        ScanRequest scanRequest = new ScanRequest().withTableName("FifteenPuzzle");
        ScanResult result = dynamoDB.scan(scanRequest);
        List<Map<String, AttributeValue>> items = result.getItems() != null ? result.getItems() : new ArrayList<>();

        List<Map<String, AttributeValue>> lastX = items.stream()
                .filter(item -> item.containsKey("CreatedAt"))
                .sorted(Comparator
                        .comparing((Map<String, AttributeValue> item) -> Instant.parse(item.get("CreatedAt").getS()))
                        .reversed())
                .limit(x)
                .collect(Collectors.toList());

        System.out.println("Last " + x + " entries from FifteenPuzzle:");
        lastX.forEach(System.out::println);

        return lastX;
    }

    public List<Map<String, AttributeValue>> getLastXFromGameOfLife(int x) {
        ScanRequest scanRequest = new ScanRequest().withTableName("GameOfLife");
        ScanResult result = dynamoDB.scan(scanRequest);
        List<Map<String, AttributeValue>> items = result.getItems() != null ? result.getItems() : new ArrayList<>();

        List<Map<String, AttributeValue>> lastX = items.stream()
                .filter(item -> item.containsKey("CreatedAt"))
                .sorted(Comparator
                        .comparing((Map<String, AttributeValue> item) -> Instant.parse(item.get("CreatedAt").getS()))
                        .reversed())
                .limit(x)
                .collect(Collectors.toList());

        System.out.println("Last " + x + " entries from GameOfLife:");
        lastX.forEach(System.out::println);

        return lastX;
    }

}
