package smartparking;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import javax.imageio.ImageIO;

public class QRDialog extends JDialog {

    private static final Color BG       = new Color(18, 18, 30);
    private static final Color CARD     = new Color(28, 30, 50);
    private static final Color ACCENT   = new Color(124, 58, 237);
    private static final Color GREEN    = new Color(34, 197, 94);
    private static final Color WA       = new Color(37, 211, 102);
    private static final Color TEXT     = new Color(230, 232, 255);
    private static final Color TEXT_DIM = new Color(140, 145, 190);
    private static final Color BORDER   = new Color(55, 58, 90);

    public QRDialog(Frame owner, String vehicleNo, int slotNo, String vehicleType,
                    byte[] qrPng, String checkoutUrl, CheckoutServer server) {
        super(owner, "Parking Ticket — " + vehicleNo, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 660);
        setLocationRelativeTo(owner);
        setResizable(false);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(buildContent(vehicleNo, slotNo, vehicleType, qrPng, checkoutUrl));
    }

    private JPanel buildContent(String vehicleNo, int slotNo, String vehicleType,
                                 byte[] qrPng, String checkoutUrl) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        // ── Header ──────────────────────────────────────────────────────
        JLabel titleLbl = new JLabel("Vehicle Parked Successfully!");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(GREEN);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel(vehicleNo + "   |   Slot #" + slotNo + "   |   " + vehicleType);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLbl.setForeground(TEXT_DIM);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── QR Code Panel ────────────────────────────────────────────────
        // Paint QR directly onto a custom JPanel for guaranteed rendering
        JPanel qrHolder = new JPanel(new BorderLayout());
        qrHolder.setBackground(Color.WHITE);
        qrHolder.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER, 2),
                new EmptyBorder(10, 10, 10, 10)));
        qrHolder.setMaximumSize(new Dimension(300, 300));
        qrHolder.setPreferredSize(new Dimension(280, 280));
        qrHolder.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (qrPng != null && qrPng.length > 0) {
            try {
                BufferedImage srcImg = ImageIO.read(new ByteArrayInputStream(qrPng));
                if (srcImg != null) {
                    // Scale up cleanly (nearest-neighbour for QR sharpness)
                    int dispSize = 260;
                    BufferedImage scaled = new BufferedImage(dispSize, dispSize, BufferedImage.TYPE_INT_RGB);
                    Graphics2D sg = scaled.createGraphics();
                    sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    sg.drawImage(srcImg, 0, 0, dispSize, dispSize, null);
                    sg.dispose();

                    JLabel qrLabel = new JLabel(new ImageIcon(scaled));
                    qrLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    qrHolder.add(qrLabel, BorderLayout.CENTER);
                } else {
                    qrHolder.add(errorLabel("Could not decode QR image"), BorderLayout.CENTER);
                }
            } catch (Exception e) {
                qrHolder.add(errorLabel("QR render error: " + e.getMessage()), BorderLayout.CENTER);
            }
        } else {
            qrHolder.add(errorLabel("QR generation failed — check console"), BorderLayout.CENTER);
        }

        JPanel qrWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        qrWrap.setOpaque(false);
        qrWrap.add(qrHolder);

        JLabel scanHint = new JLabel("Customer scans this QR code at exit to self-checkout");
        scanHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        scanHint.setForeground(TEXT_DIM);
        scanHint.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── URL Row ───────────────────────────────────────────────────────
        JPanel urlRow = new JPanel(new BorderLayout(8, 0));
        urlRow.setBackground(new Color(20, 22, 40));
        urlRow.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(7, 10, 7, 10)));
        urlRow.setMaximumSize(new Dimension(9999, 40));
        String displayUrl = checkoutUrl.length() > 52
                ? checkoutUrl.substring(0, 52) + "..." : checkoutUrl;
        JLabel urlLbl = new JLabel(displayUrl);
        urlLbl.setFont(new Font("Consolas", Font.PLAIN, 11));
        urlLbl.setForeground(new Color(167, 139, 250));
        JButton copyBtn = smallBtn("Copy", new Color(55, 58, 90));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                   .setContents(new StringSelection(checkoutUrl), null);
            copyBtn.setText("Copied!");
            Timer t = new Timer(1500, ev -> copyBtn.setText("Copy"));
            t.setRepeats(false); t.start();
        });
        urlRow.add(urlLbl, BorderLayout.CENTER);
        urlRow.add(copyBtn, BorderLayout.EAST);

        // ── Action Buttons ────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.setMaximumSize(new Dimension(9999, 48));

        JButton btnWA = bigBtn("Send via WhatsApp", WA);
        btnWA.addActionListener(e -> {
            String phone = JOptionPane.showInputDialog(this,
                    "Enter customer WhatsApp number:\n(e.g. +919876543210)",
                    "Customer Phone", JOptionPane.QUESTION_MESSAGE);
            if (phone == null || phone.trim().isEmpty()) return;
            MessagingService.MessageResult r =
                    MessagingService.sendParkingConfirmation(phone.trim(), vehicleNo, slotNo, checkoutUrl);
            if (r.whatsappLink != null) {
                try { Desktop.getDesktop().browse(new URI(r.whatsappLink)); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Open this link:\n" + r.whatsappLink, "WhatsApp Link",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
            if (r.success) JOptionPane.showMessageDialog(this, r.message, "Sent!", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnBrowser = bigBtn("Open in Browser", ACCENT);
        btnBrowser.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI(checkoutUrl)); }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Open this URL:\n" + checkoutUrl, "Checkout URL",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        btnPanel.add(btnWA);
        btnPanel.add(btnBrowser);

        JButton closeBtn = new JButton("Done");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeBtn.setForeground(TEXT_DIM);
        closeBtn.setBackground(CARD);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> dispose());

        // ── Assemble ──────────────────────────────────────────────────────
        root.add(titleLbl);
        root.add(Box.createVerticalStrut(4));
        root.add(subLbl);
        root.add(Box.createVerticalStrut(16));
        root.add(qrWrap);
        root.add(Box.createVerticalStrut(8));
        root.add(scanHint);
        root.add(Box.createVerticalStrut(14));
        root.add(urlRow);
        root.add(Box.createVerticalStrut(12));
        root.add(btnPanel);
        root.add(Box.createVerticalStrut(8));
        root.add(closeBtn);

        return root;
    }

    private JLabel errorLabel(String msg) {
        JLabel l = new JLabel("<html><center><font color='red'>⚠</font><br>" + msg + "</center></html>");
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private JButton bigBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(12, 10, 12, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton smallBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setBackground(bg); b.setForeground(new Color(230, 232, 255));
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(4, 10, 4, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
