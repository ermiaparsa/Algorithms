import os
import random
from typing import List, Dict, Tuple
import matplotlib.pyplot as plt
from union_find import QuickFindUF, QuickUnionUF, WeightedQuickUnionUF


def reshape_fa(text: str) -> str:
    """Helper to reshape and format Persian/Arabic text for Matplotlib."""
    try:
        import arabic_reshaper
        from bidi.algorithm import get_display
        reshaped_text = arabic_reshaper.reshape(text)
        return get_display(reshaped_text)
    except Exception:
        return text



def compute_tree_layout(parents: List[int], n: int) -> Dict[int, Tuple[float, float]]:
    """
    Computes (x, y) coordinates for all nodes in the forest.
    - Roots are at y = 0.
    - Children are placed at y = -(depth).
    - Trees are placed horizontally side-by-side.
    """
    # Group children by parent
    children: Dict[int, List[int]] = {i: [] for i in range(n)}
    roots: List[int] = []

    for i in range(n):
        p = parents[i]
        if p == i:
            roots.append(i)
        else:
            children[p].append(i)

    # Sort roots: place non-trivial trees first, single isolated nodes together at the end
    trees_with_children = [r for r in roots if len(children[r]) > 0]
    isolated_roots = [r for r in roots if len(children[r]) == 0]

    # Deterministic sorting for nice visual order
    trees_with_children.sort(key=lambda r: len(children[r]), reverse=True)
    all_roots = isolated_roots + trees_with_children

    pos: Dict[int, Tuple[float, float]] = {}
    current_x = 0.0

    def layout_subtree(u: int, depth: int) -> Tuple[float, float]:
        """
        Recursively lays out subtree rooted at u.
        Returns (min_x, max_x) of the subtree.
        """
        nonlocal current_x
        u_children = children[u]

        if not u_children:
            x = current_x
            current_x += 1.0
            pos[u] = (x, -depth * 1.2)
            return (x, x)

        child_xs = []
        for v in u_children:
            min_x, max_x = layout_subtree(v, depth + 1)
            child_xs.append((min_x + max_x) / 2.0)
            current_x += 0.4  # small gap between child subtrees

        # Place parent at the average x of its children
        parent_x = sum(child_xs) / len(child_xs)
        pos[u] = (parent_x, -depth * 1.2)
        return (min(child_xs), max(child_xs))

    # Layout isolated nodes compactly at top
    for r in isolated_roots:
        pos[r] = (current_x, 0.0)
        current_x += 1.0

    if isolated_roots:
        current_x += 1.5

    # Layout multi-node trees
    for r in trees_with_children:
        layout_subtree(r, 0)
        current_x += 2.0  # spacing between trees

    return pos


def draw_forest(ax: plt.Axes, parents: List[int], n: int, title_left: str, title_right: str,
                color_theme: str = "#8B1E0F"):
    """
    Renders the forest onto a matplotlib axes.
    """
    pos = compute_tree_layout(parents, n)

    # Draw edges
    for child in range(n):
        p = parents[child]
        if p != child:
            x_vals = [pos[child][0], pos[p][0]]
            y_vals = [pos[child][1], pos[p][1]]
            ax.plot(x_vals, y_vals, color="#222222", linewidth=1.2, zorder=1)

    # Draw nodes
    x_nodes = [pos[i][0] for i in range(n)]
    y_nodes = [pos[i][1] for i in range(n)]
    ax.scatter(x_nodes, y_nodes, color="#111111", s=32, zorder=2)

    # Titles and annotations
    ax.text(0.01, 0.95, title_left, transform=ax.transAxes,
            fontsize=15, fontweight='bold', color=color_theme,
            verticalalignment='top', horizontalalignment='left')

    ax.text(0.99, 0.95, title_right, transform=ax.transAxes,
            fontsize=15, fontweight='bold', color=color_theme,
            verticalalignment='top', horizontalalignment='right')

    ax.axis("off")
    ax.set_aspect("equal", adjustable="datalim")


def generate_tree_comparison(n: int = 100, m: int = 88, seed: int = 381,
                             output_path: str = "output/tree_structure_comparison.png"):
    """
    Simulates N=100 elements after M=88 unions and generates comparison chart.
    """
    random.seed(seed)
    pairs = []
    while len(pairs) < m:
        p = random.randint(0, n - 1)
        q = random.randint(0, n - 1)
        if p != q:
            pairs.append((p, q))

    qu = QuickUnionUF(n)
    wqu = WeightedQuickUnionUF(n)

    for p, q in pairs:
        qu.union(p, q)
        wqu.union(p, q)

    avg_d_qu = qu.average_depth()
    max_d_qu = qu.max_depth()
    avg_d_wqu = wqu.average_depth()
    max_d_wqu = wqu.max_depth()

    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(15, 9), facecolor="#ffffff")
    fig.patch.set_facecolor("#ffffff")

    title_qu_left = "quick-union"
    title_qu_right = reshape_fa(f"متوسط ارتفاع تا ریشه: {avg_d_qu:.2f} (حداکثر: {max_d_qu})")

    title_wqu_left = "weighted"
    title_wqu_right = reshape_fa(f"متوسط ارتفاع تا ریشه: {avg_d_wqu:.2f} (حداکثر: {max_d_wqu})")

    draw_forest(ax1, qu.get_parent_pointers(), n, title_qu_left, title_qu_right, color_theme="#A32414")
    draw_forest(ax2, wqu.get_parent_pointers(), n, title_wqu_left, title_wqu_right, color_theme="#A32414")

    # Bottom caption exactly like the slide
    caption = reshape_fa(f"ساختمان داده‌ی زیرمجموعه‌های مجزا شامل {n} عنصر (پس از {m} عمل اجتماع)")
    fig.text(0.5, 0.03, caption, ha="center", va="center", fontsize=13, color="#0C6291", fontweight='bold')

    plt.tight_layout(rect=[0.02, 0.06, 0.98, 0.98])
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    plt.savefig(output_path, dpi=300, facecolor=fig.get_facecolor(), edgecolor='none')
    plt.close()
    print(f"Tree structure comparison saved to: {output_path}")
    print(f"Quick-Union: Avg Depth = {avg_d_qu:.2f}, Max Depth = {max_d_qu}")
    print(f"Weighted Quick-Union: Avg Depth = {avg_d_wqu:.2f}, Max Depth = {max_d_wqu}")


if __name__ == "__main__":
    generate_tree_comparison()
