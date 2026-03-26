# Smart Parking Management System — V2 (QR + WhatsApp)
**Mini Project | B.Tech CSE Sem 4 | IIIT Kalyani**
Subject: Object Oriented Programming (Java)
Submitted by: Subham Agarwal | Roll No: CSE/24100
Professor: Oishila Bandyopadhyay

---

## What's New in V2

| Feature | Description |
|---|---|
| QR Code Generation | Unique QR ticket generated for every vehicle on park |
| Web Checkout Page | Built-in HTTP server serves a mobile-friendly checkout page |
| WhatsApp Integration | QR + checkout link sent to customer via WhatsApp (Twilio) |
| Self-Checkout | Customer scans QR → opens checkout page → pays online |
| Staff Exit Scan | Staff can scan QR at gate to open checkout directly |

---

## Flow Diagram

```
Staff parks vehicle
        ↓
System assigns slot
        ↓
QR Code generated  ──────────→  Checkout URL encoded in QR
        ↓
WhatsApp message sent to customer's phone
  (contains QR image + link)
        ↓
Customer leaves → scans their QR code (or staff scans it)
        ↓
Browser opens: http://localhost:8080/checkout?token=<TOKEN>
        ↓
Checkout page shows: vehicle, slot, duration, fee
        ↓
Customer clicks "Confirm Checkout" → fee calculated → slot freed
        ↓
Receipt shown on screen
```

---

## Project Structure

```
SmartParkingV2/
└── src/
    └── smartparking/
        ├── Vehicle.java            ← Base class (encapsulation)
        ├── Car.java                ← Inherits Vehicle (inheritance)
        ├── Bike.java               ← Inherits Vehicle (inheritance)
        ├── ParkingSlot.java        ← Slot state management
        ├── ParkingManager.java     ← Core backend logic + fee engine
        ├── ParkingException.java   ← Custom exception
        ├── QRCodeGenerator.java    ← QR PNG generator (ZXing or fallback)
        ├── CheckoutServer.java     ← Built-in HTTP server (Java HttpServer)
        ├── MessagingService.java   ← WhatsApp via Twilio API
        ├── QRDialog.java           ← Swing dialog: shows QR + WhatsApp send
        └── ParkingGUI.java         ← Main Swing GUI
```

---

## How to Compile & Run

### Prerequisites
- JDK 11 or higher

### Compile

```bash
# From SmartParkingV2/
javac -d out -sourcepath src src/smartparking/*.java
```

### Run

```bash
java -cp out smartparking.ParkingGUI
```

The app will start the HTTP checkout server on port 8080 automatically.

---

## Enable Real QR Scanning (ZXing)

Without ZXing, the app generates a placeholder QR image. For real scannable QR codes:

1. Download ZXing core JAR from:
   https://repo1.maven.org/maven2/com/google/zxing/core/3.5.2/core-3.5.2.jar
   https://repo1.maven.org/maven2/com/google/zxing/javase/3.5.2/javase-3.5.2.jar

2. Place both JARs in the `libs/` folder

3. Compile with:
   ```bash
   javac -cp "libs/*" -d out -sourcepath src src/smartparking/*.java
   ```

4. Run with:
   ```bash
   java -cp "out:libs/*" smartparking.ParkingGUI
   # Windows:
   java -cp "out;libs/*" smartparking.ParkingGUI
   ```

ZXing is detected automatically via reflection — no code changes needed.

---

## Enable WhatsApp Messaging (Twilio)

### Step 1 — Sign up at Twilio
- Go to: https://www.twilio.com (free trial available)
- Note your **Account SID** and **Auth Token** from the Console

### Step 2 — Enable WhatsApp Sandbox
- Console → Messaging → Try it out → Send a WhatsApp message
- Follow the sandbox join instructions (send "join <code>" to Twilio's number)
- Default Twilio sandbox number: **+1 415 523 8886**

### Step 3 — Set credentials
Option A — Environment variables (recommended):
```bash
export TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_AUTH_TOKEN=your_auth_token_here
export TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
```

Option B — Edit `MessagingService.java` directly:
```java
private static final String ACCOUNT_SID = "ACxxxx...";
private static final String AUTH_TOKEN  = "your_token";
```

### Without Twilio (Free Option)
Set in MessagingService.java:
```java
public static boolean USE_WHATSAPP_LINK = true;
```
The app will generate a `wa.me` deep link that opens WhatsApp with a
pre-filled message — staff can send it manually from their phone.

---

## Accessing the Checkout Page

When the server is running, customers can open:
```
http://localhost:8080/checkout?token=<TOKEN>
```

For remote access (customer's phone on same WiFi):
Replace `localhost` with your PC's local IP (e.g. `192.168.1.5`):
```
http://192.168.1.5:8080/checkout?token=<TOKEN>
```
Update `CheckoutServer` constructor in `ParkingGUI.java`:
```java
server = new CheckoutServer(manager, SERVER_PORT, "192.168.1.5");
```

---

## OOP Concepts

| Concept | Where Used |
|---|---|
| Class & Object | Vehicle, ParkingSlot, ParkingManager, CheckoutServer |
| Encapsulation | Private fields + getters/setters in Vehicle, ParkingSlot |
| Inheritance | Car, Bike extend Vehicle |
| Polymorphism | Vehicle reference holds Car or Bike; fee rate per type |
| Abstraction | GUI separated from business logic; HTTP handler abstraction |
| Exception Handling | ParkingException, IllegalArgumentException |

---

## Technologies

| Component | Technology |
|---|---|
| Language | Java 11+ |
| Frontend GUI | Java Swing / AWT |
| Web Checkout | Java com.sun.net.httpserver (built-in) |
| QR Code | ZXing (optional) / Built-in fallback |
| WhatsApp | Twilio API / wa.me deep-link |
| Backend Logic | Java OOP (ParkingManager) |
