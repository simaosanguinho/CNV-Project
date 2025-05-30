import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from sklearn.preprocessing import PolynomialFeatures
from sklearn.linear_model import LinearRegression
import re

# Load CSV
df = pd.read_csv('gameoflife_results_with_metrics.csv')
df = df.dropna(subset=['ninsts'])

# Extract size from mapFilename (e.g., glider-10-10.json -> size=10)
def extract_size(filename):
    match = re.search(r'glider-(\d+)-\d+\.json', filename)
    if match:
        return int(match.group(1))
    else:
        return None

df['size'] = df['mapFilename'].apply(extract_size)
df = df.dropna(subset=['size'])

# Features and target
X = df[['size', 'iterations']].values
y = df['ninsts'].values

# Fit polynomial regression model (degree=2)
poly = PolynomialFeatures(degree=2)
X_poly = poly.fit_transform(X)
model = LinearRegression()
model.fit(X_poly, y)

# Prepare grid for plotting surface
size_range = np.linspace(df['size'].min(), df['size'].max(), 30)
iterations_range = np.linspace(df['iterations'].min(), df['iterations'].max(), 30)
size_grid, iter_grid = np.meshgrid(size_range, iterations_range)
X_grid = np.c_[size_grid.ravel(), iter_grid.ravel()]
X_grid_poly = poly.transform(X_grid)
y_pred = model.predict(X_grid_poly).reshape(size_grid.shape)

# Print cost function formula
intercept = model.intercept_
coeffs = model.coef_
terms = poly.get_feature_names_out(['size', 'iterations'])

formula_parts = [f"{intercept:.2f}"]
for coef, term in zip(coeffs[1:], terms[1:]):  # Skip intercept term
    sign = '+' if coef >= 0 else '-'
    formula_parts.append(f" {sign} {abs(coef):.2f}*{term}")

print("\nEstimated cost function (ninsts):")
print("ninsts =", "".join(formula_parts))


# Plot
fig = plt.figure(figsize=(12, 8))
ax = fig.add_subplot(111, projection='3d')

surf = ax.plot_surface(size_grid, iter_grid, y_pred, cmap='viridis', alpha=0.8)
ax.scatter(df['size'], df['iterations'], df['ninsts'], color='red', s=20, label='Actual Data')

ax.set_xlabel('Map Size')
ax.set_ylabel('Iterations')
ax.set_zlabel('Number of Instructions (ninsts)')  # units here
ax.set_title('Estimated Cost Function for Game of Life')

# Remove colorbar - just comment out or remove this line:
# fig.colorbar(surf, shrink=0.5, aspect=5)

ax.legend()
plt.tight_layout()
plt.show()


