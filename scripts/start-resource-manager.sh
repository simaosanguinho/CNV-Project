#!/bin/bash

source config.sh

# Step 0: Compile project
(cd $DIR && cd .. && mvn clean package) || exit 1

# Step 1: launch a vm instance.
# Run new instance.
aws ec2 run-instances \
    --image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
    --instance-type t2.micro \
    --key-name $AWS_KEYPAIR_NAME \
    --security-group-ids $AWS_SECURITY_GROUP \
    --monitoring Enabled=true | jq -r ".Instances[0].InstanceId" > resource.id
echo "New instance with id $(cat resource.id)."

# Wait for instance to be running.
aws ec2 wait instance-running --instance-ids $(cat resource.id)
echo "New instance with id $(cat resource.id) is now running."

# Extract DNS name.
aws ec2 describe-instances \
    --instance-ids $(cat resource.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > resource.dns
echo "New instance with id $(cat resource.id) has address $(cat resource.dns)."

# Wait for instance to have SSH ready.
while ! nc -z $(cat resource.dns) 22; do
    echo "Waiting for $(cat resource.dns):22 (SSH)..."
    sleep 0.5
done
echo "New instance with id $(cat resource.id) is ready for SSH access."

# Step 2: install software in the VM instance.
# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat resource.dns) $cmd

# Install web server.
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $RESOURCE_MANAGER_JAR_PATH ec2-user@$(cat resource.dns):
# also scp the config.sh
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH config.sh ec2-user@$(cat resource.dns):
#also the image id
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH image.id ec2-user@$(cat resource.dns):


# run project
BASE_DIR="/home/ec2-user"
RESOURCE_JAR="$BASE_DIR/resourcemanager-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
#java -jar /home/ec2-user/resourcemanager-1.0.0-SNAPSHOT-jar-with-dependencies.jar

cmd_run="source /home/ec2-user/config.sh; java -jar $RESOURCE_JAR"

#ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat resource.dns)
#cd /home/ec2-user/ && source /home/ec2-user/config.sh; java -jar /home/ec2-user/resourcemanager-1.0.0-SNAPSHOT-jar-with-dependencies.jar

ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd_run

# Setup resource manager to start on instance launch
cmd="echo \"cd /home/ec2-user/ && $cmd_run\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat resource.dns) $cmd

# Step 3: test VM instance.
# Requesting an instance reboot.
aws ec2 reboot-instances --instance-ids $(cat resource.id)
echo "Rebooting instance to test resource manager auto-start."

# Letting the instance shutdown.
sleep 1

# Wait for port 8000 to become available.
while ! nc -z $(cat resource.dns) 8001; do
    echo "Waiting for $(cat resource.dns):8001..."
    sleep 0.5
done

# Sending a query!
echo "Sending a query!"
HOST=$(cat resource.dns)
curl "http://$HOST:8001/gameoflife?mapFilename=glider-10-10.json&iterations=10"

# Step 7: delete the AMI instance.
#$DIR/deregister-image.sh
#echo "Image with id $(cat image.id) has been deregistered successfully."
