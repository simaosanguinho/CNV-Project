import requests
import csv
import os
import re

# Params to test
grid_sizes = [10, 15, 20, 25, 30, 35, 40, 45, 50]
num_blue_agents = [1, 3, 5, 7, 9]
num_red_agents = [1, 3, 5, 7, 9]
flag_placements = ['A', 'B', 'C']

# Paths
METRICS_PATH = '../../metrics/ctf/'  # your new path
output_file = 'ctf_results_with_metrics.csv'

results = []

# Step 1: Call API and collect responses
for size in grid_sizes:
    print(f"Testing gridSize={size} with blue_agents={num_blue_agents}, red_agents={num_red_agents}...")
    for blue in num_blue_agents:
        for red in num_red_agents:
            for flag in flag_placements:
                url = (
                    f"http://localhost:8001/capturetheflag?"
                    f"gridSize={size}&numBlueAgents={blue}&numRedAgents={red}&flagPlacementType={flag}"
                )
                try:
                    response = requests.get(url)
                    response.raise_for_status()
                    data = response.text

                    results.append({
                        'gridSize': size,
                        'numBlueAgents': blue,
                        'numRedAgents': red,
                        'flagPlacementType': flag,
                    })

                    print(f"Success: gridSize={size}, blue={blue}, red={red}, flag={flag}")

                except requests.RequestException as e:
                    print(f"Error: gridSize={size}, blue={blue}, red={red}, flag={flag} - {e}")
                    results.append({
                        'gridSize': size,
                        'numBlueAgents': blue,
                        'numRedAgents': red,
                        'flagPlacementType': flag,
                    
                    })


# # Step 2: Read metrics files
#
# # Regex to extract 4-tuple from filename
# filename_pattern = re.compile(r'\((\d+),\s*(\d+),\s*(\d+),\s*([A-Z])\)')
#
# nblocks_pattern = re.compile(r'nblocks:\s*(\d+)')
# ninsts_pattern = re.compile(r'ninsts:\s*(\d+)')
#
# metrics_data = {}  # key: (size, blue, red, flag) -> {nblocks, ninsts}
#
# for filename in os.listdir(METRICS_PATH):
#     match = filename_pattern.search(filename)
#     if not match:
#         continue
#     size_f, blue_f, red_f, flag_f = match.groups()
#     size_f, blue_f, red_f = int(size_f), int(blue_f), int(red_f)
#
#     filepath = os.path.join(METRICS_PATH, filename)
#     with open(filepath, 'r') as f:
#         content = f.read()
#
#         nblocks_match = nblocks_pattern.search(content)
#         ninsts_match = ninsts_pattern.search(content)
#
#         if nblocks_match and ninsts_match:
#             nblocks = int(nblocks_match.group(1))
#             ninsts = int(ninsts_match.group(1))
#
#             metrics_data[(size_f, blue_f, red_f, flag_f)] = {
#                 'nblocks': nblocks,
#                 'ninsts': ninsts
#             }
#         else:
#             print(f"Skipping {filename} due to missing nblocks/ninsts")
#
# # Step 3: Add metrics info to results
# for res in results:
#     key = (res['gridSize'], res['numBlueAgents'], res['numRedAgents'], res['flagPlacementType'])
#     metric = metrics_data.get(key, {'nblocks': None, 'ninsts': None})
#     res['nblocks'] = metric['nblocks']
#     res['ninsts'] = metric['ninsts']
#
# # Step 4: Write all to CSV
# with open(output_file, 'w', newline='') as f:
#     fieldnames = ['gridSize', 'numBlueAgents', 'numRedAgents', 'flagPlacementType', 'ninsts', 'nblocks']
#     writer = csv.DictWriter(f, fieldnames=fieldnames)
#     writer.writeheader()
#     for row in results:
#         writer.writerow(row)
#
# print(f"\n✅ Results with metrics saved to {output_file}")
