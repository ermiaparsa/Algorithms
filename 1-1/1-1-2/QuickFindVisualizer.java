import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class QuickFindVisualizer extends JPanel {

    // ---------------- QUICK-FIND ----------------
    public static class QuickFindUF {
        int[] id;

        public QuickFindUF(int n) {
            id = new int[n];
            for (int i = 0; i < n; i++) {
                id[i] = i;
            }
        }

        public boolean connected(int p, int q) {
            return id[p] == id[q];
        }

        public void union(int p, int q) {
            int pid = id[p];
            int qid = id[q];
            if (pid == qid) return;

            for (int i = 0; i < id.length; i++) {
                if (id[i] == pid) {
                    id[i] = qid;
                }
            }
        }

        public int componentId(int x) {
            return id[x];
        }

        public int[] getIdArray() {
            return id;
        }
    }

    // ---------------- RGB LED NEON PALETTE ----------------
    // Curated vibrant LED neon colors inspired by  & cyber night-mode aesthetics
    private static final Color[] LED_NEON_PALETTE = {
        new Color(0, 245, 212),   // Neon Mint / Cyan LED
        new Color(168, 85, 247),  // Radiant  Purple LED
        new Color(255, 64, 129),  // Electric Fuchsia / Pink LED
        new Color(59, 130, 246),  // Vivid Sky Blue LED
        new Color(255, 183, 3),   // Sunburst Gold LED
        new Color(16, 231, 126),  // Cyber Emerald LED
        new Color(255, 87, 34),   // Hot Coral / Flame LED
        new Color(124, 58, 237),  // Deep Violet LED
        new Color(6, 182, 212),   // Electric Aqua LED
        new Color(163, 230, 53),  // Neon Lime LED
        new Color(236, 72, 153),  // Hot Magenta LED
        new Color(250, 204, 21),  // Amber Glow LED
        new Color(99, 102, 241),  // Electric Indigo LED
        new Color(20, 241, 149),  // Cyber Spring LED
        new Color(244, 63, 94),   // Vivid Rose LED
        new Color(56, 189, 248)   // Azure Glow LED
    };

    // ---------------- FIELDS ----------------
    private final int n;
    private QuickFindUF uf;
    private QuickFindUF initialUF;

    private final List<int[]> allPairs;
    private final List<int[]> drawnEdges = new ArrayList<>();

    private int step = 0;
    private int[] activePair = null;
    private final Set<Integer> lastChangedNodes = new HashSet<>();

    private final Map<Integer, Color> componentColors = new HashMap<>();
    private final Random random = new Random();

    // UI Controls
    private JLabel statusLabel;
    private JPanel statusCard;
    private JPanel statusContainer;
    private GeminiButton nextButton;
    private GeminiButton autoButton;
    private GeminiButton resetButton;
    private javax.swing.Timer autoTimer;
    private Color statusIndicatorColor = new Color(0, 245, 212);

    // ---------------- CONSTRUCTOR ----------------
    public QuickFindVisualizer(int n, List<int[]> pairs) {
        this.n = n;
        this.allPairs = pairs;

        this.uf = new QuickFindUF(n);
        this.initialUF = new QuickFindUF(n);

        assignInitialColors();

        setPreferredSize(new Dimension(960, 720));
        setBackground(new Color(9, 11, 18));
        setLayout(new BorderLayout());

        buildControlPanel();

        // Auto-play timer
        autoTimer = new javax.swing.Timer(850, e -> {
            if (step < allPairs.size()) {
                doStep();
            } else {
                stopAutoPlay();
            }
        });
    }

    // ---------------- INITIAL COLORS ----------------
    private void assignInitialColors() {
        componentColors.clear();
        for (int i = 0; i < n; i++) {
            componentColors.put(i, getNeonColor(i));
        }
    }

    private Color getNeonColor(int id) {
        if (id >= 0 && id < LED_NEON_PALETTE.length) {
            return LED_NEON_PALETTE[id];
        }
        float hue = (id * 0.618033988749895f) % 1.0f;
        return Color.getHSBColor(hue, 0.90f, 1.0f);
    }

    private Color getNodeColor(int node) {
        int comp = uf.componentId(node);
        return componentColors.computeIfAbsent(comp, this::getNeonColor);
    }

    // ---------------- CONTROL PANEL SETUP ----------------
    private void buildControlPanel() {
        // Bottom control container (Dark Glassmorphism)
        JPanel controlPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dark background
                g2.setColor(new Color(10, 14, 24, 240));
                g2.fillRect(0, 0, w, h);

                // Top hairline glowing border with  gradient
                LinearGradientPaint sepGrad = new LinearGradientPaint(
                    0, 0, w, 0,
                    new float[]{0f, 0.33f, 0.66f, 1f},
                    new Color[]{
                        new Color(66, 133, 244, 200),
                        new Color(155, 81, 224, 220),
                        new Color(236, 72, 153, 220),
                        new Color(0, 242, 254, 200)
                    }
                );
                g2.setPaint(sepGrad);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, 0, w, 0);

                g2.dispose();
            }
        };
        controlPanel.setLayout(new BorderLayout());
        controlPanel.setOpaque(false);
        controlPanel.setPreferredSize(new Dimension(960, 105));

        // Buttons Bar
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 12));
        buttonPanel.setOpaque(false);

        nextButton = new GeminiButton("✦ Next Step", GeminiButton.Style.GEMINI_GRADIENT);
        autoButton = new GeminiButton("▶ Auto Play", GeminiButton.Style.NEON_CYAN);
        resetButton = new GeminiButton("↺ Reset", GeminiButton.Style.DARK_GLASS);

        nextButton.addActionListener(e -> doStep());
        autoButton.addActionListener(e -> toggleAutoPlay());
        resetButton.addActionListener(e -> reset());

        buttonPanel.add(nextButton);
        buttonPanel.add(autoButton);
        buttonPanel.add(resetButton);

        // Status Card Bar
        statusContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        statusContainer.setOpaque(false);

        statusLabel = new JLabel("✦ Ready! Step: 0 / " + allPairs.size() + " — Click Next Step or Auto Play");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setForeground(new Color(226, 232, 240));

        statusCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dark card container
                g2.setColor(new Color(15, 21, 35, 235));
                g2.fillRoundRect(0, 0, w, h, 16, 16);

                // LED border
                g2.setColor(new Color(statusIndicatorColor.getRed(), statusIndicatorColor.getGreen(), statusIndicatorColor.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

                // Glowing indicator dot
                int dotSize = 8;
                int dotX = 14;
                int dotY = (h - dotSize) / 2;

                // Glow halo
                g2.setColor(new Color(statusIndicatorColor.getRed(), statusIndicatorColor.getGreen(), statusIndicatorColor.getBlue(), 70));
                g2.fillOval(dotX - 3, dotY - 3, dotSize + 6, dotSize + 6);

                // Bright dot
                g2.setColor(statusIndicatorColor);
                g2.fillOval(dotX, dotY, dotSize, dotSize);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 5));
        statusCard.setOpaque(false);
        statusCard.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 16));
        statusCard.add(statusLabel);
        statusContainer.add(statusCard);

        controlPanel.add(buttonPanel, BorderLayout.NORTH);
        controlPanel.add(statusContainer, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void updateStatus(String text, Color indicatorColor) {
        this.statusIndicatorColor = indicatorColor;
        this.statusLabel.setText(text);
        if (statusCard != null) {
            statusCard.revalidate();
        }
        if (statusContainer != null) {
            statusContainer.revalidate();
            statusContainer.repaint();
        }
    }

    // ---------------- STEP LOGIC ----------------
    private void doStep() {
        if (step >= allPairs.size()) {
            stopAutoPlay();
            updateStatus("✦ Complete! All " + allPairs.size() + " union operations processed.", new Color(168, 85, 247));
            repaint();
            return;
        }

        int[] pair = allPairs.get(step);
        int p = pair[0];
        int q = pair[1];
        activePair = pair;
        lastChangedNodes.clear();

        boolean alreadyConnected = uf.connected(p, q);

        if (!alreadyConnected) {
            int oldPid = uf.componentId(p);
            int newPid = uf.componentId(q);

            // Record nodes whose id[] will change
            for (int i = 0; i < n; i++) {
                if (uf.componentId(i) == oldPid) {
                    lastChangedNodes.add(i);
                }
            }

            uf.union(p, q);
            drawnEdges.add(pair);

            // Assign destination component color to merged nodes
            Color mergedColor = componentColors.get(newPid);
            for (int i = 0; i < n; i++) {
                if (uf.componentId(i) == newPid) {
                    componentColors.put(uf.componentId(i), mergedColor);
                }
            }
            componentColors.put(newPid, mergedColor);
            componentColors.remove(oldPid);

            updateStatus("✦ Step " + (step + 1) + "/" + allPairs.size()
                    + " : union(" + p + ", " + q + ") applied — merged component #" + oldPid + " into #" + newPid,
                    new Color(0, 245, 212)); // Glowing Cyan
        } else {
            updateStatus("✦ Step " + (step + 1) + "/" + allPairs.size()
                    + " : (" + p + ", " + q + ") are already connected in component #" + uf.componentId(p),
                    new Color(255, 183, 3)); // Amber Alert
        }

        step++;
        repaint();

        if (step >= allPairs.size()) {
            stopAutoPlay();
        }
    }

    private void toggleAutoPlay() {
        if (autoTimer.isRunning()) {
            stopAutoPlay();
        } else {
            if (step >= allPairs.size()) {
                reset();
            }
            autoButton.setText("❚❚ Pause");
            autoTimer.start();
        }
    }

    private void stopAutoPlay() {
        autoTimer.stop();
        autoButton.setText("▶ Auto Play");
    }

    private void reset() {
        stopAutoPlay();
        this.uf = new QuickFindUF(n);
        this.initialUF = new QuickFindUF(n);
        drawnEdges.clear();
        step = 0;
        activePair = null;
        lastChangedNodes.clear();
        assignInitialColors();
        updateStatus("✦ Reset complete. Step: 0 / " + allPairs.size(), new Color(0, 245, 212));
        repaint();
    }

    private int countUFComponents() {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < n; i++) {
            set.add(uf.componentId(i));
        }
        return set.size();
    }

    // ---------------- DYNAMIC CIRCULAR LAYOUT ----------------
    private Point[] calculatePositions(int count, int width, int height) {
        Point[] pts = new Point[count];
        int topOffset = 185;
        int bottomOffset = 120;
        int availHeight = Math.max(180, height - topOffset - bottomOffset);
        int centerX = width / 2;
        int centerY = topOffset + availHeight / 2;
        int radius = Math.max(90, Math.min(width / 2 - 80, availHeight / 2 - 30));

        double angleStep = 2 * Math.PI / count;
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2 + i * angleStep;
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY + (int) (radius * Math.sin(angle));
            pts[i] = new Point(x, y);
        }
        return pts;
    }

    // ---------------- PAINTING ----------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Deep Space Night-Mode Background
        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(9, 11, 18), w, h, new Color(13, 17, 28));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, w, h);

        // 2. Cosmic Ambient Nebulae ( website aesthetic)
        RadialGradientPaint nebula1 = new RadialGradientPaint(
            w * 0.18f, h * 0.28f, 420f,
            new float[]{0f, 0.55f, 1f},
            new Color[]{new Color(66, 133, 244, 42), new Color(66, 133, 244, 12), new Color(9, 11, 18, 0)}
        );
        g2.setPaint(nebula1);
        g2.fillRect(0, 0, w, h);

        RadialGradientPaint nebula2 = new RadialGradientPaint(
            w * 0.82f, h * 0.65f, 440f,
            new float[]{0f, 0.55f, 1f},
            new Color[]{new Color(217, 70, 239, 36), new Color(155, 81, 224, 10), new Color(13, 17, 28, 0)}
        );
        g2.setPaint(nebula2);
        g2.fillRect(0, 0, w, h);

        RadialGradientPaint nebula3 = new RadialGradientPaint(
            w * 0.50f, h * 0.50f, 320f,
            new float[]{0f, 1f},
            new Color[]{new Color(0, 242, 254, 18), new Color(9, 11, 18, 0)}
        );
        g2.setPaint(nebula3);
        g2.fillRect(0, 0, w, h);

        // 3. Subtle Cyber Matrix Grid
        g2.setColor(new Color(255, 255, 255, 5));
        g2.setStroke(new BasicStroke(1f));
        for (int x = 0; x < w; x += 40) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);

        // 4. Header:  Gradient Title
        String title = "✦ QUICK-FIND VISUALIZER ✦";
        Font titleFont = new Font("SansSerif", Font.BOLD, 22);
        g2.setFont(titleFont);
        FontMetrics fm = g2.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int titleX = (w - titleWidth) / 2;
        int titleY = 38;

        LinearGradientPaint geminiGrad = new LinearGradientPaint(
            titleX, 0, titleX + titleWidth, 0,
            new float[]{0.0f, 0.33f, 0.66f, 1.0f},
            new Color[]{
                new Color(66, 133, 244), //  Blue
                new Color(155, 81, 224), // Vibrant Purple
                new Color(236, 72, 153), // Hot Magenta
                new Color(0, 242, 254)   // Electric Cyan
            }
        );
        g2.setPaint(geminiGrad);
        g2.drawString(title, titleX, titleY);

        // Header Stat Badges
        int badgeY = 56;
        drawPillBadge(g2, w / 2 - 230, badgeY, "Nodes: " + n, new Color(0, 245, 212));
        drawPillBadge(g2, w / 2 - 65, badgeY, "Components: " + countUFComponents(), new Color(168, 85, 247));
        drawPillBadge(g2, w / 2 + 100, badgeY, "Operations: " + step + " / " + allPairs.size(), new Color(255, 64, 129));

        // 5. Quick-Find Data Structure HUD: id[] Array
        drawIdArrayHUD(g2, w);

        // 6. Calculate Dynamic Positions for Nodes
        Point[] positions = calculatePositions(n, w, h);

        // 7. Draw Edges with RGB LED Neon Glow Bloom
        for (int[] edge : drawnEdges) {
            Point p1 = positions[edge[0]];
            Point p2 = positions[edge[1]];
            Color c1 = getNodeColor(edge[0]);
            Color c2 = getNodeColor(edge[1]);

            LinearGradientPaint lineGrad = new LinearGradientPaint(
                p1.x, p1.y, p2.x, p2.y,
                new float[]{0f, 1f},
                new Color[]{c1, c2}
            );

            // Layer 1: Outer LED Bloom
            g2.setPaint(lineGrad);
            g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 35));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);

            // Layer 2: Medium Glow
            g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 120));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);

            // Layer 3: Laser Core Beam
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.WHITE);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // 8. Draw Nodes
        int r = 24;
        for (int i = 0; i < n; i++) {
            Point p = positions[i];
            Color nodeColor = getNodeColor(i);
            boolean isActiveNode = (activePair != null && (i == activePair[0] || i == activePair[1]));

            // Active pair extra outer pulse halo
            if (isActiveNode) {
                RadialGradientPaint activePulse = new RadialGradientPaint(
                    p.x, p.y, r + 22,
                    new float[]{0.3f, 1.0f},
                    new Color[]{new Color(0, 242, 254, 110), new Color(236, 72, 153, 0)}
                );
                g2.setPaint(activePulse);
                g2.fillOval(p.x - r - 22, p.y - r - 22, 2 * (r + 22), 2 * (r + 22));

                g2.setStroke(new BasicStroke(1.8f));
                g2.setColor(new Color(0, 242, 254, 200));
                g2.drawOval(p.x - r - 6, p.y - r - 6, 2 * (r + 6), 2 * (r + 6));
            }

            // Normal LED Glow Aura
            RadialGradientPaint halo = new RadialGradientPaint(
                p.x, p.y, r + 14,
                new float[]{0.4f, 1.0f},
                new Color[]{
                    new Color(nodeColor.getRed(), nodeColor.getGreen(), nodeColor.getBlue(), 85),
                    new Color(nodeColor.getRed(), nodeColor.getGreen(), nodeColor.getBlue(), 0)
                }
            );
            g2.setPaint(halo);
            g2.fillOval(p.x - r - 14, p.y - r - 14, 2 * (r + 14), 2 * (r + 14));

            // Node Core: Sleek dark glass disc
            GradientPaint nodeGrad = new GradientPaint(
                p.x - r, p.y - r, new Color(26, 34, 54),
                p.x + r, p.y + r, new Color(11, 15, 26)
            );
            g2.setPaint(nodeGrad);
            g2.fillOval(p.x - r, p.y - r, 2 * r, 2 * r);

            // Glowing LED Ring Border
            g2.setStroke(new BasicStroke(2.8f));
            g2.setColor(nodeColor);
            g2.drawOval(p.x - r, p.y - r, 2 * r, 2 * r);

            // Crisp White Node Label
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            String text = String.valueOf(i);
            FontMetrics fmNode = g2.getFontMetrics();
            int tx = p.x - fmNode.stringWidth(text) / 2;
            int ty = p.y + (fmNode.getAscent() - fmNode.getDescent()) / 2;

            g2.setColor(Color.WHITE);
            g2.drawString(text, tx, ty);
        }
    }

    // ---------------- ID[] ARRAY HUD DRAWING ----------------
    private void drawIdArrayHUD(Graphics2D g2, int width) {
        int[] idArray = uf.getIdArray();
        int boxW = Math.max(28, Math.min(46, (width - 120) / n - 6));
        int boxH = 46;
        int totalWidth = n * boxW + (n - 1) * 6;
        int startX = (width - totalWidth) / 2;
        int startY = 100;

        // HUD Section Header
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        String hudTitle = "QUICK-FIND STATE: id[] ARRAY";
        int hudTitleW = g2.getFontMetrics().stringWidth(hudTitle);
        g2.setColor(new Color(148, 163, 184));
        g2.drawString(hudTitle, (width - hudTitleW) / 2, startY - 6);

        for (int i = 0; i < n; i++) {
            int cx = startX + i * (boxW + 6);
            int compId = idArray[i];
            Color compCol = getNeonColor(compId);
            boolean isModified = lastChangedNodes.contains(i);

            // Index label i=X
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.setColor(new Color(148, 163, 184));
            String idxStr = "i=" + i;
            int idxW = g2.getFontMetrics().stringWidth(idxStr);
            g2.drawString(idxStr, cx + (boxW - idxW) / 2, startY + 11);

            // Cell background
            RoundRectangle2D.Float card = new RoundRectangle2D.Float(cx, startY + 15, boxW, boxH - 15, 8, 8);
            if (isModified) {
                // Highlight changed cells with glowing neon aura
                g2.setColor(new Color(compCol.getRed(), compCol.getGreen(), compCol.getBlue(), 70));
                g2.fill(card);
            } else {
                g2.setColor(new Color(15, 22, 38, 230));
                g2.fill(card);
            }

            // Cell LED Border
            if (isModified) {
                g2.setColor(compCol);
                g2.setStroke(new BasicStroke(2.2f));
            } else {
                g2.setColor(new Color(compCol.getRed(), compCol.getGreen(), compCol.getBlue(), 160));
                g2.setStroke(new BasicStroke(1.4f));
            }
            g2.draw(card);

            // Cell Value: id[i]
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            String valStr = String.valueOf(compId);
            int valW = g2.getFontMetrics().stringWidth(valStr);
            g2.setColor(compCol);
            g2.drawString(valStr, cx + (boxW - valW) / 2, startY + 36);
        }
    }

    private static void drawPillBadge(Graphics2D g2, int x, int y, String text, Color accent) {
        Font font = new Font("SansSerif", Font.BOLD, 12);
        g2.setFont(font);
        int w = g2.getFontMetrics().stringWidth(text) + 26;
        int h = 24;

        g2.setColor(new Color(18, 24, 39, 220));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x, y, w, h, 12, 12);

        // Accent indicator dot
        g2.setColor(accent);
        g2.fillOval(x + 8, y + 8, 7, 7);

        g2.setColor(new Color(241, 245, 249));
        g2.drawString(text, x + 20, y + 16);
    }

    // ---------------- CUSTOM  BUTTON ----------------
    public static class GeminiButton extends JButton {
        public enum Style {
            GEMINI_GRADIENT,
            NEON_CYAN,
            DARK_GLASS
        }

        private final Style style;
        private boolean isHovered = false;
        private boolean isPressed = false;

        public GeminiButton(String text, Style style) {
            super(text);
            this.style = style;
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(145, 40));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 20;

            if (style == Style.GEMINI_GRADIENT) {
                //  Brand Multi-Stop LED Gradient
                Color c1 = isHovered ? new Color(59, 130, 246) : new Color(37, 99, 235);
                Color c2 = isHovered ? new Color(168, 85, 247) : new Color(147, 51, 234);
                Color c3 = isHovered ? new Color(244, 63, 94) : new Color(219, 39, 119);

                LinearGradientPaint grad = new LinearGradientPaint(
                    0, 0, w, h,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{c1, c2, c3}
                );
                g2.setPaint(grad);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // Hover LED Bloom glow
                if (isHovered) {
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                }
                g2.setColor(Color.WHITE);

            } else if (style == Style.NEON_CYAN) {
                // Neon Cyan / Indigo Cyber Button
                Color bg = isHovered ? new Color(24, 38, 62) : new Color(17, 26, 44);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                Color borderCol = isHovered ? new Color(0, 245, 212) : new Color(6, 182, 212);
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.setColor(isHovered ? new Color(0, 245, 212) : new Color(226, 232, 240));

            } else { // DARK_GLASS
                Color bg = isHovered ? new Color(28, 38, 58) : new Color(20, 27, 43);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                Color borderCol = isHovered ? new Color(168, 85, 247) : new Color(79, 70, 229);
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.setColor(new Color(241, 245, 249));
            }

            // Draw button text
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), tx, ty);

            g2.dispose();
        }
    }

    // ---------------- INPUT DIALOGS & NIGHT-MODE UIMANAGER ----------------
    private static void setupDarkTheme() {
        UIManager.put("OptionPane.background", new Color(15, 19, 32));
        UIManager.put("Panel.background", new Color(15, 19, 32));
        UIManager.put("OptionPane.messageForeground", new Color(241, 245, 249));
        UIManager.put("TextField.background", new Color(24, 31, 51));
        UIManager.put("TextField.foreground", new Color(0, 245, 212));
        UIManager.put("TextField.caretColor", new Color(0, 245, 212));
        UIManager.put("TextField.selectionBackground", new Color(66, 133, 244));
        UIManager.put("Button.background", new Color(30, 41, 69));
        UIManager.put("Button.foreground", new Color(241, 245, 249));
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
    }

    private static int readPositiveInt(String message, int defaultValue) {
        while (true) {
            String input = (String) JOptionPane.showInputDialog(
                null,
                message,
                "Quick-Find Configuration",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                String.valueOf(defaultValue)
            );
            if (input == null) System.exit(0);

            try {
                int value = Integer.parseInt(input.trim());
                if (value > 0) return value;
            } catch (NumberFormatException ignored) {
            }

            JOptionPane.showMessageDialog(
                null,
                "Please enter a valid positive integer.",
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static int[] readPair(int index, int n) {
        // Classic Sedgewick Quick-Find test pairs for smooth testing
        String[] defaultExamples = {"4 3", "3 8", "6 5", "9 4", "2 1", "8 9", "5 0", "7 2", "6 1", "1 0"};
        String defVal = (index < defaultExamples.length && n >= 10) ? defaultExamples[index] : (index % n) + " " + ((index + 1) % n);

        while (true) {
            String input = (String) JOptionPane.showInputDialog(
                null,
                "Enter union pair #" + (index + 1) + " (format: p q)\nNodes must be between 0 and " + (n - 1) + ":",
                "Union Operation #" + (index + 1),
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                defVal
            );

            if (input == null) System.exit(0);

            String[] parts = input.trim().split("\\s+");
            if (parts.length == 2) {
                try {
                    int p = Integer.parseInt(parts[0]);
                    int q = Integer.parseInt(parts[1]);

                    if (p >= 0 && p < n && q >= 0 && q < n) {
                        return new int[]{p, q};
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            JOptionPane.showMessageDialog(
                null,
                "Invalid pair format. Please enter two space-separated integers between 0 and " + (n - 1) + ".\nExample: 3 7",
                "Input Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ---------------- MAIN ENTRY POINT ----------------
    public static void main(String[] args) {
        setupDarkTheme();

        SwingUtilities.invokeLater(() -> {
            int n = readPositiveInt("Enter number of nodes (default: 10):", 10);
            int m = readPositiveInt("Enter number of union operations (default: 7):", 7);

            List<int[]> pairs = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                pairs.add(readPair(i, n));
            }

            JFrame frame = new JFrame("Quick-Find Visualizer — Night Mode RGB LED");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(9, 11, 18));
            frame.add(new QuickFindVisualizer(n, pairs));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
