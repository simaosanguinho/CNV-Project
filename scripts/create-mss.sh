#!/bin/bash

source config.sh

echo "Creating CaptureTheFlag table..."

aws dynamodb create-table \
  --table-name CaptureTheFlag \
  --attribute-definitions \
    AttributeName=GridSize,AttributeType=N \
    AttributeName=CompositeKey,AttributeType=S \
    AttributeName=NumRedAgents,AttributeType=N \
    AttributeName=FlagPlacementType,AttributeType=S \
    AttributeName=CreatedAt,AttributeType=S \
  --key-schema \
    AttributeName=GridSize,KeyType=HASH \
    AttributeName=CompositeKey,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --table-class STANDARD \
  --global-secondary-indexes '[
    {
      "IndexName": "RedAgentsIndex",
      "KeySchema": [
        { "AttributeName": "NumRedAgents", "KeyType": "HASH" }
      ],
      "Projection": {
        "ProjectionType": "ALL"
      }
    },
    {
      "IndexName": "FlagPlacementIndex",
      "KeySchema": [
        { "AttributeName": "FlagPlacementType", "KeyType": "HASH" }
      ],
      "Projection": {
        "ProjectionType": "ALL"
      }
    },
    {
      "IndexName": "CreatedAtIndex",
      "KeySchema": [
        { "AttributeName": "CreatedAt", "KeyType": "HASH" }
      ],
      "Projection": {
        "ProjectionType": "ALL"
      }
    }
  ]'

aws dynamodb wait table-exists --table-name CaptureTheFlag
echo "CaptureTheFlag table created and is now active."


echo "Creating FifteenPuzzle table..."

aws dynamodb create-table \
    --table-name FifteenPuzzle \
    --attribute-definitions \
        AttributeName=Size,AttributeType=N \
        AttributeName=Shuffles,AttributeType=N \
        AttributeName=CreatedAt,AttributeType=S \
    --key-schema \
        AttributeName=Size,KeyType=HASH \
        AttributeName=Shuffles,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --table-class STANDARD \
    --global-secondary-indexes '[
      {
        "IndexName": "CreatedAtIndex",
        "KeySchema": [
          { "AttributeName": "CreatedAt", "KeyType": "HASH" }
        ],
        "Projection": {
          "ProjectionType": "ALL"
        }
      }
    ]'

aws dynamodb wait table-exists --table-name FifteenPuzzle
echo "FifteenPuzzle table created and is now active."


echo "Creating GameOfLife table..."

aws dynamodb create-table \
    --table-name GameOfLife \
    --attribute-definitions \
        AttributeName=MapFilename,AttributeType=S \
        AttributeName=Iterations,AttributeType=N \
        AttributeName=CreatedAt,AttributeType=S \
    --key-schema \
        AttributeName=MapFilename,KeyType=HASH \
        AttributeName=Iterations,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --table-class STANDARD \
    --global-secondary-indexes '[
      {
        "IndexName": "CreatedAtIndex",
        "KeySchema": [
          { "AttributeName": "CreatedAt", "KeyType": "HASH" }
        ],
        "Projection": {
          "ProjectionType": "ALL"
        }
      }
    ]'

aws dynamodb wait table-exists --table-name GameOfLife
echo "GameOfLife table created and is now active."

echo "All DynamoDB tables created successfully."
