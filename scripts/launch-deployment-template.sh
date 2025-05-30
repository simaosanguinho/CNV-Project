#!/bin/bash

source config.sh

# Create load balancer and configure health check.
aws elb create-load-balancer \
	--load-balancer-name CNV-LoadBalancer \
	--listeners "Protocol=HTTP,LoadBalancerPort=80,InstanceProtocol=HTTP,InstancePort=8000" \
  	--availability-zones $AWS_DEFAULT_AVAILABILITY_ZONE

aws elb configure-health-check \
  	--load-balancer-name CNV-LoadBalancer \
  	--health-check Target=HTTP:8000/,Interval=30,UnhealthyThreshold=2,HealthyThreshold=10,Timeout=5

# Create Launch Template
aws ec2 create-launch-template \
	--launch-template-name CNV-LaunchTemplate \
	--version-description "v1" \
	--launch-template-data "{
		\"ImageId\": \"$(cat image.id)\",
		\"InstanceType\": \"t2.micro\",
		\"KeyName\": \"$AWS_KEYPAIR_NAME\",
		\"SecurityGroupIds\": [\"$AWS_SECURITY_GROUP\"],
		\"Monitoring\": {\"Enabled\": true}
}"

# Create auto scaling group with launch template.
aws autoscaling create-auto-scaling-group \
  	--auto-scaling-group-name CNV-AutoScalingGroup \
  	--launch-template LaunchTemplateName=CNV-LaunchTemplate,Version=1 \
  	--load-balancer-names CNV-LoadBalancer \
  	--availability-zones eu-west-1a \
  	--health-check-type ELB \
  	--health-check-grace-period 60 \
  	--min-size 1 \
  	--max-size 10 \
  	--desired-capacity 1

echo "Auto Scaling Group created. Now creating scaling policies and alarms..."

# Create Scale Up Policy
SCALE_UP_POLICY_ARN=$(aws autoscaling put-scaling-policy \
	--auto-scaling-group-name CNV-AutoScalingGroup \
	--policy-name CNV-ScaleUpPolicy \
	--policy-type SimpleScaling \
	--adjustment-type ChangeInCapacity \
	--scaling-adjustment 1 \
	--cooldown 60 \
	--query 'PolicyARN' \
	--output text)

echo "Scale Up Policy ARN: $SCALE_UP_POLICY_ARN"

# Create Scale Down Policy  
SCALE_DOWN_POLICY_ARN=$(aws autoscaling put-scaling-policy \
	--auto-scaling-group-name CNV-AutoScalingGroup \
	--policy-name CNV-ScaleDownPolicy \
	--policy-type SimpleScaling \
	--adjustment-type ChangeInCapacity \
	--scaling-adjustment -1 \
	--cooldown 60 \
	--query 'PolicyARN' \
	--output text)

echo "Scale Down Policy ARN: $SCALE_DOWN_POLICY_ARN"

# Create CloudWatch Alarm for High CPU (Scale Up)
aws cloudwatch put-metric-alarm \
	--alarm-name CNV-HighCPUAlarm \
	--alarm-description "Alarm when CPU exceeds 30%" \
	--metric-name CPUUtilization \
	--namespace AWS/EC2 \
	--statistic Average \
	--period 60 \
	--threshold 30 \
	--comparison-operator GreaterThanThreshold \
	--evaluation-periods 2 \
	--alarm-actions $SCALE_UP_POLICY_ARN \
	--dimensions Name=AutoScalingGroupName,Value=CNV-AutoScalingGroup

echo "High CPU Alarm created (threshold: 30%)"

# Create CloudWatch Alarm for Low CPU (Scale Down)
aws cloudwatch put-metric-alarm \
	--alarm-name CNV-LowCPUAlarm \
	--alarm-description "Alarm when CPU is below 10%" \
	--metric-name CPUUtilization \
	--namespace AWS/EC2 \
	--statistic Average \
	--period 60 \
	--threshold 10 \
	--comparison-operator LessThanThreshold \
	--evaluation-periods 2 \
	--alarm-actions $SCALE_DOWN_POLICY_ARN \
	--dimensions Name=AutoScalingGroupName,Value=CNV-AutoScalingGroup

echo "Low CPU Alarm created (threshold: 10%)"

echo ""
echo "=== Auto Scaling Configuration Complete ==="
echo "✅ CPU-based auto scaling is ready!"
echo ""
echo "Scale Up: CPU > 30% for 2 minutes"
echo "Scale Down: CPU < 10% for 2 minutes"
echo "Cooldown: 60 seconds between scaling actions"