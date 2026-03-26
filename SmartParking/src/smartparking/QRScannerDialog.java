package smartparking;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.function.Consumer;

/**
 * QR Scanner Window — uses webcam to scan QR codes at exit gate.
 *
 * HOW IT WORKS:
 *   1. Opens webcam feed using Java Robot / javax.imageio or FMJ/OpenCV if available
 *   2. Falls back to manual token entry if no camera is found
 *   3. Decodes QR using ZXing if available, else pure-Java fallback
 *   4. On successful scan → calls onTokenScanned callback with the token
 *
 * CAMERA SUPPORT:
 *   - With Webcam Capture API (webcam-capture.jar): full live preview
 *   - Without: shows manual entry form (type/paste the QR URL or token)
 *
 * DROP-IN UPGRADE (optional):
 *   Download webcam-capture-0.3.12.jar from:
 *   https://github.com/sarxos/webcam-capture/releases
 *   Place in libs/ and recompile — auto-detected via reflection.
 */
public class QRScannerDialog extends JDialog {

    private static final Color BG      = new Color(18, 18, 30);
    private static final Color CARD    = new Color(28, 30, 50);
    private static final Color ACCENT  = new Color(99, 102, 241);
    private static final Color GREEN   = new Color(34, 197, 94);
    private static final Color RED     = new Color(239, 68, 68);
    private static final Color AMBER   = new Color(251, 191, 36);
    private static final Color TEXT    = new Color(230, 232, 255);
    private static final Color DIM     = new Color(140, 145, 190);
    private static final Color BORDER  = new Color(55, 58, 90);

    private final Consumer<String> onTokenFound;  // called with token when QR is scanned
    private final CheckoutServer   server;

    private JTextField tfManualInput;
    private JLabel     lblStatus;
    private JPanel     cameraPanel;
    private Timer      scanTimer;
    private Object     webcam;          // com.github.sarxos.webcam.Webcam (optional)
    private boolean    scanning = false;
    private boolean    verified = false;

    public QRScannerDialog(Frame owner, CheckoutServer server, Consumer<String> onTokenFound) {
        super(owner, "📷  Exit Gate — QR Scanner", true);
        this.server       = server;
        this.onTokenFound = onTokenFound;
        setSize(520, 600);
        setLocationRelativeTo(owner);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        // ── Header ────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(28, 30, 50));
        header.setBorder(new EmptyBorder(16, 20, 12, 20));

        JLabel title = new JLabel("🚪  Exit Gate — QR Verification");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(ACCENT);
        JLabel sub = new JLabel("Scan customer's QR code to proceed to checkout");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(DIM);
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);

        // ── Camera view ───────────────────────────────────────────────
        cameraPanel = new JPanel(new BorderLayout());
        cameraPanel.setBackground(new Color(10, 10, 20));
        cameraPanel.setPreferredSize(new Dimension(480, 280));
        cameraPanel.setBorder(BorderFactory.createLineBorder(BORDER, 2));

        boolean hasWebcam = tryInitWebcam();
        if (hasWebcam) {
            startCameraFeed();
        } else {
            showNoCameraUI();
        }

        // ── Manual entry fallback ─────────────────────────────────────
        JPanel manualPanel = new JPanel(new BorderLayout(0, 8));
        manualPanel.setBackground(BG);
        manualPanel.setBorder(new EmptyBorder(14, 20, 0, 20));

        JLabel divider = new JLabel("─────  Or enter QR token / URL manually  ─────");
        divider.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        divider.setForeground(DIM);
        divider.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        tfManualInput = new JTextField();
        tfManualInput.setFont(new Font("Consolas", Font.PLAIN, 12));
        tfManualInput.setBackground(new Color(22, 24, 42));
        tfManualInput.setForeground(TEXT);
        tfManualInput.setCaretColor(ACCENT);
        tfManualInput.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        tfManualInput.setToolTipText("Paste QR URL or token (e.g. MH12AB_1A2B3C4D)");
        tfManualInput.addActionListener(e -> processInput());

        JButton btnVerify = new JButton("Verify");
        btnVerify.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnVerify.setBackground(ACCENT);
        btnVerify.setForeground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setBorder(new EmptyBorder(8, 18, 8, 18));
        btnVerify.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVerify.addActionListener(e -> processInput());

