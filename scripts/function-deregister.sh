#!/bin/bash

source config.sh

aws lambda delete-function --function-name capturetheflag-lambda
aws lambda delete-function --function-name fifteenpuzzle-lambda
aws lambda delete-function --function-name gameoflife-lambda

echo "Lambda functions deleted successfully."

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

echo "Role policy detached successfully."

aws iam delete-role --role-name lambda-role

echo "Role deleted successfully."