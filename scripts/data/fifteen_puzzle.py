import csv
import requests
import os
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import csv

import re

DO_SHOW = True  # Set to False if you don't want to visualize the results

# Define the parameters to test
sizes = [10, 15, 20, 25, 30, 35, 40, 45, 50]  # Example sizes for the fifteen puzzle
shuffles = [10, 20, 30, 40, 50, 60, 70]  # Example number of shuffles

# Output CSV file
output_file = 'fifteenpuzzle_results.csv'

# Make requests and store results
results = []

for size in sizes:
    print(f"Testing size={size} with shuffles={shuffles}...")
    for shuffle in shuffles:
        url = f"http://localhost:8000/fifteenpuzzle?size={size}&shuffles={shuffle}"
        try:
            response = requests.get(url)
            response.raise_for_status()
            data = response.text  # or response.json() if it's JSON
            results.append({'size': size, 'shuffles': shuffle, 'response': data})
            print(f"Success for size={size}, shuffles={shuffle}: {data}")
        except requests.RequestException as e:
            print(f"Error for size={size}, shuffles={shuffle}: {e}")
            results.append({'size': size, 'shuffles': shuffle, 'response': f"ERROR: {e}"})
            
            
METRICS_PATH = '../../metrics/fifteen-puzzle/'
# files are always named as "... (size, shuffles"
# content of files is always this format:
#Thread: <X>
#nblocks: <Y>
#nmethods: <Z>
#ninsts: <W>,



# go over the all files in the metrics path and create a csv file with the results
METRICS_PATH = '../../metrics/fifteen-puzzle/'
output_csv = 'fifteenpuzzle_metrics.csv'

data_rows = []

# Regex to extract (size, shuffles) from the filename
filename_pattern = re.compile(r'\((\d+),\s*(\d+)\s*\)?')

# Regex to extract nblocks and ninsts from file content
nblocks_pattern = re.compile(r'nblocks:\s*(\d+)')
ninsts_pattern = re.compile(r'ninsts:\s*(\d+)')

# Go through all files in the metrics directory
for filename in os.listdir(METRICS_PATH):
    match = filename_pattern.search(filename)
    if not match:
        continue  # skip files that don't match expected pattern
    size, shuffles = map(int, match.groups())

    filepath = os.path.join(METRICS_PATH, filename)
    with open(filepath, 'r') as f:
        content = f.read()

        nblocks_match = nblocks_pattern.search(content)
        ninsts_match = ninsts_pattern.search(content)

        if nblocks_match and ninsts_match:
            nblocks = int(nblocks_match.group(1))
            ninsts = int(ninsts_match.group(1))

            data_rows.append({
                'size': size,
                'shuffles': shuffles,
                'ninsts': ninsts,
                'nblocks': nblocks
            })
        else:
            print(f"Skipping file {filename} due to missing data.")

# Write to CSV
with open(output_csv, 'w', newline='') as csvfile:
    fieldnames = ['size', 'shuffles', 'ninsts', 'nblocks']
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)

    writer.writeheader()
    for row in data_rows:
        writer.writerow(row)

print(f"CSV saved to {output_csv}")