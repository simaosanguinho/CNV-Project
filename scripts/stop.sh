#!/bin/bash

source config.sh

echo "=== CNV Project Full Termination ==="

# Get snapshot id
aws ec2 describe-images --image-ids $(cat image.id) \
  | jq -r '.Images[0].BlockDeviceMappings[].Ebs.SnapshotId' > image.snapshot

echo "Stopping VM instance..."
aws ec2 terminate-instances --instance-ids $(cat instance.id)

echo "Stopping AMI image..."
$DIR/deregister-image.sh

echo "Deleting Snapshot..."
aws ec2 delete-snapshot --snapshot-id $(cat image.snapshot)

echo "Deleting MSS..."
$DIR/delete-mss.sh

echo "Deleting Lambda Functions..."
$DIR/function-deregister.sh

echo "Stopping Resource Manager..."
aws ec2 terminate-instances --instance-ids $(cat resource.id)