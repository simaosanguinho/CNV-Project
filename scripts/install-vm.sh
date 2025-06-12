#!/bin/bash
source config.sh

# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd


# Install web server.
# zip -r CNV-Project.zip $WEBSERVER_PATH
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH \
  "$WEBSERVER_PATH/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar" \
  ec2-user@$(cat instance.dns):/home/ec2-user/

# also scp the config.sh
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH \
  "$WEBSERVER_PATH/scripts/config.sh" \
  ec2-user@$(cat instance.dns):/home/ec2-user/


WEBSERVER_JAR="/home/ec2-user/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
JAVA_AGENT="ICount:pt.ulisboa.tecnico.cnv.capturetheflag,pt.ulisboa.tecnico.cnv.fifteenpuzzle,pt.ulisboa.tecnico.cnv.gameoflife:output"

cmd_run="java -javaagent:$WEBSERVER_JAR=$JAVA_AGENT -jar $WEBSERVER_JAR"


# Setup web server to start on instance launch
cmd="echo \"source /home/ec2-user/config.sh && $cmd_run\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd
