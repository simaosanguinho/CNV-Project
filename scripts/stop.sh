#!/bin/bash

source config.sh

echo "=== CNV Project Full Termination ==="

echo "Stopping VM instance..."
aws ec2 terminate-instances --instance-ids $(cat instance.id)

echo "Stopping AMI image..."
$DIR/deregister-image.sh

echo "Stopping deployment infrastructure..."
$DIR/terminate-deployment-template.sh