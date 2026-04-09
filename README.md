# spenta 💸

A blazing-fast, privacy-first, and 100% offline automated expense tracker for Android.

**spenta** acts as your financial co-pilot by intelligently listening to your device's incoming transactional notifications (like bank alerts or UPI payment receipts). It parses the data and prompts you to categorize the expense—all without ever sending a single byte of your financial data to the cloud.

## ✨ Features

* **Automated Expense Capture:** Uses a background `NotificationListenerService` to instantly detect when you spend or receive money.
* **100% Offline & Private:** Built with a strict zero-internet policy. Your financial data never leaves your device. Everything is stored locally.
* **Frictionless Categorization:** Interact with expenses directly from your notification shade using `RemoteInput`. No need to open the app to log a transaction.
* **Smart Regex Parsing:** Context-aware text processing that accurately identifies transaction amounts and merchants while ignoring generic balance updates or OTPs.
* **Modern Android UI:** A sleek, dark-themed dashboard built entirely with Jetpack Compose, featuring a custom color palette of Deep Onyx, Cyber Lime, and Soft Terracotta.
* **Custom Onboarding:** A polished first-run experience to set your local currency, initial wallet balance, and grant necessary device permissions.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Local Storage:** * Room Database (Transactions & Categories)
  * Jetpack DataStore (User Preferences)
* **Asynchrony:** Kotlin Coroutines & Flow
* **Background Tasks:** `NotificationListenerService` & `BroadcastReceiver`

## 🚀 How It Works Under the Hood

1. **The Listener:** The app runs a highly optimized background service that listens for incoming notifications from banking and payment apps.
2. **The Filter:** It runs the notification text through a "Filter Pipeline" to ensure it contains transaction verbs (e.g., "debited", "paid") and excludes spam or balance updates.
3. **The Engine:** Using finely-tuned Regular Expressions (Regex), it extracts the currency symbol, the exact amount, and guesses the merchant name.
4. **The Prompt:** It fires a high-priority local notification back to the user, asking "What was this for?" with a direct text-reply field.
5. **The Vault:** Once the user types a category (e.g., "Food"), a `BroadcastReceiver` instantly saves the complete transaction entity to the local SQLite database via Room.

## 📱 Installation & Testing

Because **spenta** requires the `BIND_NOTIFICATION_LISTENER_SERVICE` permission, special steps are required for testing.

### Building from Source
1. Clone this repository: `git clone https://github.com/yourusername/spenta.git`
2. Open the project in Android Studio (Panda 2026.1 or newer).
3. Build and run the app on a **Physical Android Device** (Emulators often struggle with Notification Listeners).
4. **Crucial Step:** Upon first launch, navigate through the onboarding flow and click "Enable Notification Access". This will take you to Android Settings. You *must* toggle **spenta** to "ON" for the app to function.

### Installing via APK (Sideloading)
If you are downloading the latest `.apk` from the Releases tab:
1. Download the APK to your Android device.
2. Tap the file to install. If prompted, grant your browser/file manager permission to "Install unknown apps".
3. Open **spenta**, complete the setup, and grant Notification Access in your device settings.

## 🎨 Design System
**spenta** utilizes a custom Compose theme extending `darkColorScheme`:
* **Background:** Deep Onyx (`#121212`)
* **Primary Accent:** Cyber Lime (`#CCFF00`)
* **Secondary Accent:** Soft Terracotta (`#E2725B`)

## 🔒 Privacy Policy Breakdown
This application requires access to read your device notifications to function. 
* **Data Collection:** The app collects notification titles and text exclusively to identify financial transactions.
* **Data Storage:** All extracted data is stored locally in an encrypted SQLite database provided by the Android OS. 
* **Data Transmission:** This app does not declare the `android.permission.INTERNET` manifest permission. It is physically impossible for the app to transmit your data to external servers, third parties, or the developer. 

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
