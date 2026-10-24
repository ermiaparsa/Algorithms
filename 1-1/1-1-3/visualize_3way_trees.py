"""
3-Way Tree Forest Visualization:
Directly compares the component tree structures of all 3 algorithms:
1. Quick-Find (flat stars, depth ≤ 1)
2. Quick-Union (unbalanced, deep trees)
3. Weighted Quick-Union (balanced, height ≤ lg N)
"""

import os
import random
from typing import List, Dict, Tuple
import matplotlib.pyplot as plt

from union_find import QuickFindUF, QuickUnionUF, WeightedQuickUnionUF
from visualize_trees import compute_tree_layout, draw_forest, reshape_fa


def generate_3way_tree_comparison(n: int = 100, m: int = 88, seed: int = 381,
                                   output_path: str = "output/tree_structure_3way_comparison.png"):
    random.seed(seed)
    pairs = []
    while len(pairs) < m:
        p = random.randint(0, n - 1)
        q = random.randint(0, n - 1)
        if p != q:
            pairs.append((p, q))

    qf = QuickFindUF(n)
    qu = QuickUnionUF(n)
    wqu = WeightedQuickUnionUF(n)

    for p, q in pairs:
        qf.union(p, q)
        qu.union(p, q)
        wqu.union(p, q)

    fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(15, 12), facecolor="#ffffff")

    # 1. Quick-Find
    avg_qf = qf.average_depth()
    max_qf = qf.max_depth()
    title_qf_left = "Quick-Find (Flat Components)"
    title_qf_right = reshape_fa(f"متوسط ارتفاع: {avg_qf:.2f} (حداکثر: {max_qf}) | Avg Depth: {avg_qf:.2f}")
    draw_forest(ax1, qf.get_parent_pointers(), n, title_qf_left, title_qf_right, color_theme="#D9381E")

    # 2. Quick-Union
    avg_qu = qu.average_depth()
    max_qu = qu.max_depth()
    title_qu_left = "Quick-Union (Unbalanced Trees)"
    title_qu_right = reshape_fa(f"متوسط ارتفاع: {avg_qu:.2f} (حداکثر: {max_qu}) | Avg Depth: {avg_qu:.2f}")
    draw_forest(ax2, qu.get_parent_pointers(), n, title_qu_left, title_qu_right, color_theme="#2A75D3")

    # 3. Weighted Quick-Union
    avg_wqu = wqu.average_depth()
    max_wqu = wqu.max_depth()
    title_wqu_left = "Weighted Quick-Union (Balanced Trees)"
    title_wqu_right = reshape_fa(f"متوسط ارتفاع: {avg_wqu:.2f} (حداکثر: {max_wqu}) | Avg Depth: {avg_wqu:.2f}")
    draw_forest(ax3, wqu.get_parent_pointers(), n, title_wqu_left, title_wqu_right, color_theme="#1B8A5A")

    caption = reshape_fa(f"مقایسه‌ی ۳ الگوریتم برای {n} عنصر پس از {m} عمل اجتماع | Disjoint-Set Comparison: {n} Elements after {m} Unions")
    fig.text(0.5, 0.02, caption, ha="center", va="center", fontsize=12, color="#222222", fontweight='bold')

    plt.tight_layout(rect=[0.02, 0.04, 0.98, 0.98])
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    plt.savefig(output_path, dpi=300, facecolor=fig.get_facecolor(), edgecolor='none')
    plt.close()
    print(f"3-Way tree structure comparison saved to: {output_path}")


if __name__ == "__main__":
    generate_3way_tree_comparison()
