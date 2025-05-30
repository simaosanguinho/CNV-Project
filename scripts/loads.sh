#!/bin/bash
URL="http://cnv-loadbalancer-37842190.eu-west-1.elb.amazonaws.com/gameoflife?mapFilename=glider-10-10.json&iterations=1000"

echo "Starting load test for 10 minutes..."
END_TIME=$(($(date +%s) + 600))  # 10 minutes from now

while [ $(date +%s) -lt $END_TIME ]; do
    for i in {1..100}; do
        curl -s "$URL" > /dev/null &
    done
    sleep 1
done

wait
echo "Load test completed."