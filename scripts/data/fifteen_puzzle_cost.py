import pandas as pd
import numpy as np
import matplotlib
import matplotlib.pyplot as plt
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import PolynomialFeatures
from mpl_toolkits.mplot3d import Axes3D  # noqa: F401 (needed for 3D plots)

# Optional: change backend if GTK warning causes issues
# matplotlib.use('TkAgg')  # Uncomment this if needed
# matplotlib.use('Agg')    # Use this to save plot without displaying

# Load the CSV data
csv_path = 'fifteenpuzzle_metrics.csv'
df = pd.read_csv(csv_path)

# Prepare input (X) and output (y)
X = df[['size', 'shuffles']]
y = df['ninsts']

# Polynomial regression setup
degree = 2
poly = PolynomialFeatures(degree)
X_poly = poly.fit_transform(X)
model = LinearRegression()
model.fit(X_poly, y)

# Create a meshgrid for plotting
size_range = np.linspace(df['size'].min(), df['size'].max(), 30)
shuffles_range = np.linspace(df['shuffles'].min(), df['shuffles'].max(), 30)
size_grid, shuffles_grid = np.meshgrid(size_range, shuffles_range)
X_grid = np.column_stack([size_grid.ravel(), shuffles_grid.ravel()])

# Transform the grid data using polynomial features
X_grid_df = pd.DataFrame(X_grid, columns=['size', 'shuffles'])
X_grid_poly = poly.transform(X_grid_df)
y_pred_grid = model.predict(X_grid_poly).reshape(size_grid.shape)

# 3D plot
fig = plt.figure(figsize=(12, 8))
ax = fig.add_subplot(111, projection='3d')
ax.scatter(df['size'], df['shuffles'], df['ninsts'], color='blue', label='Actual Data', alpha=0.6)
ax.plot_surface(size_grid, shuffles_grid, y_pred_grid, cmap='viridis', alpha=0.6)

ax.set_xlabel('Size')
ax.set_ylabel('Shuffles')
ax.set_zlabel('Instructions (ninsts)')
ax.set_title('Estimated Cost Function: Instructions vs Size and Shuffles')
ax.legend()
plt.tight_layout()
plt.show()

# Print symbolic equation
feature_names = poly.get_feature_names_out(['size', 'shuffles'])
coefs = model.coef_
intercept = model.intercept_

terms = [f"{intercept:.2f}"]
for name, coef in zip(feature_names[1:], coefs[1:]):  # skip bias
    terms.append(f"{coef:.2f}*{name}")

equation = " + ".join(terms)
print("\nEstimated Cost Function (ninsts):")
print(f"ninsts ≈ {equation}")
