import java.util.Scanner;

public class Main {

    // --- Disjoint Set (Union-Find) Data Structure ---
    public static class UF {
        private int[] parent; // parent[i] = parent of i
        private int[] size;   // size[i] = number of elements in subtree rooted at i
        private int count;    // number of components

        public UF(int n) {
            count = n;
            parent = new int[n];
            size = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i; // initially each element is its own root
                size[i] = 1;
            }
        }

        // Returns the canonical root of the component containing p (with path compression)
        public int find(int p) {
            validate(p);
            while (p != parent[p]) {
                parent[p] = parent[parent[p]]; // path compression by halving
                p = parent[p];
            }
            return p;
        }

        // Returns true if p and q are in the same component
        public boolean connected(int p, int q) {
            return find(p) == find(q);
        }

        // Merges the component containing p with the component containing q
        public void union(int p, int q) {
            int rootP = find(p);
            int rootQ = find(q);
            if (rootP == rootQ) return;

            // Make smaller tree point to root of larger tree (Weighted union)
            if (size[rootP] < size[rootQ]) {
                parent[rootP] = rootQ;
                size[rootQ] += size[rootP];
            } else {
                parent[rootQ] = rootP;
                size[rootP] += size[rootQ];
            }
            count--;
        }

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

    // --- Client / Driver Code ---
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        if (!scanner.hasNextInt()) return;
        int n = scanner.nextInt();
        UF uf = new UF(n);

        System.out.println("--- Active Unions (Non-redundant pairs) ---");
        while (scanner.hasNextInt()) {
            int p = scanner.nextInt();
            int q = scanner.nextInt();

            if (!uf.connected(p, q)) {
                uf.union(p, q);
                System.out.println(p + " " + q);
            }
        }

        System.out.println("-------------------------------------------");
        System.out.println("Remaining components: " + uf.count());
        scanner.close();
    }
}