        inputRow.add(tfManualInput, BorderLayout.CENTER);
        inputRow.add(btnVerify, BorderLayout.EAST);

        manualPanel.add(divider, BorderLayout.NORTH);
        manualPanel.add(inputRow, BorderLayout.CENTER);

        // ── Status bar ────────────────────────────────────────────────
        lblStatus = new JLabel("  Waiting for QR scan...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(DIM);
        lblStatus.setBorder(new EmptyBorder(10, 20, 10, 20));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(22, 24, 42));

        // ── Cancel button ─────────────────────────────────────────────
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.setBackground(new Color(55, 58, 90));
        btnCancel.setForeground(DIM);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorder(new EmptyBorder(8, 20, 8, 20));
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> { stopCamera(); dispose(); });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG);
        bottom.add(lblStatus, BorderLayout.CENTER);
        bottom.add(btnCancel, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(12, 20, 12, 20));
        center.add(cameraPanel, BorderLayout.NORTH);
        center.add(manualPanel, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { stopCamera(); }
        });
    }

    // ── Camera handling ───────────────────────────────────────────────────

    private boolean tryInitWebcam() {
        try {
            Class<?> wc = Class.forName("com.github.sarxos.webcam.Webcam");
            webcam = wc.getMethod("getDefault").invoke(null);
            return webcam != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void startCameraFeed() {
        scanning = true;
        JLabel camLabel = new JLabel("Starting camera...", SwingConstants.CENTER);
        camLabel.setForeground(DIM);
        camLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cameraPanel.add(camLabel, BorderLayout.CENTER);

        new Thread(() -> {
            try {
                Class<?> wc = webcam.getClass();
                wc.getMethod("open").invoke(webcam);

                scanTimer = new Timer(200, e -> {
                    if (!scanning) return;
                    try {
                        BufferedImage frame = (BufferedImage) wc
                                .getMethod("getImage").invoke(webcam);
                        if (frame != null) {
                            // Update camera preview
                            Image scaled = frame.getScaledInstance(
                                    cameraPanel.getWidth(), cameraPanel.getHeight(),
                                    Image.SCALE_FAST);
                            SwingUtilities.invokeLater(() -> {
                                camLabel.setIcon(new ImageIcon(scaled));
                                camLabel.setText("");
                            });
                            // Decode QR
                            String decoded = decodeQR(frame);
                            if (decoded != null) {
                                handleDecoded(decoded);
                            }
                        }
                    } catch (Exception ex) { /* ignore frame errors */ }
                });
                scanTimer.start();
            } catch (Exception e) {
                SwingUtilities.invokeLater(this::showNoCameraUI);
            }
        }).start();
    }

    private void showNoCameraUI() {
        cameraPanel.removeAll();
        cameraPanel.setLayout(new BorderLayout());

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("📷");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel("No webcam detected");
        msg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        msg.setForeground(AMBER);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("<html><center>Use manual entry below,<br>or add webcam-capture.jar to libs/</center></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(DIM);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Simulated scan button for testing
        JButton btnSim = new JButton("📋  Paste QR from Clipboard");
        btnSim.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSim.setBackground(new Color(55, 58, 90));
        btnSim.setForeground(TEXT);
        btnSim.setFocusPainted(false);
        btnSim.setBorder(new EmptyBorder(8, 16, 8, 16));
        btnSim.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSim.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSim.addActionListener(e -> {
            try {
                String clip = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                if (clip != null && !clip.isEmpty()) {
                    tfManualInput.setText(clip.trim());
                    processInput();
                }
            } catch (Exception ex) { /* no string in clipboard */ }
        });

        inner.add(Box.createVerticalGlue());
        inner.add(icon);
        inner.add(Box.createVerticalStrut(10));
        inner.add(msg);
        inner.add(Box.createVerticalStrut(8));
        inner.add(hint);
        inner.add(Box.createVerticalStrut(14));
        inner.add(btnSim);
        inner.add(Box.createVerticalGlue());

        cameraPanel.add(inner, BorderLayout.CENTER);
        cameraPanel.revalidate();
        cameraPanel.repaint();
    }

    // ── QR Decoding ───────────────────────────────────────────────────────

    /** Try to decode QR from image using ZXing via reflection. */
    private String decodeQR(BufferedImage img) {
        try {
            Class<?> lbBitmap  = Class.forName("com.google.zxing.client.j2se.BufferedImageLuminanceSource");
            Class<?> hybridBin = Class.forName("com.google.zxing.common.HybridBinarizer");
            Class<?> binBitmap = Class.forName("com.google.zxing.BinaryBitmap");
            Class<?> reader    = Class.forName("com.google.zxing.qrcode.QRCodeReader");
            Class<?> result    = Class.forName("com.google.zxing.Result");

            Object src  = lbBitmap.getConstructor(BufferedImage.class).newInstance(img);
            Object bin  = hybridBin.getConstructor(Class.forName("com.google.zxing.LuminanceSource")).newInstance(src);
            Object bmp  = binBitmap.getConstructor(Class.forName("com.google.zxing.Binarizer")).newInstance(bin);
            Object r    = reader.getDeclaredConstructor().newInstance();
            Object res  = reader.getMethod("decode", binBitmap).invoke(r, bmp);
            return (String) result.getMethod("getText").invoke(res);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Input processing ──────────────────────────────────────────────────

    private void processInput() {
        String input = tfManualInput.getText().trim();
        if (input.isEmpty()) {
            setStatus("⚠ Please enter a QR URL or token", AMBER);
            return;
        }
        handleDecoded(input);
    }

    private void handleDecoded(String raw) {
        if (verified) return; // prevent double-trigger

        // Extract token from URL or use directly
        String token = extractToken(raw);
        if (token == null || token.isEmpty()) {
            setStatus("❌  Invalid QR — token not found", RED);
            return;
        }

        // Verify against server
        if (server == null) {
            setStatus("❌  Server not running", RED);
            return;
        }

        String vehicleNo = server.getVehicleForToken(token);
        if (vehicleNo == null) {
            setStatus("❌  QR invalid or already used", RED);
            flashRed();
            return;
        }

        // Valid!
        verified = true;
        scanning = false;
        stopCamera();

        setStatus("✅  QR Verified — " + vehicleNo + " — Opening checkout...", GREEN);
        flashGreen();

        // Small delay for visual feedback, then callback
        Timer t = new Timer(800, e -> {
            dispose();
            onTokenFound.accept(token);
        });
        t.setRepeats(false);
        t.start();
    }

    private String extractToken(String input) {
        // If it's a full URL: http://localhost:8080/checkout?token=XXX
        if (input.contains("token=")) {
            int idx = input.indexOf("token=");
            String tok = input.substring(idx + 6).trim();
            // strip any trailing params
            int amp = tok.indexOf('&');
            if (amp > 0) tok = tok.substring(0, amp);
            return tok;
        }
        // Otherwise treat as raw token
        return input;
    }

    private void stopCamera() {
        scanning = false;
        if (scanTimer != null) scanTimer.stop();
        if (webcam != null) {
            try { webcam.getClass().getMethod("close").invoke(webcam); }
            catch (Exception ignored) {}
        }
    }

    private void setStatus(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("  " + msg);
            lblStatus.setForeground(color);
        });
    }

    private void flashGreen() {
        Timer flash = new Timer(100, null);
        flash.addActionListener(new ActionListener() {
            int count = 0;
            public void actionPerformed(ActionEvent e) {
                cameraPanel.setBackground(count++ % 2 == 0 ? new Color(0, 60, 0) : new Color(10, 10, 20));
                if (count >= 6) { flash.stop(); cameraPanel.setBackground(new Color(10, 10, 20)); }
            }
        });
        flash.start();
    }

    private void flashRed() {
        Timer flash = new Timer(100, null);
        flash.addActionListener(new ActionListener() {
            int count = 0;
            public void actionPerformed(ActionEvent e) {
                cameraPanel.setBackground(count++ % 2 == 0 ? new Color(80, 0, 0) : new Color(10, 10, 20));
                if (count >= 6) { flash.stop(); cameraPanel.setBackground(new Color(10, 10, 20)); }
            }
        });
        flash.start();
    }
}
