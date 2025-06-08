#!/bin/bash

source config.sh

echo "Testing the Lambda functions..."

echo "Invoking Capture the Flag Lambda function..."
aws lambda invoke \
  --function-name capturetheflag-lambda \
  --cli-binary-format raw-in-base64-out \
  --payload '{"gridSize": "20", "numBlueAgents": "5", "numRedAgents": "5", "flagPlacementType": "A"}' \
  out_1 \
  --log-type Tail \
  --query 'LogResult' \
  --output text | base64 -d
cat out_1

echo "Invoking Fifteen Puzzle Lambda function..."
aws lambda invoke \
  --function-name fifteenpuzzle-lambda \
  --cli-binary-format raw-in-base64-out \
  --payload '{"size": "10", "shuffles": "10"}' \
  out_2 \
  --log-type Tail \
  --query 'LogResult' \
  --output text | base64 -d

cat out_2

echo "Invoking Game of Life Lambda function..."
aws lambda invoke \
  --function-name gameoflife-lambda \
  --cli-binary-format raw-in-base64-out \
  --payload '{"mapFilename": "glider-10-10.json", "iterations": "20"}' \
  out_3 \
  --log-type Tail \
  --query 'LogResult' \
  --output text | base64 -d

cat out_3
echo "All Lambda functions invoked successfully."
