#!/bin/bash

source config.sh

aws ec2 deregister-image --image-id $(cat image.id) --region us-east-1