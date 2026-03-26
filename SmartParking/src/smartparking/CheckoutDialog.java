package smartparking;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CheckoutDialog — opened ONLY after QR scan verification.
 * Shows full vehicle/fee details and confirms checkout.
 */
public class CheckoutDialog extends JDialog {

    private static final Color BG      = new Color(18, 18, 30);
    private static final Color CARD    = new Color(28, 30, 50);
    private static final Color ACCENT  = new Color(99, 102, 241);
    private static final Color GREEN   = new Color(34, 197, 94);
    private static final Color RED     = new Color(239, 68, 68);
    private static final Color AMBER   = new Color(251, 191, 36);
    private static final Color TEXT    = new Color(230, 232, 255);
    private static final Color DIM     = new Color(140, 145, 190);
    private static final Color BORDER  = new Color(55, 58, 90);

    private final ParkingManager manager;
    private final CheckoutServer server;
    private final String token;
    private final String vehicleNo;
    private final Runnable onCheckoutDone;

    private JLabel lblFee;
    private Timer  feeRefreshTimer;

    public CheckoutDialog(Frame owner, ParkingManager manager, CheckoutServer server,
                          String token, Runnable onCheckoutDone) {
        super(owner, "💳  Checkout — QR Verified", true);
        this.manager        = manager;
        this.server         = server;
        this.token          = token;
        this.vehicleNo      = server.getVehicleForToken(token);
        this.onCheckoutDone = onCheckoutDone;

        setSize(480, 580);
        setLocationRelativeTo(owner);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        if (vehicleNo == null) {
            add(errorPanel("Token invalid or already used."), BorderLayout.CENTER);
        } else {
            build();
        }
    }

