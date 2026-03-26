package smartparking;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Lightweight HTTP server built on Java's built-in com.sun.net.httpserver.
 * No external dependencies required.
 *
 * Endpoints:
 *   GET  /checkout?token=<TOKEN>   → checkout page (customer scans QR)
 *   POST /checkout                  → process checkout, show receipt
 *   GET  /qr?token=<TOKEN>          → serve QR PNG image
 *   GET  /status                    → JSON status (for dashboard polling)
 */
public class CheckoutServer {

    private final ParkingManager manager;
    private final int port;
    private HttpServer server;

    // token → vehicle details for QR lookup
    private final Map<String, String[]> tokenMap = new HashMap<>();
    // token → qr png bytes
    private final Map<String, byte[]> qrMap = new HashMap<>();
    // tokens that have been verified by QR scan at gate
    private final java.util.Set<String> verifiedTokens = new java.util.HashSet<>();

    public static final String BASE_URL_DEFAULT = "http://localhost:8080";
    private String baseUrl;

    public CheckoutServer(ParkingManager manager, int port, String host) {
        this.manager = manager;
        this.port    = port;
        this.baseUrl = "http://" + host + ":" + port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/checkout", this::handleCheckout);
        server.createContext("/qr",       this::handleQR);
        server.createContext("/status",   this::handleStatus);
        server.setExecutor(null);
        server.start();
        System.out.println("[CheckoutServer] Listening on " + baseUrl);
    }

    public void stop() { if (server != null) server.stop(0); }

    // ── Token management ─────────────────────────────────────────────────
    /** Register a new token for a parked vehicle. Returns the checkout URL. */
    public String registerToken(String vehicleNo, int slotNo, String vehicleType) {
        String token = generateToken(vehicleNo);
        tokenMap.put(token, new String[]{vehicleNo, String.valueOf(slotNo), vehicleType});

        // Pre-generate QR
        String url = baseUrl + "/checkout?token=" + token;
        byte[] qr = QRCodeGenerator.generate(url, 300);
        if (qr != null) qrMap.put(token, qr);

        return url;
    }

    public byte[] getQRBytes(String token) { return qrMap.get(token); }
    public String getCheckoutUrl(String token) { return baseUrl + "/checkout?token=" + token; }

    public String getTokenForVehicle(String vehicleNo) {
        for (Map.Entry<String,String[]> e : tokenMap.entrySet()) {
            if (e.getValue()[0].equalsIgnoreCase(vehicleNo)) return e.getKey();
        }
        return null;
    }

    /** Returns vehicle number if token is valid, null if invalid/expired */
    public String getVehicleForToken(String token) {
        if (token == null) return null;
        String[] info = tokenMap.get(token);
        return info != null ? info[0] : null;
    }

    /** Called by QR scanner after successful scan — marks token as gate-verified */
    public void markVerified(String token) { verifiedTokens.add(token); }

    public boolean isVerified(String token) { return verifiedTokens.contains(token); }

    /** Remove token after successful checkout so it can't be reused */
    public void markUsed(String token) {
        tokenMap.remove(token);
        qrMap.remove(token);
        verifiedTokens.remove(token);
    }

    // ── Handlers ─────────────────────────────────────────────────────────
    private void handleCheckout(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String query  = ex.getRequestURI().getQuery();

        if ("GET".equals(method)) {
            String token = getParam(query, "token");
            if (token == null || !tokenMap.containsKey(token)) {
                send(ex, 404, "text/html", errorPage("Invalid or expired QR code."));
                return;
            }
            String[] info = tokenMap.get(token);
            String vehicleNo = info[0], slotNo = info[1], vType = info[2];
            Vehicle v = manager.getParkedVehicle(vehicleNo);
            if (v == null) {
                send(ex, 200, "text/html", alreadyCheckedOut(vehicleNo));
                return;
            }
            double estFee = manager.calculateFee(v);
            send(ex, 200, "text/html", checkoutPage(token, vehicleNo, slotNo, vType, v, estFee));

        } else if ("POST".equals(method)) {
            // Read POST body
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String token = getFormParam(body, "token");
            if (token == null || !tokenMap.containsKey(token)) {
                send(ex, 400, "text/html", errorPage("Invalid token."));
                return;
            }
            String[] info = tokenMap.get(token);
            String vehicleNo = info[0];
            try {
                double fee = manager.removeVehicle(vehicleNo);
                tokenMap.remove(token);
                qrMap.remove(token);
                send(ex, 200, "text/html", receiptPage(vehicleNo, info[1], fee));
            } catch (ParkingException e) {
                send(ex, 200, "text/html", errorPage(e.getMessage()));
            }
        }
    }

