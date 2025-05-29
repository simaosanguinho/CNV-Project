import requests
import csv
import os
import re

# === Parameters ===
map_files = ['glider-10-10.json', 'glider-11-11.json', 'glider-12-12.json', 'glider-13-13.json',
                'glider-14-14.json', 'glider-15-15.json', 'glider-16-16.json', 'glider-17-17.json',
                'glider-18-18.json', 'glider-19-19.json', 'glider-20-20.json', 'glider-25-25.json',
                'glider-30-30.json', 'glider-35-35.json', 'glider-40-40.json', 'glider-45-45.json',
                'glider-50-50.json']
iterations_list = [10, 20, 30, 40, 50, 100, 200, 300]

METRICS_PATH = '../../metrics/gol-metrics/'
output_file = 'gameoflife_results_with_metrics.csv'

results = []

# === Step 1: Query HTTP Server ===
for map_file in map_files:
    for iterations in iterations_list:
        url = f"http://localhost:8000/gameoflife?mapFilename={map_file}&iterations={iterations}"
        print(f"Requesting: {url}")
        try:
            response = requests.get(url)
            response.raise_for_status()
            data = response.text
            print(f"✔ Success: {map_file}, iterations={iterations}")
        except requests.RequestException as e:
            print(f"✘ Error: {map_file}, iterations={iterations} - {e}")
            data = f"ERROR: {e}"

        results.append({
            'mapFilename': map_file,
            'iterations': iterations,
        })

# === Step 2: Read Metrics Files ===
# File format: "ICOUNT Thread XX after Game of Life (10, glider-10-10.json)"
filename_pattern = re.compile(r'\((\d+),\s*([^)]+)\)')
nblocks_pattern = re.compile(r'nblocks:\s*(\d+)')
ninsts_pattern = re.compile(r'ninsts:\s*(\d+)')

metrics_data = {}  # key: (mapFilename, iterations) -> metrics

for filename in os.listdir(METRICS_PATH):
    match = filename_pattern.search(filename)
    if not match:
        continue
    iterations_str, map_file = match.groups()
    iterations = int(iterations_str.strip())

    filepath = os.path.join(METRICS_PATH, filename)
    with open(filepath, 'r') as f:
        content = f.read()

        nblocks_match = nblocks_pattern.search(content)
        ninsts_match = ninsts_pattern.search(content)

        if nblocks_match and ninsts_match:
            metrics_data[(map_file.strip(), iterations)] = {
                'nblocks': int(nblocks_match.group(1)),
                'ninsts': int(ninsts_match.group(1))
            }
        else:
            print(f"⚠ Skipping {filename} — missing metrics")

# === Step 3: Combine Results and Metrics ===
for row in results:
    key = (row['mapFilename'], row['iterations'])
    metric = metrics_data.get(key, {'nblocks': None, 'ninsts': None})
    row['nblocks'] = metric['nblocks']
    row['ninsts'] = metric['ninsts']

# === Step 4: Write CSV ===
with open(output_file, 'w', newline='') as f:
    fieldnames = ['mapFilename', 'iterations', 'ninsts', 'nblocks']
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(results)

print(f"\n✅ Results and metrics saved to: {output_file}")
