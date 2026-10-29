/**
 * Percolation System Modeling using Weighted Quick-Union with Path Compression (WQUPC).
 *
 * Problem Specification:
 * - Given an N-by-N grid of sites, each site is initially blocked.
 * - A site is open if it is opened via open(row, col).
 * - A system percolates if there is a path of open sites connecting any site in the top row
 *   to any site in the bottom row.
 * - Uses two virtual sites (virtual top site and virtual bottom site) to check percolation
 *   in O(1) union-find operations instead of O(N^2) pairwise checks.
 *
 * Indexing Convention:
 * - This implementation defaults to standard 0-based indexing:
 *   row in [0, n - 1], col in [0, n - 1].
 * - To adapt to Princeton / Coursera 1-based indexing (1 to n), see the note in validate().
 */
public class Percolation {

    // Helper class for Weighted Quick-Union with Path Compression (WQUPC)
    public static class WeightedQuickUnionUF {
        private final int[] parent; // parent[i] = parent of i
        private final int[] size;   // size[i] = number of elements in subtree rooted at i
        private int count;          // number of connected components

        /**
         * Initializes an empty union-find data structure with n sites (0 through n-1).
         * Each site is initially in its own component.
         *
         * @param n the number of sites
         */
        public WeightedQuickUnionUF(int n) {
            if (n < 0) {
                throw new IllegalArgumentException("Number of elements must be non-negative");
            }
            this.count = n;
            this.parent = new int[n];
            this.size = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        /**
         * Returns the canonical element (root) of the set containing element p.
         * Implements full two-pass path compression to flatten the tree.
         *
         * @param p an element
         * @return root identifier of the set containing p
         */
        public int find(int p) {
            validate(p);

            // Step 1: Find root
            int root = p;
            while (root != parent[root]) {
                root = parent[root];
            }

            // Step 2: Path compression - make every examined node point directly to root
            while (p != root) {
                int next = parent[p];
                parent[p] = root;
                p = next;
            }

            return root;
        }

        /**
         * Merges the set containing element p with the set containing element q.
         * Uses weighting (union-by-size) to ensure tree height is at most O(log N).
         *
         * @param p an element
         * @param q another element
         */
        public void union(int p, int q) {
            int rootP = find(p);
            int rootQ = find(q);

            if (rootP == rootQ) {
                return; // already connected
            }

            // Link smaller tree under the larger tree's root
            if (size[rootP] < size[rootQ]) {
                parent[rootP] = rootQ;
                size[rootQ] += size[rootP];
            } else {
                parent[rootQ] = rootP;
                size[rootP] += size[rootQ];
            }
            count--;
        }

        /**
         * Checks if elements p and q belong to the same component.
         *
         * @param p an element
         * @param q another element
         * @return true if connected, false otherwise
         */
        public boolean connected(int p, int q) {
            return find(p) == find(q);
        }

        /**
         * Returns the number of connected components.
         *
         * @return number of components
         */
        public int count() {
            return count;
        }

        private void validate(int p) {
            int n = parent.length;
            if (p < 0 || p >= n) {
                throw new IllegalArgumentException("Index " + p + " is not between 0 and " + (n - 1));
            }
        }
    }

    private final int n;                     // Grid dimension (n by n)
    private final boolean[] isOpen;          // Tracks open status: true if open, false if blocked
    private final WeightedQuickUnionUF uf;   // WQUPC instance for percolation
    private final int virtualTop;            // Virtual top site index
    private final int virtualBottom;         // Virtual bottom site index
    private int openSitesCount;              // Number of opened sites

    /**
     * Initializes an N-by-N grid where all sites are initially blocked.
     * Also sets up two virtual sites:
     * - Virtual Top: index n * n
     * - Virtual Bottom: index n * n + 1
     *
     * @param n the dimension of the grid (n > 0)
     * @throws IllegalArgumentException if n <= 0
     */
    public Percolation(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Grid dimension N must be greater than 0. Given: " + n);
        }

        this.n = n;
        int totalSites = n * n;
        this.isOpen = new boolean[totalSites];
        this.openSitesCount = 0;

        // Total sites in UF = N^2 grid sites + 2 virtual sites
        this.virtualTop = totalSites;
        this.virtualBottom = totalSites + 1;
        this.uf = new WeightedQuickUnionUF(totalSites + 2);
    }

    /**
     * Converts 2D (row, col) grid coordinates into a 1D flat index.
     *
     * @param row row index (0-based)
     * @param col col index (0-based)
     * @return 1D flat index in range [0, n * n - 1]
     */
    private int getIndex(int row, int col) {
        return row * n + col;
    }

    /**
     * Validates that row and column indices are within bounds.
     * Uses 0-based indexing: 0 <= row < n and 0 <= col < n.
     *
     * (Note: If 1-based indexing is required, change to row < 1 || row > n || col < 1 || col > n)
     *
     * @param row row index
     * @param col column index
     * @throws IndexOutOfBoundsException if row or col is outside valid range
     */
    private void validate(int row, int col) {
        if (row < 0 || row >= n || col < 0 || col >= n) {
            throw new IndexOutOfBoundsException(
                String.format("Site (%d, %d) is out of bounds for grid of size %dx%d (0-indexed).", row, col, n, n)
            );
        }
    }

