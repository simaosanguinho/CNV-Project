#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

export PATH=<path to aws installation>:$PATH
export AWS_DEFAULT_REGION=<insert here your aws region, e.g., eu-west-1>
export AWS_DEFAULT_AVAILABILITY_ZONE=<insert here your aws availability zone, e.g., eu-west-1a>
export AWS_ACCOUNT_ID=<insert here your aws account id>
export AWS_ACCESS_KEY_ID=<insert here your aws access key>
export AWS_SECRET_ACCESS_KEY=<insert here your aws secret access key>
export AWS_EC2_SSH_KEYPAR_PATH=<path to aws ssh keypair>
export AWS_SECURITY_GROUP=<name of your security group>
export AWS_KEYPAIR_NAME=<name of your aws keypair>
export WEBSERVER_PATH=../../CNV-Project
export IMAGE_PATH=/home/ec2-user/image.id
export RESOURCE_MANAGER_JAR_PATH=../../CNV-Project/resourcemanager/target/resourcemanager-1.0.0-SNAPSHOT-jar-with-dependencies.jar

