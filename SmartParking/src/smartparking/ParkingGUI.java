package smartparking;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.List;

public class ParkingGUI extends JFrame {

    private static final Color BG       = new Color(18, 18, 30);
    private static final Color PANEL_BG = new Color(28, 30, 50);
    private static final Color CARD_BG  = new Color(36, 38, 62);
    private static final Color ACCENT   = new Color(99, 102, 241);
    private static final Color GREEN    = new Color(34, 197, 94);
    private static final Color RED      = new Color(239, 68, 68);
    private static final Color AMBER    = new Color(251, 191, 36);
    private static final Color WA_GREEN = new Color(37, 211, 102);
    private static final Color TEXT     = new Color(230, 232, 255);
    private static final Color TEXT_DIM = new Color(140, 145, 190);
    private static final Color BORDER_C = new Color(55, 58, 90);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_HEAD  = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_MONO  = new Font("Consolas", Font.PLAIN, 12);

    private ParkingManager manager;
    private CheckoutServer server;
    private static final int SERVER_PORT = 8080;

    private JTextField tfVehicleNo, tfExitNo, tfPhone;
    private JComboBox<String> cbVehicleType;
    private JLabel lblCarAvail, lblBikeAvail;
    private DefaultTableModel slotsTableModel, activeTableModel;
    private JTable slotsTable, activeTable;
    private JTextArea logArea;

    public ParkingGUI() {
        manager = new ParkingManager(10, 10);
        startServer();
        buildUI();
        refreshAll();
    }

    private void startServer() {
        try {
            server = new CheckoutServer(manager, SERVER_PORT, "localhost");
            server.start();
        } catch (Exception e) {
            server = null;
        }
    }

