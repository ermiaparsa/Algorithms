"""
Comprehensive Benchmark & Analysis Suite for Disjoint-Set (Union-Find) Algorithms:
1. Quick-Find (QF)
2. Quick-Union (QU)
3. Weighted Quick-Union (WQU)

Metrics measured:
- Wall-clock Execution Time (ms)
- Machine-independent Cost (Array Accesses)
- Structural Evolution (Average Depth & Max Depth vs operations)
"""

import os
import time
import random
from typing import List, Dict, Tuple
import matplotlib.pyplot as plt
import numpy as np

from union_find import QuickFindUF, QuickUnionUF, WeightedQuickUnionUF


def reshape_fa(text: str) -> str:
    """Helper to reshape Persian text if available."""
    try:
        import arabic_reshaper
        from bidi.algorithm import get_display
        return get_display(arabic_reshaper.reshape(text))
    except Exception:
        return text


# Set styling for clean, professional, publication-grade plots
plt.rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial', 'Liberation Sans']
plt.rcParams['axes.edgecolor'] = '#444444'
plt.rcParams['axes.linewidth'] = 0.8
plt.rcParams['grid.color'] = '#e0e0e0'
plt.rcParams['grid.linestyle'] = '--'
plt.rcParams['grid.alpha'] = 0.7

COLORS = {
    'Quick-Find': '#D9381E',          # Vivid Red/Orange
    'Quick-Union': '#2A75D3',         # Royal Blue
    'Weighted Quick-Union': '#1B8A5A' # Forest Green
}

MARKERS = {
    'Quick-Find': 's',
    'Quick-Union': '^',
    'Weighted Quick-Union': 'o'
}


def run_scaling_benchmark(n_values: List[int], seed: int = 42) -> Dict:
    """
    Measures execution time and array accesses for each algorithm across varying N.
    For each N, generates N random union operations followed by N random find operations.
    """
    results = {
        'n': n_values,
        'Quick-Find': {'time': [], 'accesses': []},
        'Quick-Union': {'time': [], 'accesses': []},
        'Weighted Quick-Union': {'time': [], 'accesses': []}
    }

    print("Running Scaling Benchmark (Time & Array Accesses)...")
    for n in n_values:
        print(f"  Testing N = {n}...")
        random.seed(seed)
        m = n  # N union operations
        union_pairs = [(random.randint(0, n - 1), random.randint(0, n - 1)) for _ in range(m)]
        find_pairs = [(random.randint(0, n - 1), random.randint(0, n - 1)) for _ in range(m)]

        # 1. Quick-Find
        qf = QuickFindUF(n)
        t0 = time.perf_counter()
        for p, q in union_pairs:
            qf.union(p, q)
        for p, q in find_pairs:
            qf.connected(p, q)
        t_qf = (time.perf_counter() - t0) * 1000.0  # in ms
        results['Quick-Find']['time'].append(t_qf)
        results['Quick-Find']['accesses'].append(qf.array_accesses)

        # 2. Quick-Union
        qu = QuickUnionUF(n)
        t0 = time.perf_counter()
        for p, q in union_pairs:
            qu.union(p, q)
        for p, q in find_pairs:
            qu.connected(p, q)
        t_qu = (time.perf_counter() - t0) * 1000.0
        results['Quick-Union']['time'].append(t_qu)
        results['Quick-Union']['accesses'].append(qu.array_accesses)

        # 3. Weighted Quick-Union
        wqu = WeightedQuickUnionUF(n)
        t0 = time.perf_counter()
        for p, q in union_pairs:
            wqu.union(p, q)
        for p, q in find_pairs:
            wqu.connected(p, q)
        t_wqu = (time.perf_counter() - t0) * 1000.0
        results['Weighted Quick-Union']['time'].append(t_wqu)
        results['Weighted Quick-Union']['accesses'].append(wqu.array_accesses)

    return results


