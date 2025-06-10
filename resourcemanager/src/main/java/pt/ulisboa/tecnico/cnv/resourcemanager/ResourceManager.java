package pt.ulisboa.tecnico.cnv.resourcemanager;

import java.util.Arrays;
import java.util.List;
import pt.ulisboa.tecnico.cnv.resourcemanager.autoscaler.AutoScaler;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.LoadBalancer;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import pt.ulisboa.tecnico.cnv.mss.MSS;

public class ResourceManager {
    public static void main(String[] args) {
        // Get configuration from environment variables
        String region = System.getenv("AWS_DEFAULT_REGION");
        String securityGroup = System.getenv("AWS_SECURITY_GROUP");
        String keyPairName = System.getenv("AWS_KEYPAIR_NAME");
        String amiIdPath = System.getenv("IMAGE_PATH");

        // Initialize AWS clients
        Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.of(region != null ? region : "ue-west-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        CloudWatchClient cloudWatchClient = CloudWatchClient.builder()
                .region(Region.of(region != null ? region : "ue-west-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        // Configuration parameters - adjust these as needed
        String amiId = null;
        if (amiIdPath != null) {
            // read the AMI ID from the specified file
            try {
                amiId = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(amiIdPath))).trim();
                System.out.println("AMI ID: " + amiId);
            } catch (java.io.IOException e) {
                System.err.println("Error reading AMI ID from file: " + e.getMessage());
            }
        } else {
            System.err.println("AMI ID not provided. Please set the IMAGE_PATH environment variable.");
        }
        String instanceType = "t3.micro";
        List<String> securityGroupIds = Arrays.asList(securityGroup != null ? securityGroup : "default");
        String subnetId = null; // Will use default subnet if null
        int minInstances = 1;
        int maxInstances = 10;

        // Initialize the instance pool
        InstancePool instancePool = new InstancePool(
                ec2Client, cloudWatchClient, amiId, instanceType, keyPairName,
                securityGroupIds, subnetId, minInstances, maxInstances);
        /* MSS mss = MSS.getInstance(); */
        Thread loadbalancer = new Thread(new LoadBalancer(instancePool));
        Thread autoscaler = new Thread(new AutoScaler(instancePool));
        loadbalancer.start();
        autoscaler.start();
    }
}