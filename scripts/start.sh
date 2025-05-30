#!/bin/bash

source config.sh

echo "=== CNV Project Full Deployment ==="

# Step 1: Create the AMI image
echo "Creating AMI image..."
$DIR/create-image.sh

# Step 2: Launch the deployment
echo "Launching deployment infrastructure..."
$DIR/launch-deployment-template.sh

# Step 3: Wait and test deployment
echo "Waiting for deployment to become healthy..."
while true; do
    HEALTHY_INSTANCES=$(aws autoscaling describe-auto-scaling-groups \
        --auto-scaling-group-names CNV-AutoScalingGroup \
        --query 'AutoScalingGroups[0].Instances[?HealthStatus==`Healthy`]' \
        --output text | wc -l)
    
    if [ "$HEALTHY_INSTANCES" -ge 1 ]; then
        echo "Deployment is healthy with $HEALTHY_INSTANCES healthy instance(s)."
        break
    else
        echo "Waiting for healthy instances... (current: $HEALTHY_INSTANCES)"
        sleep 10
    fi
done

# Get load balancer DNS
LB_DNS=$(aws elb describe-load-balancers \
    --load-balancer-names CNV-LoadBalancer \
    --query 'LoadBalancerDescriptions[0].DNSName' \
    --output text)

echo "=== Deployment Complete ==="
echo "Load balancer DNS: $LB_DNS"
echo "Test URL: http://$LB_DNS/gameoflife?mapFilename=glider-10-10.json&iterations=10"
echo "To terminate: $DIR/terminate-deployment.sh"