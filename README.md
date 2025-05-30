# Games@Cloud

This project contains 6 modules:

1. `capturetheflag` - the Capture the Flag workload
2. `fifteenpuzzle` - the 15-Puzzle Solver workload
3. `gameoflife` - the Conway's Game of Life workload
4. `webserver` - the web server exposing the functionality of the workloads
5. `javassist` - the module that provides the Javassist Agent to instrument the workloads using tools and generate metrics
6. `scripts` - the scripts to deploy/terminate the project on AWS, to collect metrics from the workloads and to generate th cost estimation functions.


### Architecture

The architecture of the project is based on the following 3 main components:

- **Resource Manager**: This component is responsible for managing the resources of the system. It is composed of the Load Balancer(LB) and AutoScaler(AS) and it will be operating in a dedicated instance.
- **Workers**: This components will be running the workloads. They will be operating in a dedicated instance. Each instance will be running the WebServer, allowing to process requests from all 3 games. Workers can be divided into two distinct groups:
  - **VM Workers**: These workers will be running the workloads in a virtual machine.
  - **Lambda Workers**: These workers will be running the workloads in a serverless environment (to be done).
- **Metric Storage System**(to be done): This component is responsible for storing the metrics of the system. Every time a request is processed, its complexity/cost, alongside with its inputs, will be stored in the MSS. This data can later be retrieved b the LB to aid in the decision making of the request routing. The MSS will be implemented using DynamoDB, which will consist of 3 tables:
  - **Capture the Flag Table**: This table will store the metrics of the Capture the Flag workload.
  - **15-Puzzle Table**: This table will store the metrics of the 15-Puzzle workload.
  - **Game of Life Table**: This table will store the metrics of the Game of Life workload.

### AWS System Configurations

# AWS Load Balancer and Auto Scaling Group Configuration

This setup uses the following key parameters to ensure a scalable, reliable, and cost-effective deployment:

## Load Balancer

- **Name:** `CNV-LoadBalancer`  
  Provides an identifiable and manageable load balancer instance.

- **Listener Configuration:** HTTP traffic is accepted on port 80 and forwarded to backend instances on port 8000, allowing public web traffic to reach the application.

- **Availability Zone:** Restricted to `eu-west-1a` to control deployment scope and reduce latency within the zone (closest zone to Lisbon, Portugal).

- **Health Check:**  
  Targets the `/` endpoint on port 8000 with checks every 30 seconds and a 5-second timeout. Instances are marked unhealthy after 2 failed checks and healthy after 10 successful ones. This balances quick detection of failures with stability to avoid false positives.

## Auto Scaling Group

- **Launch Template:** Defines instance configuration consistently, including AMI, instance type (`t2.micro`), key pair, and security groups.

- **Load Balancer Association:** Ensures instances are registered with the LB for automatic traffic routing and health monitoring.

- **Availability Zone:** Matches the LB zone (`eu-west-1a`) to optimize network performance.

- **Health Check Type:** Uses LB health checks to assess instance health at the application level rather than just EC2 status.

- **Health Check Grace Period:** 60 seconds delay after instance launch before health evaluation to allow services to initialize properly.

- **Scaling Limits:** Maintains a minimum of 1 instance to ensure availability, scales up to 10 instances for handling load spikes, and starts with a desired capacity of 1. The minimum capacity could be 2 to ensure if a large number of requests are received at once, there is enough capacity to handle them without overwhelming the system. However the decision to set the minimum capacity to 1 is made to keep costs low while still ensuring that there is always at least one instance available to handle requests, thus avoiding resource wastage when demand is low.

This configuration provides a balance between availability, responsiveness, and cost-efficiency by ensuring that only healthy instances serve traffic and that capacity can scale dynamically based on demand.

### Scaling Up and Down Policies
- **Scale Up Policy:**  
  Triggered when the average CPU utilization across instances exceeds 70% for 2 minutes. This policy increases the instance count by 1 to handle increased load.
- **Scale Down Policy:**
  Triggered when the average CPU utilization across instances falls below 20% for 2 minutes. This policy decreases the instance count by 1 to reduce costs when demand is low.

These policies parameters were chosen based on past experiments and observations of workloads' behavior under different loads. The thresholds are set to ensure that the system can handle spikes in traffic without becoming overwhelmed, while also scaling down to save costs when the load decreases.


