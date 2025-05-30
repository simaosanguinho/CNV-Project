import pandas as pd
import numpy as np
from sklearn.preprocessing import PolynomialFeatures, OneHotEncoder
from sklearn.linear_model import LinearRegression
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
import matplotlib.pyplot as plt
import seaborn as sns

# Load data
df = pd.read_csv('ctf_results_with_metrics.csv')

# Drop any rows with missing metrics
df = df.dropna(subset=['ninsts'])

# Features and target
X = df[['gridSize', 'numBlueAgents', 'numRedAgents', 'flagPlacementType']]
y = df['ninsts']

# Create preprocessor to one-hot encode the categorical column
preprocessor = ColumnTransformer(
    transformers=[
        ('cat', OneHotEncoder(drop='first'), ['flagPlacementType'])
    ],
    remainder='passthrough'  # Keep numeric columns
)

# Build pipeline: one-hot encode → polynomial features → linear regression
degree = 2
pipeline = Pipeline([
    ('preprocess', preprocessor),
    ('poly', PolynomialFeatures(degree=degree, include_bias=False)),
    ('reg', LinearRegression())
])

# Fit the model
pipeline.fit(X, y)

# Print model equation
feature_names = pipeline.named_steps['preprocess'].get_feature_names_out()
poly_feature_names = pipeline.named_steps['poly'].get_feature_names_out(feature_names)

coefs = pipeline.named_steps['reg'].coef_
intercept = pipeline.named_steps['reg'].intercept_

# Assemble equation
terms = [f"{intercept:.2f}"]
for name, coef in zip(poly_feature_names, coefs):
    terms.append(f"{coef:.2f}*{name}")

equation = " + ".join(terms)
print("\nEstimated Cost Function (ninsts):")
print(f"ninsts ≈ {equation}")

# Optional: 2D/3D visualization for projections
# We'll do a heatmap of ninsts vs gridSize vs numBlueAgents for flagPlacementType='A' and fixed red agents
""" plot_df = df[df['flagPlacementType'] == 'A']
pivot = plot_df.pivot_table(index='gridSize', columns='numBlueAgents', values='ninsts', aggfunc='mean')

plt.figure(figsize=(10, 6))
sns.heatmap(pivot, annot=True, fmt=".0f", cmap='viridis')
plt.title("Instruction Count Heatmap (flag=A, varying gridSize vs numBlueAgents)")
plt.ylabel("gridSize")
plt.xlabel("numBlueAgents")
plt.tight_layout()
plt.show()
 """