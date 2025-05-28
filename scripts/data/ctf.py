import csv
import requests
import os
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

DO_SHOW = True  # Set to False if you don't want to visualize the results

# Define the parameters to test
sizes = [10, 20, 30, 40, 50]  # Example sizes for the fifteen puzzle
shuffles = [10, 20, 30, 40, 50]  # Example number of shuffles

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

metrics = []
for filename in os.listdir(METRICS_PATH):
    with open(os.path.join(METRICS_PATH, filename), 'r') as file:
        content = file.readlines()
        nblocks = int(content[1].strip().split(': ')[1].rstrip(','))
        nmethods = int(content[2].strip().split(': ')[1].rstrip(','))
        ninsts = int(content[3].strip().split(': ')[1])
        size = filename.split('(')[1].split(',')[0].strip()
        shuffle = filename.split(',')[1].strip()
        
        metrics.append({
            'nblocks': nblocks,
            'nmethods': nmethods,
            'ninsts': ninsts,
            'size': size,
            'shuffles': shuffle
        })
        print(f"Processed metrics for size={size}, shuffles={shuffle}: nblocks={nblocks}, nmethods={nmethods}, ninsts={ninsts}")
        
# Write results to CSV
with open(output_file, 'w', newline='') as csvfile:
    fieldnames = ['size', 'shuffles', 'nblocks', 'nmethods', 'ninsts']
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    
    writer.writeheader()
    for result in metrics:
        writer.writerow(result)
print(f"Results written to {output_file}")


def visualize_metrics(metrics):
    # Load the CSV file
    df = pd.read_csv('fifteenpuzzle_results.csv')

    # Set seaborn style
    sns.set(style="whitegrid")

    # Metrics to visualize
    metrics = ['nblocks', 'nmethods', 'ninsts']

    for metric in metrics:
        pivot = df.pivot_table(index='size', columns='shuffles', values=metric)
        
        plt.figure(figsize=(10, 6))
        sns.heatmap(pivot, annot=True, fmt='.0f', cmap='YlGnBu')
        plt.title(f'Heatmap of {metric} by Size and Shuffles')
        plt.xlabel('Shuffles')
        plt.ylabel('Size')
        plt.tight_layout()
        
        filename = f'{metric}_heatmap.png'
        plt.savefig(filename)
        plt.close()
        print(f"Saved: {filename}")
        
        
def visualize_results_trisurf():
    # Load the CSV file
    df = pd.read_csv('fifteenpuzzle_results.csv')

    # Metrics to plot
    metrics = ['nblocks', 'nmethods', 'ninsts']

    for metric in metrics:
        fig = plt.figure(figsize=(10, 7))
        ax = fig.add_subplot(111, projection='3d')
        
        x = df['size']
        y = df['shuffles']
        z = df[metric]
        
        surf = ax.plot_trisurf(x, y, z, cmap='viridis', edgecolor='none', linewidth=0.2, antialiased=True)
        
        ax.set_title(f'3D Surface Plot of {metric}')
        ax.set_xlabel('Size')
        ax.set_ylabel('Shuffles')
        ax.set_zlabel(metric)
        fig.colorbar(surf, shrink=0.5, aspect=5)
        
        plt.tight_layout()
        filename = f'{metric}_trisurf.png'
        plt.savefig(filename)
        plt.close()
        print(f"Saved: {filename}")

if DO_SHOW:
    visualize_metrics(metrics)
    visualize_results_trisurf()
        