### How to run the project

In order to run the project, you need to have the following prerequisites installed:

- **Java 11** or higher
- **Python 3.8** or higher
- **Maven** - to build the project
- **AWS CLI** - to deploy the project on AWS
- **JQ** - to parse JSON responses from AWS CLI

An AWS Key Pair must also be created in the AWS region you want to deploy the project. This key pair will be used by the scripts to manipulate and set EC2 instances running the workloads. A security group must also be created in the AWS region you want to deploy the project. This security group will be used to allow traffic to the EC2 instances running the workloads. It accepts traffic on port 80 (HTTP), 443 (HTTPS) and port 8000 (Web Server) from anywhere, allowing public access to the web server. It also allows SSH traffic on port 22 from your IP address, enabling secure access to the instances for management and debugging.

All the script to deploy the project on AWS are located in the `scripts` folder.

1. Firstly, AWS credentials must be configured in order to run the scripts. This can be done by filling the config file located in `scripts/config.sh`. The config file will set all the environment variables needed to run the scripts:

- **`PATH`**  
  Extended to include `~/aws-cli-bin` so that AWS CLI commands installed locally can be run without specifying the full path.

- **`AWS_DEFAULT_REGION`**  
  Sets the default AWS region for CLI commands.

- **`AWS_DEFAULT_AVAILABILITY_ZONE`**  
  Specifies the default availability zone within the region. Used to constrain resource deployment to a specific zone.

- **`AWS_ACCOUNT_ID`**  
  Your AWS account number, used to uniquely identify your AWS resources and for permissions or automation scripts.

- **`AWS_ACCESS_KEY_ID`**  
  Your AWS access key ID for programmatic access to AWS services (part of credentials).

- **`AWS_SECRET_ACCESS_KEY`**  
  The secret key paired with the access key ID for secure authentication.

- **`AWS_EC2_SSH_KEYPAR_PATH`**  
  File path to the private SSH key used to connect securely to EC2 instances.

- **`AWS_SECURITY_GROUP`**  
  The security group ID that defines firewall rules applied to your EC2 instances.

- **`AWS_KEYPAIR_NAME`**  
  The name of the EC2 key pair registered in AWS, used to associate the SSH key with launched instances.

- **`WEBSERVER_PATH`**  
  Relative path to the webserver project directory, which might be used for deployment or configuration purposes.

2. In order to start the project on AWS:
    - Run the script `scripts/start.sh`. This script will create the necessary resources on AWS, including the an instance with all the necessary software installed, the Load Balancer and the Auto Scaling Group. It will also deploy the workloads on the instance and start the web server.

3. To terminate the project on AWS:
    - Run the script `scripts/stop.sh`. This script will terminate the Auto Scaling Group, the Load Balancer and the instance running the workloads. It will also delete all the resources created by the project on AWS, like the Snapshot, the AMI.


### Data Collection and Cost Estimation

In order to obtain the tools to collect the metrics from the workloads and to generate the cost estimation functions, additions were made to the handlers of the games' workloads. The handlers now use the Javassist tool ICount to calculate the number of instructions executed by the workloads. Inside `scripts/data/` there are several Python scripts that are used to collect the metrics from the workloads and to generate the cost estimation functions. The scripts are:

- **`ctf.py`** - performs several requests to the Capture the Flag workload, collects the metrics and saves them in a CSV file.
- **`fifteen_puzzle.py`** - performs several requests to the 15-Puzzle workload, collects the metrics and saves them in a CSV file.
- **`gol.py`** - performs several requests to the Game of Life workload, collects the metrics and saves them in a CSV file.
- **`ctf_cost.py`** - generates the cost estimation function for the Capture the Flag workload based on the collected metrics.
- **`fifteen_puzzle_cost.py`** - generates the cost estimation function for the 15-Puzzle workload based on the collected metrics.
- **`gol_cost.py`** - generates the cost estimation function for the Game of Life workload based on the collected metrics.

### Authors

- **Simão Sanguinho** - 102082
- **José Pereira** - 103252
- **Pedro Ribeiro** - 102663

Group 1 - Cloud Computing and Virtualization, 2024/2025, Instituto Superior Técnico, University of Lisbon, Portugal.