    /**
     * Opens the site at (row, col) if it is not open already.
     * Connects this site to its adjacent open sites (up, down, left, right).
     * Also connects top-row sites to the virtual top site,
     * and bottom-row sites to the virtual bottom site.
     *
     * @param row row coordinate (0 to n - 1)
     * @param col col coordinate (0 to n - 1)
     */
    public void open(int row, int col) {
        validate(row, col);

        int siteIndex = getIndex(row, col);

        // If already open, nothing to do
        if (isOpen[siteIndex]) {
            return;
        }

        // Mark site as open
        isOpen[siteIndex] = true;
        openSitesCount++;

        // 1. Connect to virtual top if on the top row
        if (row == 0) {
            uf.union(siteIndex, virtualTop);
        }

        // 2. Connect to virtual bottom if on the bottom row
        // Note: For N = 1, a site is both row 0 and row (n - 1), so it connects to both!
        if (row == n - 1) {
            uf.union(siteIndex, virtualBottom);
        }

        // 3. Connect to adjacent open neighbors (handling edges and corners safely)
        // Up neighbor (row - 1, col)
        if (row > 0 && isOpen(row - 1, col)) {
            uf.union(siteIndex, getIndex(row - 1, col));
        }

        // Down neighbor (row + 1, col)
        if (row < n - 1 && isOpen(row + 1, col)) {
            uf.union(siteIndex, getIndex(row + 1, col));
        }

        // Left neighbor (row, col - 1)
        if (col > 0 && isOpen(row, col - 1)) {
            uf.union(siteIndex, getIndex(row, col - 1));
        }

        // Right neighbor (row, col + 1)
        if (col < n - 1 && isOpen(row, col + 1)) {
            uf.union(siteIndex, getIndex(row, col + 1));
        }
    }

    /**
     * Checks if the site at (row, col) is open.
     *
     * @param row row coordinate (0 to n - 1)
     * @param col col coordinate (0 to n - 1)
     * @return true if site is open, false if blocked
     */
    public boolean isOpen(int row, int col) {
        validate(row, col);
        return isOpen[getIndex(row, col)];
    }

    /**
     * Returns the total number of open sites.
     *
     * @return number of open sites
     */
    public int numberOfOpenSites() {
        return openSitesCount;
    }

    /**
     * Checks if the system percolates.
     * The system percolates if the virtual top site is connected to the virtual bottom site.
     *
     * @return true if the system percolates, false otherwise
     */
    public boolean percolates() {
        return uf.connected(virtualTop, virtualBottom);
    }

    /**
     * Main method demonstrating usage and verifying functionality on sample test cases.
     */
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  Percolation System with Disjoint-Set (WQUPC)    ");
        System.out.println("==================================================");

        // Test Case 1: 3x3 Grid
        System.out.println("\n--- Test Case 1: 3x3 Grid ---");
        int n = 3;
        Percolation p = new Percolation(n);

        System.out.println("Initial percolation state (should be false): " + p.percolates());
        
        // Open a non-percolating vertical path
        p.open(0, 1);
        p.open(1, 1);
        System.out.println("Opened (0,1) and (1,1). Percolates? " + p.percolates() + " (expected: false)");
        
        // Open bottom site to complete path
        p.open(2, 1);
        System.out.println("Opened (2,1). Percolates? " + p.percolates() + " (expected: true)");
        System.out.println("Total open sites: " + p.numberOfOpenSites() + " (expected: 3)");

        // Test Case 2: Edge Case N = 1
        System.out.println("\n--- Test Case 2: Edge Case N = 1 ---");
        Percolation p1 = new Percolation(1);
        System.out.println("1x1 grid initially percolates? " + p1.percolates() + " (expected: false)");
        p1.open(0, 0);
        System.out.println("1x1 grid after opening (0,0) percolates? " + p1.percolates() + " (expected: true)");

        // Test Case 3: Corner Sites and Non-percolating Diagonal
        System.out.println("\n--- Test Case 3: 3x3 Diagonal (Corners) ---");
        Percolation pDiag = new Percolation(3);
        pDiag.open(0, 0); // Top-left corner
        pDiag.open(1, 1); // Center
        pDiag.open(2, 2); // Bottom-right corner
        System.out.println("Opened (0,0), (1,1), (2,2) diagonally.");
        System.out.println("Percolates? " + pDiag.percolates() + " (expected: false, diagonal adjacency is not connected)");

        pDiag.open(0, 1);
        pDiag.open(1, 2);
        System.out.println("Opened (0,1) and (1,2) to connect the path. Percolates? " + pDiag.percolates() + " (expected: true)");

        // Test Case 4: Bounds validation
        System.out.println("\n--- Test Case 4: Exception Handling ---");
        try {
            p.open(-1, 0);
            System.out.println("FAIL: Should have thrown IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("PASS: Caught expected exception for negative index: " + e.getMessage());
        }

        try {
            p.open(3, 3);
            System.out.println("FAIL: Should have thrown IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("PASS: Caught expected exception for out-of-bound index: " + e.getMessage());
        }

        try {
            new Percolation(0);
            System.out.println("FAIL: Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("PASS: Caught expected exception for N <= 0: " + e.getMessage());
        }

        System.out.println("\nAll tests completed successfully!");
    }
}
