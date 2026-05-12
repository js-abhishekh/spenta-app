# spenta 💸

A blazing-fast, privacy-first, and automated expense tracker for Android that works 100% offline.

**spenta** acts as your financial co-pilot by intelligently listening to your device's incoming transactional notifications (like bank alerts or UPI payment receipts). It parses the data and prompts you to categorize the expense—all without ever sending a single byte of your financial data to the cloud.

---

## ✨ Features

*   **🤖 Automated Expense Capture:** Uses a background `NotificationListenerService` to instantly detect when you spend or receive money from any banking or payment app.
*   **📡 Offline Bill Splitting:** Split expenses with nearby friends using **Google Nearby Connections**. No internet required—just search, connect via P2P, or scan a QR code to send split requests instantly.
*   **📉 Safe-to-Spend Intelligence:** A dynamic budgeting engine that calculates your daily allowance. It tells you exactly how much you can spend *today* to stay within your monthly/weekly goals.
*   **🛡️ 100% Offline & Private:** Built with a strict zero-cloud policy. Your financial data never leaves your device. No accounts, no sync, no leaks.
*   **⚡ Frictionless Categorization:** Identify merchants and categories directly from your notification shade or via a "One-Tap Split" button in the app.
*   **📊 Interactive Analytics:** Visualize your cash flow with beautiful donut charts and time-period filters (Weekly, Monthly, Yearly).
*   **💾 Data Portability:** Export your entire ledger to **CSV** for Excel/Sheets or create encrypted **JSON** backups for local storage.

---

## 🛠️ Tech Stack

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM (Model-View-ViewModel) with Repository Pattern
*   **Local Storage:** 
    *   Room Database (Transactions & Categories)
    *   Jetpack DataStore (User Preferences)
*   **Connectivity:** Google Play Services Nearby (P2P_STAR Strategy)
*   **Scanning:** ML Kit Barcode Scanning (QR Discovery)
*   **Optimization:** R8/ProGuard enabled for production-grade security and performance.

---

## 🚀 How It Works Under the Hood

1.  **The Listener:** An optimized background service monitors incoming alerts from banking and UPI apps.
2.  **The Engine:** Using finely-tuned Regex, it extracts currency, exact amounts, and vendor names while ignoring OTPs and spam.
3.  **The Vault:** Data is instantly saved to a local Room database.
4.  **The Interaction:** Redirection logic allows you to move from a bank notification straight to the **Split Bill** screen with the total amount pre-filled automatically.

---

## 📱 Installation & Testing

### Prerequisites
*   **Physical Android Device:** Android 8.0 (API 26) or higher. (Emulators do not support Nearby Connections or Notification Listening well).
*   **Build Environment:** Android Studio Ladybug (2024.2.1) or newer.

### Setup
1.  Clone the repository: `git clone https://github.com/js-abhishekh/spenta-app.git`
2.  **Notification Access:** On first launch, follow the onboarding flow to enable "Notification Access". This is mandatory for automation.
3.  **Permissions:** Grant Camera (for QR) and Nearby Devices (for splitting) when prompted.

### Installing via APK (Sideloading)
If you are downloading the latest `.apk` from the Releases tab:
1. Download the APK to your Android device.
2. Tap the file to install. If prompted, grant your browser/file manager permission to "Install unknown apps".
3. Open **spenta**, complete the setup, and grant Notification Access in your device settings.

---

## 🎨 Design System

**spenta** features a custom brand identity designed for modern finance:
*   **Background:** Deep Onyx (`#0A0A0A`)
*   **Primary Accent:** Cyber Lime (`#CCFF00`)
*   **Secondary Accent:** Soft Terracotta (`#E2725B`)

---

## 🔒 Privacy Policy
*   **Zero Data Transmission:** The app does not declare the `android.permission.INTERNET` permission. It is technically impossible for the app to send your data anywhere.
*   **Local Processing:** All notification parsing and OCR scanning happen on-device using local APIs.
*   **Encrypted Storage:** Data is stored in the app's private internal storage, accessible only by the OS.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Built with 💚 by [Abhishekh JS](https://github.com/js-abhishekh) for smart spenders.*