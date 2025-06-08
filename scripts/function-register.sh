#!/bin/bash

source config.sh

aws iam create-role \
	--role-name lambda-role \
	--assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}'

sleep 5

aws iam attach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

sleep 5

# Create the Capture the Flag Lambda function
aws lambda create-function \
	--function-name capturetheflag-lambda \
	--zip-file fileb://../capturetheflag/target/capturetheflag-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.capturetheflag.CaptureTheFlagHandler \
	--runtime java11 \
	--timeout 200 \
	--memory-size 512 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

echo "Capture the Flag Lambda function created successfully."

# Create the Fifteen Puzzle Lambda function
aws lambda create-function \
	--function-name fifteenpuzzle-lambda \
	--zip-file fileb://../fifteenpuzzle/target/fifteenpuzzle-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.fifteenpuzzle.FifteenPuzzleHandler \
	--runtime java11 \
	--timeout 200 \
	--memory-size 512 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

echo "Fifteen Puzzle Lambda function created successfully."

# Create the Game of Life Lambda function
aws lambda create-function \
	--function-name gameoflife-lambda \
	--zip-file fileb://../gameoflife/target/gameoflife-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.gameoflife.GameOfLifeHandler \
	--runtime java11 \
	--timeout 200 \
	--memory-size 512 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

echo "Game of Life Lambda function created successfully."