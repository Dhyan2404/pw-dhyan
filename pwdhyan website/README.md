# PW DHYAN • PC Web Browser & Study Portal 🚀

Dedicated PC Web Browser built for **StudyParcham** by **Dhyan**.

Designed to run seamlessly on PC (Google Chrome, Edge, or any modern browser) as your own custom browser ("Apna Browser") with complete security, notification access, and automatic script injection.

---

## 🌟 Key Features

1. **Step 1: Cyber-Glass Passkey Security Gate ("Pehle Login")**
   - 6-digit numeric keypad + physical keyboard support.
   - Master Permanent PIN: `240411` (Infinite / Admin Access).
   - Real-time Google Cloud Firestore validation against `access_keys` collection.
   - Session persistence in `localStorage`.

2. **Step 2: Notification Access Gate ("Notification Access")**
   - High-tech radar prompt requesting desktop notification permissions.
   - Web Audio API chimes and test desktop alerts.
   - Delivers live class alarms, DPP notes updates, and cloud broadcast alerts from Dhyan.

3. **Step 3: StudyParcham with Auto-Injected Scripts ("StudyParcham Page with Script Loads")**
   - Built-in lightweight local proxy server that automatically embeds:
     - `scripts/masterkey.js` (Pure Liquid Glass Portal • Theme customizer & ad/telegram blocker)
     - `scripts/liquid_player.js` (Pure Liquid Glass Player • Ultra-wide lecture mode, gesture scrubbing, devtools bypass)
   - Chrome Browser Top Bar:
     - Back, Forward, Reload, Home buttons
     - Omnibar displaying live script injection status
     - Status badges showing active script states
     - Widescreen & Fullscreen toggles
     - Floating video speed dock (1x, 1.25x, 1.5x, 2x)
     - Real-time announcement alerts from Cloud Firestore

---

## ⚡ How to Run on PC

### Method 1: One-Click Windows Launcher (Recommended)
Simply **double-click** `start.bat` in this folder:
- Starts the local Node.js proxy server.
- Opens Google Chrome automatically in **Standalone App Mode** (fullscreen app without browser URL bar clutter).

### Method 2: Via Terminal (Node.js)
```bash
# In this directory:
node server.js
```
Then open your browser and navigate to:
👉 **`http://localhost:3300`**

---

## 🔐 Credentials

- **Master Permanent PIN**: `240411`
- Any valid Firestore Cloud Passkey generated via the Admin Hub.

---

Made with ❤️ by **Dhyan**
