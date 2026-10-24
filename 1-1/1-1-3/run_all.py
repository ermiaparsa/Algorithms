"""
Main Orchestration Script for Union-Find Analysis
Executes:
1. Tree Structure Visualization (replicating the textbook/slide diagram)
2. Comprehensive 4-panel Benchmark (Execution Time, Array Accesses, Tree Depth Evolution)
3. Summary Metrics Bar Charts (at N = 5,000)
4. Displays comparative analysis summary table
"""

import os
import sys
import time

from visualize_trees import generate_tree_comparison
from benchmark import run_scaling_benchmark, run_depth_evolution, plot_comprehensive_benchmark, plot_summary_bars


def print_comparison_table():
    headers = ["Algorithm", "Initialize", "Union", "Find", "Tree Height Guarantee", "Typical Avg Depth"]
    rows = [
        ["Quick-Find", "O(N)", "O(N)", "O(1)", "≤ 1 (Flat tree)", "1.00"],
        ["Quick-Union", "O(N)", "O(Tree Height)", "O(Tree Height)", "O(N) (Can degrade)", "5.0 - 7.0+"],
        ["Weighted Quick-Union", "O(N)", "O(log N)", "O(log N)", "≤ lg(N) (Guaranteed)", "1.4 - 2.0"]
    ]

    col_widths = [22, 14, 16, 16, 25, 20]
    sep_line = "+" + "+".join(["-" * (w + 2) for w in col_widths]) + "+"
    header_line = "|" + "|".join([f" {h:<{col_widths[i]}} " for i, h in enumerate(headers)]) + "|"

    print("\n" + "=" * 90)
    print("                    THEORETICAL COMPLEXITY COMPARISON TABLE")
    print("=" * 90)
    print(sep_line)
    print(header_line)
    print(sep_line)
    for r in rows:
        row_str = "|" + "|".join([f" {r[i]:<{col_widths[i]}} " for i in range(len(r))]) + "|"
        print(row_str)
    print(sep_line)


def main():
    print("=================================================================")
    print("      DISJOINT-SET (UNION-FIND) COMPARATIVE ANALYSIS SUITE       ")
    print("=================================================================")

    os.makedirs("output", exist_ok=True)

    # 1. Generate Tree Structure Comparison (matching user's slide)
    print("\n[Step 1/4] Generating Tree Structure Comparison (N=100, M=88)...")
    generate_tree_comparison(n=100, m=88, seed=381, output_path="output/tree_structure_comparison.png")

    from visualize_3way_trees import generate_3way_tree_comparison
    print("\n[Step 2/4] Generating 3-Way Tree Structure Comparison (Quick-Find vs QU vs Weighted QU)...")
    generate_3way_tree_comparison(n=100, m=88, seed=381, output_path="output/tree_structure_3way_comparison.png")

    # 3. Run Comprehensive Benchmarks
    print("\n[Step 3/4] Running Empirical Benchmarks & Scaling Analysis...")
    n_values = [100, 500, 1000, 2000, 4000, 7000, 10000]
    scaling_res = run_scaling_benchmark(n_values, seed=42)
    depth_res = run_depth_evolution(n=1000, num_steps=100, seed=42)

    plot_comprehensive_benchmark(scaling_res, depth_res, output_path="output/algorithms_comparison_benchmark.png")

    # 4. Summary Bar Charts
    print("\n[Step 4/4] Generating Summary Metrics Breakdown (N=5,000)...")
    summary_data = plot_summary_bars(n=5000, seed=42, output_path="output/summary_metrics_bar_chart.png")

    # 4. Print Summary Table
    print_comparison_table()

    print("\n" + "=" * 90)
    print("                         EMPIRICAL RESULTS AT N = 5,000")
    print("=" * 90)
    print(f"{'Algorithm':<24} | {'Runtime (ms)':<15} | {'Array Accesses':<18} | {'Avg Depth':<12} | {'Max Depth':<10}")
    print("-" * 90)
    for alg, stats in summary_data.items():
        print(f"{alg:<24} | {stats['time']:<15.2f} | {stats['accesses']:<18,d} | {stats['avg_depth']:<12.2f} | {stats['max_depth']:<10d}")
    print("-" * 90)

    print("\nAll figures generated successfully in the 'output/' directory:")
    print("  1. output/tree_structure_comparison.png     -> Disjoint forest structure (recreating slide)")
    print("  2. output/algorithms_comparison_benchmark.png -> 4-panel scaling & depth evolution graphs")
    print("  3. output/summary_metrics_bar_chart.png     -> Side-by-side performance bar charts")
    print("=================================================================\n")


if __name__ == "__main__":
    main()