    private void buildUI() {
        setTitle("Smart Parking Management System ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1260, 780);
        setMinimumSize(new Dimension(1060, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(buildHeader(),    BorderLayout.NORTH);
        add(buildMain(),      BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { if (server != null) server.stop(); }
        });
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL_BG);
        p.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_C));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        left.setOpaque(false);
        JLabel icon = new JLabel("P"); icon.setFont(new Font("Segoe UI", Font.BOLD, 30)); icon.setForeground(ACCENT);
        JLabel title = new JLabel("Smart Parking Management System");
        title.setFont(FONT_TITLE); title.setForeground(TEXT);
        JLabel sub = new JLabel(" OOP Java Project");
        sub.setFont(FONT_SMALL); sub.setForeground(TEXT_DIM);
        left.add(icon); left.add(title); left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 12));
        right.setOpaque(false);
        lblCarAvail  = statLabel("", GREEN);
        lblBikeAvail = statLabel("", GREEN);
        JLabel srvLbl = new JLabel(server != null ? "[Server :8080 Active]" : "[Server Offline]");
        srvLbl.setFont(FONT_SMALL);
        srvLbl.setForeground(server != null ? GREEN : RED);
        right.add(chip("Cars: ", lblCarAvail));
        right.add(chip("Bikes: ", lblBikeAvail));
        right.add(srvLbl);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildMain() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(6, 6, 6, 6);

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0.27; gc.weighty = 0.58;
        p.add(buildEntryCard(), gc);
        gc.gridy = 1; gc.weighty = 0.42;
        p.add(buildExitCard(), gc);

        gc.gridx = 1; gc.gridy = 0; gc.weightx = 0.36; gc.weighty = 1.0; gc.gridheight = 2;
        p.add(buildSlotsCard(), gc);

        gc.gridx = 2; gc.gridy = 0; gc.weightx = 0.37; gc.weighty = 0.55; gc.gridheight = 1;
        p.add(buildActiveCard(), gc);
        gc.gridy = 1; gc.weighty = 0.45;
        p.add(buildLogCard(), gc);

        return p;
    }

    private JPanel buildEntryCard() {
        JPanel card = card("Vehicle Entry");
        card.setLayout(new BorderLayout(0, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5, 4, 5, 4);

        g.gridx = 0; g.gridy = 0; g.weightx = 0; form.add(label("Vehicle No:"), g);
        g.gridx = 1; g.weightx = 1;
        tfVehicleNo = styledField("e.g. MH12AB1234"); form.add(tfVehicleNo, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0; form.add(label("Type:"), g);
        g.gridx = 1; g.weightx = 1;
        cbVehicleType = new JComboBox<>(new String[]{"Car", "Bike"});
        styleCombo(cbVehicleType); form.add(cbVehicleType, g);

        g.gridx = 0; g.gridy = 2; g.weightx = 0; form.add(label("Phone (WhatsApp):"), g);
        g.gridx = 1; g.weightx = 1;
        tfPhone = styledField("+91XXXXXXXXXX  (optional)"); form.add(tfPhone, g);

        JLabel qrNote = new JLabel("  QR ticket auto-generated on park");
        qrNote.setFont(FONT_SMALL); qrNote.setForeground(new Color(167, 139, 250));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        btnRow.setOpaque(false);
        JButton bEntry = btn("  Park + Generate QR  ", ACCENT);
        bEntry.addActionListener(e -> handleEntry());
        btnRow.add(bEntry);

        JPanel rates = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 2));
        rates.setOpaque(false);
        rates.add(badge("Car  Rs.30/hr",  new Color(99, 102, 241, 60)));
        rates.add(badge("Bike Rs.15/hr",  new Color(34, 197, 94,  60)));

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(form, BorderLayout.NORTH);
        center.add(qrNote, BorderLayout.CENTER);
        JPanel btm = new JPanel(new BorderLayout(0, 4)); btm.setOpaque(false);
        btm.add(btnRow, BorderLayout.NORTH); btm.add(rates, BorderLayout.SOUTH);
        center.add(btm, BorderLayout.SOUTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildExitCard() {
        JPanel card = card("Exit Gate  —  QR Scan Required");
        card.setLayout(new BorderLayout(0, 10));

        JPanel lockNote = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        lockNote.setOpaque(false);
        JLabel lockLbl = new JLabel("  Payment only allowed after QR scan verification");
        lockLbl.setFont(FONT_SMALL); lockLbl.setForeground(AMBER);
        lockNote.add(lockLbl);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5, 4, 5, 4);
        g.gridx = 0; g.gridy = 0; g.weightx = 0; form.add(label("Vehicle No:"), g);
        g.gridx = 1; g.weightx = 1;
        tfExitNo = styledField("Enter vehicle no for lookup"); form.add(tfExitNo, g);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        btnRow.setOpaque(false);
        JButton bLookup = btn("  Lookup  ", new Color(55, 58, 90));
        bLookup.addActionListener(e -> handleLookup());
        JButton bShowQR = btn("  Show QR  ", new Color(124, 58, 237));
        bShowQR.addActionListener(e -> handleShowQR());
        JButton bScan = btn("  Scan QR + Checkout  ", new Color(34, 197, 94));
        bScan.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bScan.addActionListener(e -> handleScanAndCheckout());
        btnRow.add(bLookup); btnRow.add(bShowQR); btnRow.add(bScan);

        JLabel waHint = new JLabel("  Customer can also self-checkout via WhatsApp QR link");
        waHint.setFont(FONT_SMALL); waHint.setForeground(WA_GREEN);

        card.add(lockNote, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 6)); center.setOpaque(false);
        center.add(form, BorderLayout.NORTH);
        center.add(btnRow, BorderLayout.CENTER);
        center.add(waHint, BorderLayout.SOUTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSlotsCard() {
        JPanel card = card("Parking Slots");
        card.setLayout(new BorderLayout(0, 8));
        String[] cols = {"Slot", "Type", "Status", "Vehicle"};
        slotsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        slotsTable = new JTable(slotsTableModel);
        styleTable(slotsTable);
        slotsTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, val, s, f, r, c);
                setHorizontalAlignment(CENTER);
                setForeground("AVAILABLE".equals(val) ? GREEN : RED);
                return this;
            }
        });
        JScrollPane sp = new JScrollPane(slotsTable);
        sp.setBackground(CARD_BG); sp.getViewport().setBackground(CARD_BG);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C));
        JPanel fr = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); fr.setOpaque(false);
        JComboBox<String> cbF = new JComboBox<>(new String[]{"All Slots","Available - Car","Available - Bike","Occupied"});
        styleCombo(cbF); cbF.addActionListener(e -> applyFilter(cbF.getSelectedItem().toString()));
        fr.add(label("Filter: ")); fr.add(cbF);
        card.add(fr, BorderLayout.NORTH); card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildActiveCard() {
        JPanel card = card("Currently Parked");
        card.setLayout(new BorderLayout(0, 8));
        String[] cols = {"Vehicle", "Type", "Slot", "Entry", "QR"};
        activeTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        activeTable = new JTable(activeTableModel);
        styleTable(activeTable);
        activeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = activeTable.getSelectedRow();
                    if (row >= 0) {
                        tfExitNo.setText((String) activeTableModel.getValueAt(row, 0));
                        handleShowQR();
                    }
                }
            }
        });
        JScrollPane sp = new JScrollPane(activeTable);
        sp.setBackground(CARD_BG); sp.getViewport().setBackground(CARD_BG);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C));
        JLabel hint = new JLabel("  Double-click to re-show QR ticket");
        hint.setFont(FONT_SMALL); hint.setForeground(TEXT_DIM);
        card.add(hint, BorderLayout.NORTH); card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildLogCard() {
        JPanel card = card("Activity Log");
        card.setLayout(new BorderLayout(0, 6));
        logArea = new JTextArea();
        logArea.setEditable(false); logArea.setFont(FONT_MONO);
        logArea.setBackground(new Color(20, 22, 38));
        logArea.setForeground(new Color(180, 255, 180));
        logArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C));
        sp.getViewport().setBackground(new Color(20, 22, 38));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL_BG);
        p.setBorder(new CompoundBorder(new MatteBorder(1,0,0,0,BORDER_C), new EmptyBorder(5,16,5,16)));
        JLabel l = new JLabel("IIIT Kalyani  |  Prof. Oishila Bandyopadhyay  |  QR Self-Checkout: http://localhost:8080/checkout?token=<TOKEN>");
        l.setFont(FONT_SMALL); l.setForeground(TEXT_DIM);
        JLabel r = new JLabel("Car Rs.30/hr  |  Bike Rs.15/hr  ");
        r.setFont(FONT_SMALL); r.setForeground(AMBER);
        p.add(l, BorderLayout.WEST); p.add(r, BorderLayout.EAST);
        return p;
    }

    // ── Handlers ─────────────────────────────────────────────────────────
    private void handleEntry() {
        String vNo   = tfVehicleNo.getText().trim();
        String type  = (String) cbVehicleType.getSelectedItem();
        String phone = tfPhone.getText().trim();
        try {
            Vehicle v = type.equals("Car") ? new Car(vNo) : new Bike(vNo);
            ParkingSlot slot = manager.addVehicle(v);

            String checkoutUrl;
            byte[] qrPng;
            if (server != null) {
                checkoutUrl = server.registerToken(vNo, slot.getSlotNumber(), type);
                qrPng = server.getQRBytes(server.getTokenForVehicle(vNo));
            } else {
                checkoutUrl = "http://localhost:8080/checkout?token=" + vNo;
                qrPng = QRCodeGenerator.generate(checkoutUrl, 300);
            }

            tfVehicleNo.setText(""); tfPhone.setText("");
            refreshAll();

            // Auto-send WhatsApp if phone given
            if (!phone.isEmpty() && phone.startsWith("+")) {
                final String fp = phone, fu = checkoutUrl, fv = vNo;
                final int fs = slot.getSlotNumber();
                new Thread(() -> {
                    MessagingService.MessageResult r =
                            MessagingService.sendParkingConfirmation(fp, fv, fs, fu);
                    if (r.whatsappLink != null && !r.whatsappLink.isEmpty()) {
                        try { Desktop.getDesktop().browse(new URI(r.whatsappLink)); }
                        catch (Exception ignored) {}
                    }
                }).start();
            }

            // Show QR dialog
            new QRDialog(this, vNo.toUpperCase(), slot.getSlotNumber(), type, qrPng, checkoutUrl, server)
                    .setVisible(true);

        } catch (ParkingException | IllegalArgumentException ex) {
            showError("Entry Failed", ex.getMessage());
        }
    }

    private void handleScanAndCheckout() {
        // Open QR scanner — checkout only proceeds after successful scan
        if (server == null) { showError("Server Offline", "Checkout server not running."); return; }
        QRScannerDialog scanner = new QRScannerDialog(this, server, token -> {
            // This runs only after QR is verified
            server.markVerified(token);
            SwingUtilities.invokeLater(() -> {
                new CheckoutDialog(this, manager, server, token, () -> {
                    tfExitNo.setText("");
                    refreshAll();
                }).setVisible(true);
            });
        });
        scanner.setVisible(true);
    }

    private void handleShowQR() {
        String vNo = tfExitNo.getText().trim();
        if (vNo.isEmpty()) { showError("Input", "Enter a vehicle number."); return; }
        if (server == null) { showError("Server Offline", "Checkout server not running."); return; }
        String token = server.getTokenForVehicle(vNo);
        if (token == null) { showError("Not Found", "No QR for " + vNo.toUpperCase() + ". Park vehicle first."); return; }
        String url  = server.getCheckoutUrl(token);
        byte[] qr   = server.getQRBytes(token);
        Vehicle v   = manager.getParkedVehicle(vNo);
        String type = v != null ? v.getVehicleType() : "Car";
        int slotNo  = 0;
        for (ParkingSlot s : manager.getAllSlots())
            if (s.isOccupied() && s.getParkedVehicle().getVehicleNo().equalsIgnoreCase(vNo))
                slotNo = s.getSlotNumber();
        new QRDialog(this, vNo.toUpperCase(), slotNo, type, qr, url, server).setVisible(true);
    }

    private void handleLookup() {
        String vNo = tfExitNo.getText().trim();
        if (vNo.isEmpty()) { showError("Input", "Enter a vehicle number."); return; }
        Vehicle v = manager.getParkedVehicle(vNo);
        if (v == null) { showError("Not Found", vNo.toUpperCase() + " not found."); return; }
        double est = manager.calculateFee(v);
        showInfo("Vehicle Found", "<html>" + v.getDetails().replace("|","<br>")
                + String.format("<br>Est. Fee: <b>Rs.%.2f</b></html>", est));
    }

    private void applyFilter(String filter) {
        slotsTableModel.setRowCount(0);
        for (ParkingSlot s : manager.getAllSlots()) {
            boolean show = switch (filter) {
                case "Available - Car"  -> !s.isOccupied() && s.getSlotType().equals("Car");
                case "Available - Bike" -> !s.isOccupied() && s.getSlotType().equals("Bike");
                case "Occupied"         -> s.isOccupied();
                default -> true;
            };
            if (show) {
                String vn = s.isOccupied() ? s.getParkedVehicle().getVehicleNo() : "---";
                slotsTableModel.addRow(new Object[]{s.getSlotNumber(), s.getSlotType(),
                        s.isOccupied() ? "OCCUPIED" : "AVAILABLE", vn});
            }
        }
    }

    private void refreshAll() {
        int ca=manager.getAvailableCount("Car"), ct=manager.getTotalSlots("Car");
        int ba=manager.getAvailableCount("Bike"), bt=manager.getTotalSlots("Bike");
        lblCarAvail.setText(ca+"/"+ct);  lblCarAvail.setForeground(ca==0?RED:GREEN);
        lblBikeAvail.setText(ba+"/"+bt); lblBikeAvail.setForeground(ba==0?RED:GREEN);
        applyFilter("All Slots");
        activeTableModel.setRowCount(0);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        for (ParkingSlot s : manager.getOccupiedSlots()) {
            Vehicle v = s.getParkedVehicle();
            boolean hasQr = server != null && server.getTokenForVehicle(v.getVehicleNo()) != null;
            activeTableModel.addRow(new Object[]{v.getVehicleNo(), v.getVehicleType(),
                    "Slot "+s.getSlotNumber(), v.getEntryTime().format(fmt), hasQr?"Yes":"--"});
        }
        StringBuilder sb = new StringBuilder();
        List<String> log = manager.getActivityLog();
        for (int i=log.size()-1; i>=0; i--) sb.append(log.get(i)).append("\n");
        logArea.setText(sb.toString());
    }

    // ── UI helpers ───────────────────────────────────────────────────────
    private JPanel card(String title) {
        JPanel p = new JPanel(new BorderLayout(0, 8)); p.setBackground(CARD_BG);
        p.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_C), new EmptyBorder(14,16,14,16)));
        JLabel lbl = new JLabel(title); lbl.setFont(FONT_HEAD); lbl.setForeground(ACCENT);
        lbl.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDER_C), new EmptyBorder(0,0,8,0)));
        p.add(lbl, BorderLayout.NORTH); return p;
    }
    private JLabel label(String t) { JLabel l=new JLabel(t); l.setFont(FONT_BODY); l.setForeground(TEXT_DIM); return l; }
    private JLabel statLabel(String t, Color c) { JLabel l=new JLabel(t); l.setFont(FONT_HEAD); l.setForeground(c); return l; }
    private JPanel chip(String lbl, JLabel val) {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); p.setOpaque(false);
        JLabel l=new JLabel(lbl); l.setFont(FONT_SMALL); l.setForeground(TEXT_DIM);
        p.add(l); p.add(val); return p;
    }
    private JTextField styledField(String ph) {
        JTextField tf = new JTextField(14) {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) { g.setColor(TEXT_DIM); g.setFont(FONT_SMALL); g.drawString(ph, 8, getHeight()/2+4); }
            }
        };
        tf.setFont(FONT_BODY); tf.setBackground(new Color(22,24,42)); tf.setForeground(TEXT); tf.setCaretColor(ACCENT);
        tf.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_C), new EmptyBorder(6,8,6,8)));
        return tf;
    }
    private void styleCombo(JComboBox<?> cb) { cb.setFont(FONT_BODY); cb.setBackground(new Color(22,24,42)); cb.setForeground(TEXT); cb.setBorder(BorderFactory.createLineBorder(BORDER_C)); }
    private JButton btn(String text, Color bg) {
        JButton b=new JButton(text); b.setFont(FONT_BODY); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(8,16,8,16)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e){b.setBackground(bg.brighter());} public void mouseExited(MouseEvent e){b.setBackground(bg);} });
        return b;
    }
    private JLabel badge(String text, Color bg) {
        JLabel l=new JLabel("  "+text+"  "); l.setFont(FONT_SMALL); l.setForeground(TEXT); l.setOpaque(true); l.setBackground(bg);
        l.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_C), new EmptyBorder(3,6,3,6))); return l;
    }
    private void styleTable(JTable t) {
        t.setFont(FONT_BODY); t.setForeground(TEXT); t.setBackground(CARD_BG);
        t.setSelectionBackground(new Color(99,102,241,80)); t.setSelectionForeground(TEXT);
        t.setRowHeight(28); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0,0));
        t.getTableHeader().setFont(FONT_SMALL); t.getTableHeader().setBackground(new Color(22,24,42));
        t.getTableHeader().setForeground(ACCENT); t.getTableHeader().setBorder(BorderFactory.createLineBorder(BORDER_C));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl,Object val,boolean sel,boolean foc,int row,int col) {
                super.getTableCellRendererComponent(tbl,val,sel,foc,row,col);
                setBackground(sel?new Color(99,102,241,80):row%2==0?CARD_BG:new Color(30,32,55));
                setForeground(TEXT); setBorder(new EmptyBorder(0,10,0,10)); return this;
            }
        });
    }
    private void showInfo(String t,String m){JOptionPane.showMessageDialog(this,m,t,JOptionPane.INFORMATION_MESSAGE);}
    private void showError(String t,String m){JOptionPane.showMessageDialog(this,m,t,JOptionPane.ERROR_MESSAGE);}

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("OptionPane.background", new Color(28,30,50));
        UIManager.put("Panel.background", new Color(28,30,50));
        UIManager.put("OptionPane.messageForeground", new Color(230,232,255));
        SwingUtilities.invokeLater(() -> new ParkingGUI().setVisible(true));
    }
}
