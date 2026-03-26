package smartparking;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * MessagingService — sends WhatsApp messages via Twilio API.
 *
 * HOW TO ENABLE:
 *   1. Sign up at https://www.twilio.com (free trial available)
 *   2. Enable WhatsApp Sandbox: Console → Messaging → Try it out → Send a WhatsApp message
 *   3. Fill in your credentials below or set as environment variables:
 *        TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_FROM
 *
 * FREE ALTERNATIVE (no Twilio):
 *   Set USE_WHATSAPP_LINK = true — generates a wa.me deep-link that opens
 *   WhatsApp with a pre-filled message. Staff can send it manually.
 */
public class MessagingService {

    // ── Config — fill these in or set as env vars ─────────────────────────
    private static final String ACCOUNT_SID  = getEnv("TWILIO_ACCOUNT_SID",  "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    private static final String AUTH_TOKEN   = getEnv("TWILIO_AUTH_TOKEN",   "your_auth_token_here");
    private static final String FROM_NUMBER  = getEnv("TWILIO_WHATSAPP_FROM","whatsapp:+14155238886"); // Twilio sandbox

    /** Set true to skip Twilio and just generate a WhatsApp deep link */
    public static boolean USE_WHATSAPP_LINK = false;

    // ─────────────────────────────────────────────────────────────────────
    public static class MessageResult {
        public final boolean success;
        public final String  message;
        public final String  whatsappLink;   // always set for manual fallback
        public MessageResult(boolean s, String m, String l) { success=s; message=m; whatsappLink=l; }
    }

    /**
     * Sends a parking confirmation WhatsApp message to the customer.
     * @param toPhone  customer phone in E.164 format, e.g. +919876543210
     * @param vehicleNo vehicle registration number
     * @param slotNo   assigned slot number
     * @param checkoutUrl  URL the QR code encodes (your checkout page + token)
     */
    public static MessageResult sendParkingConfirmation(
            String toPhone, String vehicleNo, int slotNo, String checkoutUrl) {

        String body = buildMessage(vehicleNo, slotNo, checkoutUrl);
        String waLink = buildWhatsAppLink(toPhone, body);

        if (USE_WHATSAPP_LINK || isPlaceholderConfig()) {
            return new MessageResult(false, "WhatsApp link generated (manual send)", waLink);
        }

        return sendViaTwilio("whatsapp:" + toPhone, body, waLink);
    }

    private static String buildMessage(String vehicleNo, int slotNo, String checkoutUrl) {
        return "🅿 *Smart Parking — IIIT Kalyani*\n\n"
             + "✅ Your vehicle *" + vehicleNo + "* has been parked.\n"
             + "📍 Slot Number: *" + slotNo + "*\n\n"
             + "🔲 *Your Exit QR Code:*\n"
             + checkoutUrl + "\n\n"
             + "Show this QR code at the exit gate for instant checkout.\n"
             + "_Keep this message safe._";
    }

    private static String buildWhatsAppLink(String toPhone, String body) {
        try {
            String phone = toPhone.replaceAll("[^0-9]", "");
            String encoded = URLEncoder.encode(body, StandardCharsets.UTF_8.name());
            return "https://wa.me/" + phone + "?text=" + encoded;
        } catch (Exception e) {
            return "https://wa.me/?text=ParkingConfirmation";
        }
    }

    private static MessageResult sendViaTwilio(String toWhatsApp, String body, String waLink) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";
            String data = "To=" + URLEncoder.encode(toWhatsApp, "UTF-8")
                        + "&From=" + URLEncoder.encode(FROM_NUMBER, "UTF-8")
                        + "&Body=" + URLEncoder.encode(body, "UTF-8");

            URL apiUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String auth = Base64.getEncoder().encodeToString((ACCOUNT_SID + ":" + AUTH_TOKEN).getBytes());
            conn.setRequestProperty("Authorization", "Basic " + auth);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                return new MessageResult(true, "WhatsApp message sent via Twilio!", waLink);
            } else {
                return new MessageResult(false, "Twilio error: HTTP " + code, waLink);
            }
        } catch (Exception e) {
            return new MessageResult(false, "Send failed: " + e.getMessage(), waLink);
        }
    }

    private static boolean isPlaceholderConfig() {
        return ACCOUNT_SID.startsWith("ACxxx") || AUTH_TOKEN.equals("your_auth_token_here");
    }

    private static String getEnv(String key, String def) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : def;
    }
}