def run_depth_evolution(n: int = 1000, num_steps: int = 100, seed: int = 42) -> Dict:
    """
    Tracks average depth and max depth as the number of union operations increases from 1 to N.
    """
    print(f"\nRunning Tree Depth Evolution for N = {n}...")
    random.seed(seed)
    m_total = n
    union_pairs = [(random.randint(0, n - 1), random.randint(0, n - 1)) for _ in range(m_total)]

    step_interval = max(1, m_total // num_steps)
    m_checkpoints = list(range(1, m_total + 1, step_interval))
    if m_checkpoints[-1] != m_total:
        m_checkpoints.append(m_total)

    results = {
        'm': m_checkpoints,
        'Quick-Find': {'avg_depth': [], 'max_depth': []},
        'Quick-Union': {'avg_depth': [], 'max_depth': []},
        'Weighted Quick-Union': {'avg_depth': [], 'max_depth': []}
    }

    qf = QuickFindUF(n)
    qu = QuickUnionUF(n)
    wqu = WeightedQuickUnionUF(n)

    checkpoint_set = set(m_checkpoints)

    for op_idx, (p, q) in enumerate(union_pairs, start=1):
        qf.union(p, q)
        qu.union(p, q)
        wqu.union(p, q)

        if op_idx in checkpoint_set:
            results['Quick-Find']['avg_depth'].append(qf.average_depth())
            results['Quick-Find']['max_depth'].append(qf.max_depth())

            results['Quick-Union']['avg_depth'].append(qu.average_depth())
            results['Quick-Union']['max_depth'].append(qu.max_depth())

            results['Weighted Quick-Union']['avg_depth'].append(wqu.average_depth())
            results['Weighted Quick-Union']['max_depth'].append(wqu.max_depth())

    return results


def plot_comprehensive_benchmark(scaling_res: Dict, depth_res: Dict, output_path: str = "output/algorithms_comparison_benchmark.png"):
    """
    Creates a 4-panel comprehensive comparison figure.
    """
    fig, axes = plt.subplots(2, 2, figsize=(16, 12), facecolor="#f8f9fa")

    # Panel 1: Execution Time vs N
    ax1 = axes[0, 0]
    ax1.set_facecolor('#ffffff')
    for alg in ['Quick-Find', 'Quick-Union', 'Weighted Quick-Union']:
        ax1.plot(scaling_res['n'], scaling_res[alg]['time'],
                 marker=MARKERS[alg], linewidth=2.2, markersize=7,
                 color=COLORS[alg], label=alg)
    ax1.set_title("A. Execution Time vs Input Size (N Operations)", fontsize=13, fontweight='bold', pad=10)
    ax1.set_xlabel("Input Size N (Objects & Operations)", fontsize=11)
    ax1.set_ylabel("Execution Time (milliseconds)", fontsize=11)
    ax1.legend(frameon=True, facecolor='#ffffff', edgecolor='#cccccc', fontsize=10)
    ax1.grid(True)

    # Panel 2: Array Accesses vs N (Log-Log)
    ax2 = axes[0, 1]
    ax2.set_facecolor('#ffffff')
    for alg in ['Quick-Find', 'Quick-Union', 'Weighted Quick-Union']:
        ax2.loglog(scaling_res['n'], scaling_res[alg]['accesses'],
                   marker=MARKERS[alg], linewidth=2.2, markersize=7,
                   color=COLORS[alg], label=alg)
    # Add reference O(N^2) and O(N log N) lines
    n_ref = np.array(scaling_res['n'])
    ref_n2 = 1.0 * (n_ref ** 2)
    ref_nlogn = 10.0 * n_ref * np.log2(n_ref)
    ax2.loglog(n_ref, ref_n2, 'k--', alpha=0.4, label='O(N²) Reference')
    ax2.loglog(n_ref, ref_nlogn, 'k:', alpha=0.4, label='O(N log N) Reference')

    ax2.set_title("B. Cost Model: Total Array Accesses (Log-Log Scale)", fontsize=13, fontweight='bold', pad=10)
    ax2.set_xlabel("Input Size N", fontsize=11)
    ax2.set_ylabel("Array Accesses (Reads + Writes)", fontsize=11)
    ax2.legend(frameon=True, facecolor='#ffffff', edgecolor='#cccccc', fontsize=10)
    ax2.grid(True, which="both")

    # Panel 3: Average Depth vs Number of Unions
    ax3 = axes[1, 0]
    ax3.set_facecolor('#ffffff')
    for alg in ['Quick-Find', 'Quick-Union', 'Weighted Quick-Union']:
        ax3.plot(depth_res['m'], depth_res[alg]['avg_depth'],
                 linewidth=2.2, color=COLORS[alg], label=alg)
    ax3.set_title("C. Average Tree Depth vs Number of Unions (N = 1,000)", fontsize=13, fontweight='bold', pad=10)
    ax3.set_xlabel("Number of Union Operations (M)", fontsize=11)
    ax3.set_ylabel("Average Depth to Root", fontsize=11)
    ax3.legend(frameon=True, facecolor='#ffffff', edgecolor='#cccccc', fontsize=10)
    ax3.grid(True)

    # Panel 4: Maximum Tree Depth (Height) vs Number of Unions
    ax4 = axes[1, 1]
    ax4.set_facecolor('#ffffff')
    for alg in ['Quick-Find', 'Quick-Union', 'Weighted Quick-Union']:
        ax4.plot(depth_res['m'], depth_res[alg]['max_depth'],
                 linewidth=2.2, color=COLORS[alg], label=alg)
    # Theoretical guarantee for WQU: lg(N)
    lg_n = np.log2(1000)
    ax4.axhline(y=lg_n, color='#1B8A5A', linestyle='--', alpha=0.6,
                label=f'WQU Theoretical Limit: lg(1000) ≈ {lg_n:.1f}')

    ax4.set_title("D. Maximum Tree Depth (Height) vs Number of Unions (N = 1,000)", fontsize=13, fontweight='bold', pad=10)
    ax4.set_xlabel("Number of Union Operations (M)", fontsize=11)
    ax4.set_ylabel("Maximum Tree Depth", fontsize=11)
    ax4.legend(frameon=True, facecolor='#ffffff', edgecolor='#cccccc', fontsize=10)
    ax4.grid(True)

    plt.suptitle("Union-Find Algorithms: Empirical & Structural Comparison\nQuick-Find vs Quick-Union vs Weighted Quick-Union",
                 fontsize=16, fontweight='bold', y=0.99)
    plt.tight_layout(rect=[0.02, 0.02, 0.98, 0.96])

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    plt.savefig(output_path, dpi=300, facecolor=fig.get_facecolor(), edgecolor='none')
    plt.close()
    print(f"\nComprehensive benchmark figure saved to: {output_path}")


def plot_summary_bars(n: int = 5000, seed: int = 42, output_path: str = "output/summary_metrics_bar_chart.png"):
    """
    Produces side-by-side bar charts for a representative benchmark size (N=5000).
    """
    random.seed(seed)
    m = n
    union_pairs = [(random.randint(0, n - 1), random.randint(0, n - 1)) for _ in range(m)]
    find_pairs = [(random.randint(0, n - 1), random.randint(0, n - 1)) for _ in range(m)]

    data = {}
    for name, cls in [('Quick-Find', QuickFindUF),
                      ('Quick-Union', QuickUnionUF),
                      ('Weighted Quick-Union', WeightedQuickUnionUF)]:
        uf = cls(n)
        t0 = time.perf_counter()
        for p, q in union_pairs:
            uf.union(p, q)
        for p, q in find_pairs:
            uf.connected(p, q)
        runtime = (time.perf_counter() - t0) * 1000.0
        data[name] = {
            'time': runtime,
            'accesses': uf.array_accesses,
            'avg_depth': uf.average_depth(),
            'max_depth': uf.max_depth()
        }

    algs = list(data.keys())
    fig, axes = plt.subplots(1, 4, figsize=(18, 5), facecolor="#f8f9fa")

    metrics = [
        ('time', 'Total Runtime (ms)\n[Lower is Better]', False),
        ('accesses', 'Array Accesses (Count)\n[Lower is Better]', True),
        ('avg_depth', 'Average Tree Depth\n[Lower is Better]', False),
        ('max_depth', 'Max Tree Depth (Height)\n[Lower is Better]', False)
    ]

    for ax, (key, label, is_log) in zip(axes, metrics):
        ax.set_facecolor('#ffffff')
        values = [data[a][key] for a in algs]
        bar_colors = [COLORS[a] for a in algs]
        bars = ax.bar(algs, values, color=bar_colors, width=0.55, edgecolor='#333333', linewidth=0.8)
        ax.set_title(label, fontsize=11, fontweight='bold', pad=8)
        if is_log:
            ax.set_yscale('log')
        ax.grid(axis='y', linestyle='--', alpha=0.7)
        ax.tick_params(axis='x', rotation=15)

        # Value annotations on bars
        for bar in bars:
            h = bar.get_height()
            if is_log:
                txt = f"{h:.1e}" if h > 1e4 else f"{int(h)}"
            elif key == 'time':
                txt = f"{h:.1f}ms"
            elif key == 'avg_depth':
                txt = f"{h:.2f}"
            else:
                txt = f"{int(h)}"
            ax.text(bar.get_x() + bar.get_width() / 2., h * (1.05 if is_log else 1.02),
                    txt, ha='center', va='bottom', fontsize=9, fontweight='bold')

    plt.suptitle(f"Performance & Tree Metrics Breakdown for N = {n:,} Elements ({m:,} Unions + {m:,} Finds)",
                 fontsize=14, fontweight='bold', y=1.03)
    plt.tight_layout()
    plt.savefig(output_path, dpi=300, facecolor=fig.get_facecolor(), edgecolor='none', bbox_inches='tight')
    plt.close()
    print(f"Summary bar chart saved to: {output_path}")

    return data


if __name__ == "__main__":
    n_values = [100, 500, 1000, 2000, 4000, 7000, 10000]
    scaling_res = run_scaling_benchmark(n_values)
    depth_res = run_depth_evolution(n=1000, num_steps=100)
    plot_comprehensive_benchmark(scaling_res, depth_res)
    plot_summary_bars(n=5000)
