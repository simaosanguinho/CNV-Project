#!/bin/bash

source config.sh

echo "=== CNV Project Full Deployment ==="

# Step 1: Create MSS - DynamoDB
echo "Creating MSS..."
$DIR/create-mss.sh

# Step 2: Create Lambda Functions
echo "Creating Lambda Functions..."
$DIR/function-register.sh

# Step 3: Create the AMI image
echo "Creating AMI image..."
$DIR/create-image.sh

# Step 4: Start up Resource Manager (Auto Scaler + LoadBalancer)
echo "Creating Resource Manager..."
$DIR/start-resource-manager.sh


echo "=== Deployment Complete ==="
echo "Resource Manager is running."
echo "Test URL: http://$(cat resource.dns):8001/gameoflife?mapFilename=glider-10-10.json&iterations=10"
echo "To terminate: $DIR/stop.sh"