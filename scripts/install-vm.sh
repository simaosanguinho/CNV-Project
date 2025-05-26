#!/bin/bash
source config.sh

# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Install web server.
zip -r CNV-Project.zip $WEBSERVER_PATH
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH CNV-Project.zip ec2-user@$(cat instance.dns):

# Build web server.
# unzip project
cmd_unzip="unzip -d CNV-Project CNV-Project.zip"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd_unzip

# run project
BASE_DIR="/home/ec2-user/CNV-Project/CNV-Project"
WEBSERVER_JAR="$BASE_DIR/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
JAVASSIST_JAR="$BASE_DIR/javassist/target/JavassistWrapper-1.0-jar-with-dependencies.jar"
AGENT_OUTPUT_DIR="$BASE_DIR/output"

cmd_run="java -cp "$WEBSERVER_JAR" \
    -Xbootclasspath/a:"$JAVASSIST_JAR" \
    -javaagent:"$WEBSERVER_JAR"=ICount:pt.ulisboa.tecnico.cnv.capturetheflag,pt.ulisboa.tecnico.cnv.fifteenpuzzle,pt.ulisboa.tecnico.cnv.gameoflife:"$AGENT_OUTPUT_DIR" \
    pt.ulisboa.tecnico.cnv.webserver.WebServer"
    
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd_run

# Setup web server to start on instance launch
cmd="echo \""$cmd_run"\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd