#!/bin/bash

source config.sh

aws dynamodb delete-table --table-name CaptureTheFlag
echo "CaptureTheFlag table deleted successfully."

aws dynamodb delete-table --table-name FifteenPuzzle
echo "FifteenPuzzle table deleted successfully."

aws dynamodb delete-table --table-name GameOfLife
echo "GameOfLife table deleted successfully."