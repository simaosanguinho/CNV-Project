#!/bin/bash

source config.sh

echo "=== CNV Project Full Termination ==="

# Step 1: Create the AMI image
echo "Stopping AMI image..."
$DIR/deregister-image.sh

# Step 2: Launch the deployment
echo "Stopping deployment infrastructure..."
$DIR/terminate-deployment-template.sh