"""
Union-Find (Disjoint-Set) Algorithms Implementation
1. Quick-Find
2. Quick-Union
3. Weighted Quick-Union

Includes array-access instrumentation and tree structure depth metrics.
"""

from typing import List, Tuple


class QuickFindUF:
    """
    Quick-Find implementation.
    - find: O(1)
    - union: O(N)
    """
    def __init__(self, n: int):
        self.n = n
        self.id = list(range(n))
        self.component_count = n
        self.array_accesses = 0

    def find(self, p: int) -> int:
        self.array_accesses += 1
        return self.id[p]

    def connected(self, p: int, q: int) -> bool:
        return self.find(p) == self.find(q)

    def union(self, p: int, q: int) -> bool:
        self.array_accesses += 2
        p_id = self.id[p]
        q_id = self.id[q]

        if p_id == q_id:
            return False

        for i in range(self.n):
            self.array_accesses += 1
            if self.id[i] == p_id:
                self.id[i] = q_id
                self.array_accesses += 1

        self.component_count -= 1
        return True

    def depth(self, p: int) -> int:
        """In Quick-Find, any element not equal to its ID has depth 1 to root."""
        return 0 if self.id[p] == p else 1

    def all_depths(self) -> List[int]:
        return [self.depth(i) for i in range(self.n)]

    def average_depth(self) -> float:
        depths = self.all_depths()
        return sum(depths) / len(depths) if depths else 0.0

    def max_depth(self) -> int:
        depths = self.all_depths()
        return max(depths) if depths else 0

    def get_parent_pointers(self) -> List[int]:
        return list(self.id)


class QuickUnionUF:
    """
    Quick-Union implementation (trees, without weighting).
    - find: O(tree height) - worst case O(N)
    - union: O(tree height) - worst case O(N)
    """
    def __init__(self, n: int):
        self.n = n
        self.parent = list(range(n))
        self.component_count = n
        self.array_accesses = 0

    def find(self, p: int) -> int:
        curr = p
        while True:
            self.array_accesses += 1
            p_parent = self.parent[curr]
            if curr == p_parent:
                break
            curr = p_parent
        return curr

    def connected(self, p: int, q: int) -> bool:
        return self.find(p) == self.find(q)

    def union(self, p: int, q: int) -> bool:
        root_p = self.find(p)
        root_q = self.find(q)

        if root_p == root_q:
            return False

        # Make root of p point to root of q
        self.parent[root_p] = root_q
        self.array_accesses += 1
        self.component_count -= 1
        return True

    def depth(self, p: int) -> int:
        d = 0
        curr = p
        while curr != self.parent[curr]:
            curr = self.parent[curr]
            d += 1
        return d

    def all_depths(self) -> List[int]:
        return [self.depth(i) for i in range(self.n)]

    def average_depth(self) -> float:
        depths = self.all_depths()
        return sum(depths) / len(depths) if depths else 0.0

    def max_depth(self) -> int:
        depths = self.all_depths()
        return max(depths) if depths else 0

    def get_parent_pointers(self) -> List[int]:
        return list(self.parent)


class WeightedQuickUnionUF:
    """
    Weighted Quick-Union (WQU) implementation.
    Maintains tree sizes to attach smaller tree under larger tree.
    - find: O(log N)
    - union: O(log N)
    - Tree height guaranteed <= lg(N)
    """
    def __init__(self, n: int):
        self.n = n
        self.parent = list(range(n))
        self.sz = [1] * n
        self.component_count = n
        self.array_accesses = 0

    def find(self, p: int) -> int:
        curr = p
        while True:
            self.array_accesses += 1
            p_parent = self.parent[curr]
            if curr == p_parent:
                break
            curr = p_parent
        return curr

    def connected(self, p: int, q: int) -> bool:
        return self.find(p) == self.find(q)

    def union(self, p: int, q: int) -> bool:
        root_p = self.find(p)
        root_q = self.find(q)

        if root_p == root_q:
            return False

        # Link smaller tree to larger tree
        self.array_accesses += 2  # reading sz[root_p] and sz[root_q]
        if self.sz[root_p] < self.sz[root_q]:
            self.parent[root_p] = root_q
            self.sz[root_q] += self.sz[root_p]
            self.array_accesses += 3  # write parent, read+write sz
        else:
            self.parent[root_q] = root_p
            self.sz[root_p] += self.sz[root_q]
            self.array_accesses += 3

        self.component_count -= 1
        return True

    def depth(self, p: int) -> int:
        d = 0
        curr = p
        while curr != self.parent[curr]:
            curr = self.parent[curr]
            d += 1
        return d

    def all_depths(self) -> List[int]:
        return [self.depth(i) for i in range(self.n)]

    def average_depth(self) -> float:
        depths = self.all_depths()
        return sum(depths) / len(depths) if depths else 0.0

    def max_depth(self) -> int:
        depths = self.all_depths()
        return max(depths) if depths else 0

    def get_parent_pointers(self) -> List[int]:
        return list(self.parent)