    private void build() {
        Vehicle v = manager.getParkedVehicle(vehicleNo);
        if (v == null) {
            add(errorPanel("Vehicle already checked out."), BorderLayout.CENTER);
            return;
        }

        int slotNo = 0;
        for (ParkingSlot s : manager.getAllSlots())
            if (s.isOccupied() && s.getParkedVehicle().getVehicleNo().equalsIgnoreCase(vehicleNo))
                slotNo = s.getSlotNumber();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");
        long mins    = Duration.between(v.getEntryTime(), LocalDateTime.now()).toMinutes();
        long hrs     = Math.max(1, (long)Math.ceil(mins / 60.0));
        double rate  = v.getVehicleType().equals("Car") ? 30.0 : 15.0;
        double fee   = hrs * rate;
        String icon  = v.getVehicleType().equals("Car") ? "🚗" : "🏍";

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // ── Verified badge ─────────────────────────────────────────────
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER));
        badge.setOpaque(false);
        JLabel verifiedLbl = new JLabel("  ✅  QR VERIFIED — GATE CLEARED  ");
        verifiedLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        verifiedLbl.setForeground(GREEN);
        verifiedLbl.setOpaque(true);
        verifiedLbl.setBackground(new Color(0, 60, 0));
        verifiedLbl.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(GREEN, 1),
                new EmptyBorder(5, 12, 5, 12)));
        badge.add(verifiedLbl);

        // ── Vehicle header ─────────────────────────────────────────────
        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel vnoLbl = new JLabel(vehicleNo);
        vnoLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        vnoLbl.setForeground(TEXT);
        vnoLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel typeLbl = new JLabel(v.getVehicleType() + "  ·  Slot #" + slotNo);
        typeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        typeLbl.setForeground(DIM);
        typeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Info grid ─────────────────────────────────────────────────
        JPanel grid = new JPanel(new GridLayout(3, 2, 10, 8));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(9999, 120));
        grid.setBorder(new EmptyBorder(10, 0, 10, 0));

        grid.add(infoBox("Entry Time",    v.getEntryTime().format(fmt)));
        grid.add(infoBox("Exit Time",     LocalDateTime.now().format(fmt)));
        grid.add(infoBox("Duration",      hrs + " hr" + (hrs>1?"s":"") + " (" + mins + " mins)"));
        grid.add(infoBox("Rate",          "₹" + (int)rate + " / hr"));
        grid.add(infoBox("Slot",          "#" + slotNo));
        grid.add(infoBox("Vehicle Type",  v.getVehicleType()));

        // ── Fee display ────────────────────────────────────────────────
        JPanel feeBox = new JPanel(new BorderLayout());
        feeBox.setBackground(new Color(20, 15, 50));
        feeBox.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(124, 58, 237), 2),
                new EmptyBorder(16, 20, 16, 20)));
        feeBox.setMaximumSize(new Dimension(9999, 100));

        JLabel feeTitleLbl = new JLabel("Total Parking Fee");
        feeTitleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        feeTitleLbl.setForeground(DIM);

        lblFee = new JLabel("₹" + String.format("%.2f", fee));
        lblFee.setFont(new Font("Segoe UI", Font.BOLD, 38));
        lblFee.setForeground(new Color(167, 139, 250));

        JLabel calcLbl = new JLabel(hrs + " hrs × ₹" + (int)rate + "/hr");
        calcLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        calcLbl.setForeground(DIM);

        JPanel feeText = new JPanel();
        feeText.setOpaque(false);
        feeText.setLayout(new BoxLayout(feeText, BoxLayout.Y_AXIS));
        feeText.add(feeTitleLbl);
        feeText.add(lblFee);
        feeText.add(calcLbl);
        feeBox.add(feeText, BorderLayout.CENTER);

        // ── Payment method ─────────────────────────────────────────────
        JPanel payRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        payRow.setOpaque(false);
        payRow.setMaximumSize(new Dimension(9999, 40));

        String[] methods = {"💵 Cash", "💳 Card", "📱 UPI"};
        ButtonGroup bg = new ButtonGroup();
        JRadioButton[] radios = new JRadioButton[3];
        radios[0] = payRadio(methods[0], true);
        for (int i = 0; i < 3; i++) {
            radios[i] = payRadio(methods[i], i == 0);
            bg.add(radios[i]);
            payRow.add(radios[i]);
        }

        // ── Confirm button ─────────────────────────────────────────────
        JButton btnConfirm = new JButton("✅  Confirm Payment & Release Vehicle");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConfirm.setBackground(new Color(34, 197, 94));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setBorder(new EmptyBorder(14, 20, 14, 20));
        btnConfirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConfirm.setMaximumSize(new Dimension(9999, 52));
        btnConfirm.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConfirm.addActionListener(e -> confirmCheckout(fee));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.setBackground(BG);
        btnCancel.setForeground(DIM);
        btnCancel.setBorder(new EmptyBorder(6, 0, 0, 0));
        btnCancel.setContentAreaFilled(false);
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancel.addActionListener(e -> { stopRefresh(); dispose(); });

        // ── Assemble ──────────────────────────────────────────────────
        root.add(badge);
        root.add(Box.createVerticalStrut(10));
        root.add(iconLbl);
        root.add(Box.createVerticalStrut(4));
        root.add(vnoLbl);
        root.add(Box.createVerticalStrut(2));
        root.add(typeLbl);
        root.add(Box.createVerticalStrut(10));
        root.add(grid);
        root.add(feeBox);
        root.add(Box.createVerticalStrut(10));
        root.add(payRow);
        root.add(Box.createVerticalStrut(12));
        root.add(btnConfirm);
        root.add(Box.createVerticalStrut(4));
        root.add(btnCancel);

        // Refresh fee every 30s in case they take a while
        feeRefreshTimer = new Timer(30000, e2 -> {
            Vehicle vv = manager.getParkedVehicle(vehicleNo);
            if (vv != null) {
                double newFee = manager.calculateFee(vv);
                lblFee.setText("₹" + String.format("%.2f", newFee));
            }
        });
        feeRefreshTimer.start();

        add(new JScrollPane(root), BorderLayout.CENTER);
    }

    private void confirmCheckout(double fee) {
        stopRefresh();
        try {
            double actualFee = manager.removeVehicle(vehicleNo);
            if (server != null) {
                server.markUsed(token);
            }
            showReceipt(actualFee);
            onCheckoutDone.run();
            dispose();
        } catch (ParkingException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(), "Checkout Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showReceipt(double fee) {
        JDialog receipt = new JDialog((Frame) getOwner(), "🧾  Receipt", true);
        receipt.setSize(380, 420);
        receipt.setLocationRelativeTo(getOwner());
        receipt.getContentPane().setBackground(BG);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(28, 28, 28, 28));

        JLabel tick = new JLabel("🎉");
        tick.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        tick.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel done = new JLabel("Payment Complete!");
        done.setFont(new Font("Segoe UI", Font.BOLD, 20));
        done.setForeground(GREEN);
        done.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel vLbl = new JLabel(vehicleNo + " has exited");
        vLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        vLbl.setForeground(DIM);
        vLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel amtLbl = new JLabel("₹" + String.format("%.2f", fee) + " Paid");
        amtLbl.setFont(new Font("Segoe UI", Font.BOLD, 32));
        amtLbl.setForeground(new Color(167, 139, 250));
        amtLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel timeLbl = new JLabel(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")));
        timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLbl.setForeground(DIM);
        timeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel thanks = new JLabel("Drive safe! — IIIT Kalyani Smart Parking 🚦");
        thanks.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        thanks.setForeground(new Color(100, 100, 140));
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton close = new JButton("Close");
        close.setFont(new Font("Segoe UI", Font.BOLD, 13));
        close.setBackground(ACCENT);
        close.setForeground(Color.WHITE);
        close.setFocusPainted(false);
        close.setBorder(new EmptyBorder(10, 30, 10, 30));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setAlignmentX(Component.CENTER_ALIGNMENT);
        close.addActionListener(e -> receipt.dispose());

        p.add(tick);
        p.add(Box.createVerticalStrut(8));
        p.add(done);
        p.add(Box.createVerticalStrut(6));
        p.add(vLbl);
        p.add(Box.createVerticalStrut(16));
        p.add(amtLbl);
        p.add(Box.createVerticalStrut(8));
        p.add(timeLbl);
        p.add(Box.createVerticalStrut(20));
        p.add(thanks);
        p.add(Box.createVerticalStrut(20));
        p.add(close);

        receipt.add(p);
        receipt.setVisible(true);
    }

    private JPanel infoBox(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(new Color(30, 32, 55));
        p.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbl.setForeground(DIM);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 12));
        val.setForeground(TEXT);
        p.add(lbl, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private JRadioButton payRadio(String text, boolean selected) {
        JRadioButton r = new JRadioButton(text, selected);
        r.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        r.setForeground(TEXT);
        r.setBackground(BG);
        r.setFocusPainted(false);
        return r;
    }

    private JPanel errorPanel(String msg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        JLabel l = new JLabel("<html><center><font color='red'>❌</font><br><br>" + msg + "</center></html>");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(TEXT);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private void stopRefresh() { if (feeRefreshTimer != null) feeRefreshTimer.stop(); }
}