    private void handleQR(HttpExchange ex) throws IOException {
        String token = getParam(ex.getRequestURI().getQuery(), "token");
        byte[] png   = token != null ? qrMap.get(token) : null;
        if (png == null) { send(ex, 404, "text/plain", "QR not found"); return; }
        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.sendResponseHeaders(200, png.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(png); }
    }

    private void handleStatus(HttpExchange ex) throws IOException {
        int carAvail  = manager.getAvailableCount("Car");
        int bikeAvail = manager.getAvailableCount("Bike");
        String json = "{\"carAvail\":" + carAvail + ",\"bikeAvail\":" + bikeAvail
                    + ",\"occupied\":" + manager.getOccupiedSlots().size() + "}";
        send(ex, 200, "application/json", json);
    }

    // ── Page builders ─────────────────────────────────────────────────────
    private String checkoutPage(String token, String vehicleNo, String slotNo,
                                 String vType, Vehicle v, double estFee) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");
        long mins = Duration.between(v.getEntryTime(), LocalDateTime.now()).toMinutes();
        String duration = (mins / 60) + "h " + (mins % 60) + "m";
        String entryStr = v.getEntryTime().format(fmt);
        String nowStr   = LocalDateTime.now().format(fmt);
        String icon     = vType.equals("Car") ? "🚗" : "🏍";

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Checkout — Smart Parking</title>
        <style>
          * { box-sizing: border-box; margin: 0; padding: 0; }
          body { font-family: 'Segoe UI', sans-serif; background: #0f0f1a; color: #e0e0ff;
                 min-height: 100vh; display: flex; align-items: center; justify-content: center; }
          .card { background: #1a1a2e; border: 1px solid #2d2d50; border-radius: 20px;
                  padding: 36px 32px; width: 100%; max-width: 460px; box-shadow: 0 20px 60px rgba(0,0,0,.5); }
          h1 { font-size: 1.5rem; color: #7c3aed; margin-bottom: 6px; }
          .sub { color: #888; font-size: .85rem; margin-bottom: 24px; }
          .badge { display: inline-block; background: #7c3aed22; color: #a78bfa;
                   border: 1px solid #7c3aed44; border-radius: 8px; padding: 3px 12px;
                   font-size: .78rem; margin-bottom: 20px; }
          .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 24px; }
          .info-box { background: #12122a; border: 1px solid #2d2d50; border-radius: 12px;
                      padding: 14px 16px; }
          .info-box .label { font-size: .72rem; color: #666; text-transform: uppercase;
                              letter-spacing: .05em; margin-bottom: 4px; }
          .info-box .value { font-size: 1.1rem; font-weight: 700; color: #e0e0ff; }
          .fee-box { background: linear-gradient(135deg,#1e1b4b,#1a1a2e);
                     border: 2px solid #7c3aed; border-radius: 14px; padding: 20px;
                     text-align: center; margin-bottom: 24px; }
          .fee-box .label { color: #888; font-size: .85rem; }
          .fee-box .amount { font-size: 2.5rem; font-weight: 800; color: #a78bfa; margin: 6px 0; }
          .fee-box .note { color: #555; font-size: .78rem; }
          .btn { display: block; width: 100%; padding: 16px;
                 background: linear-gradient(135deg, #7c3aed, #4f46e5);
                 color: white; border: none; border-radius: 12px; font-size: 1.05rem;
                 font-weight: 700; cursor: pointer; transition: opacity .2s; }
          .btn:hover { opacity: .88; }
          .timeline { font-size: .8rem; color: #555; margin-bottom: 20px; line-height: 1.9; }
          .timeline span { color: #888; }
          .icon-big { font-size: 2.5rem; margin-bottom: 10px; display: block; }
          .warning { background: #2d1a00; border: 1px solid #7c3a00; border-radius: 10px;
                     padding: 10px 14px; font-size: .8rem; color: #fbbf24; margin-bottom: 20px; }
        </style>
        </head>
        <body>
        <div class="card">
          <span class="icon-big">""" + icon + """
        </span>
          <h1>Exit Checkout</h1>
          <p class="sub">Smart Parking Management — IIIT Kalyani</p>
          <span class="badge">✅ QR Verified</span>

          <div class="info-grid">
            <div class="info-box">
              <div class="label">Vehicle No</div>
              <div class="value">""" + vehicleNo + """
        </div>
            </div>
            <div class="info-box">
              <div class="label">Type</div>
              <div class="value">""" + icon + " " + vType + """
        </div>
            </div>
            <div class="info-box">
              <div class="label">Slot</div>
              <div class="value">#""" + slotNo + """
        </div>
            </div>
            <div class="info-box">
              <div class="label">Duration</div>
              <div class="value">""" + duration + """
        </div>
            </div>
          </div>

          <div class="timeline">
            <span>Entry:</span>  """ + entryStr + """
        <br>
            <span>Now:  </span>  """ + nowStr + """
        </div>

          <div class="fee-box">
            <div class="label">Total Parking Fee</div>
            <div class="amount">₹""" + String.format("%.2f", estFee) + """
        </div>
            <div class="note">Minimum 1 hour charged</div>
          </div>

          <div class="warning">⚠️ Fee is estimated. Exact amount calculated on checkout.</div>

          <form method="POST" action="/checkout">
            <input type="hidden" name="token" value=\"""" + token + """
        \">
            <button class="btn" type="submit">✅ Confirm Checkout &amp; Pay ₹""" + String.format("%.2f", estFee) + """
        </button>
          </form>
        </div>
        </body></html>
        """;
    }

    private String receiptPage(String vehicleNo, String slotNo, double fee) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        return """
        <!DOCTYPE html><html lang="en">
        <head><meta charset="UTF-8"><title>Receipt</title>
        <style>
          * { box-sizing: border-box; margin:0; padding:0; }
          body { font-family:'Segoe UI',sans-serif; background:#0f0f1a; color:#e0e0ff;
                 min-height:100vh; display:flex; align-items:center; justify-content:center; }
          .card { background:#1a1a2e; border:1px solid #2d2d50; border-radius:20px;
                  padding:36px 32px; width:100%; max-width:420px; text-align:center; }
          .tick { font-size:4rem; margin-bottom:16px; display:block; }
          h1 { color:#22c55e; font-size:1.6rem; margin-bottom:8px; }
          .sub { color:#555; font-size:.85rem; margin-bottom:28px; }
          .row { display:flex; justify-content:space-between; padding:10px 0;
                 border-bottom:1px solid #1e1e38; font-size:.95rem; }
          .row:last-of-type { border:none; }
          .lbl { color:#666; }
          .val { font-weight:600; color:#e0e0ff; }
          .fee-final { font-size:2rem; font-weight:800; color:#22c55e;
                       background:#0a2a0a; border:2px solid #22c55e44;
                       border-radius:12px; padding:16px; margin:20px 0; }
          .thanks { color:#444; font-size:.8rem; margin-top:16px; }
        </style></head>
        <body>
        <div class="card">
          <span class="tick">🎉</span>
          <h1>Checkout Complete!</h1>
          <p class="sub">Thank you for using Smart Parking</p>
          <div class="row"><span class="lbl">Vehicle No</span><span class="val">""" + vehicleNo + """
        </span></div>
          <div class="row"><span class="lbl">Slot Released</span><span class="val">#""" + slotNo + """
        </span></div>
          <div class="row"><span class="lbl">Time</span><span class="val">""" + now + """
        </span></div>
          <div class="fee-final">₹""" + String.format("%.2f", fee) + """
         Paid</div>
          <p class="thanks">Drive safe! 🚦 — IIIT Kalyani Smart Parking</p>
        </div></body></html>""";
    }

    private String alreadyCheckedOut(String vNo) {
        return "<html><body style='font-family:sans-serif;background:#0f0f1a;color:#e0e0ff;"
             + "display:flex;align-items:center;justify-content:center;height:100vh'>"
             + "<div style='text-align:center'><div style='font-size:3rem'>✅</div>"
             + "<h2 style='color:#22c55e'>Already Checked Out</h2>"
             + "<p style='color:#555'>Vehicle " + vNo + " has already exited.</p></div></body></html>";
    }

    private String errorPage(String msg) {
        return "<html><body style='font-family:sans-serif;background:#0f0f1a;color:#e0e0ff;"
             + "display:flex;align-items:center;justify-content:center;height:100vh'>"
             + "<div style='text-align:center'><div style='font-size:3rem'>❌</div>"
             + "<h2 style='color:#ef4444'>Error</h2><p style='color:#888'>" + msg + "</p></div></body></html>";
    }

    // ── Utilities ─────────────────────────────────────────────────────────
    private void send(HttpExchange ex, int code, String ct, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", ct + "; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private String getParam(String query, String key) {
        if (query == null) return null;
        for (String p : query.split("&")) {
            if (p.startsWith(key + "=")) {
                try { return URLDecoder.decode(p.substring(key.length()+1), "UTF-8"); }
                catch (Exception e) { return null; }
            }
        }
        return null;
    }

    private String getFormParam(String body, String key) {
        return getParam(body.replace("&", "&"), key);
    }

    private String generateToken(String vehicleNo) {
        return vehicleNo.replaceAll("[^A-Z0-9]","")
             + "_" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
    }
}
