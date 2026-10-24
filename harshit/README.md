# CampusFix — College Complaint Management App

**CampusFix** is a native Android application built with **Jetpack Compose**, **Material 3**, and **Firebase** for college students to report campus issues (Classroom, Electricity, Water, Cleanliness, Other) and track resolution status in real time.

---

## 📱 Project Specifications

- **App Name:** CampusFix
- **Application ID:** `com.exampe.CampusFix`
- **Min SDK:** API 24 (Android 7.0)
- **Target / Compile SDK:** API 34 (Android 14)
- **UI Toolkit:** Jetpack Compose + Material 3
- **Backend:** Firebase Authentication & Cloud Firestore
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`)

---

## 🚀 Laptop 2 (Current Machine): Push to GitHub

To push this generated project to your GitHub repository:

1. Open PowerShell or Command Prompt in this folder (`c:\Users\CSE LAB G03 68\Desktop\harshit`).
2. Run the following commands (replace `<YOUR_GITHUB_REPO_URL>` with your repository URL):

```powershell
git init
git add .
git commit -m "Initial commit: CampusFix Android App"
git branch -M main
git remote add origin <YOUR_GITHUB_REPO_URL>
git push -u origin main
```

*(Note: `app/google-services.json` is already excluded in `.gitignore` to protect your credentials).*

---

## 💻 Laptop 1: Open in Android Studio & Build APK

### Step 1: Clone the Repository
On Laptop 1, open your terminal or Git Bash and clone the project:
```bash
git clone <YOUR_GITHUB_REPO_URL>
```

### Step 2: Add `google-services.json`
Download your Firebase Android configuration file from the [Firebase Console](https://console.firebase.google.com/):
- Package name must match: **`com.exampe.CampusFix`**
- Place the downloaded file into the `app/` folder:
  ```
  CampusFix/
    └── app/
          └── google-services.json  <-- Place file here
  ```

### Step 3: Firebase Console Setup
1. **Enable Authentication:**
   - Go to **Firebase Console** → **Authentication** → **Sign-in method**.
   - Enable **Email/Password**.
2. **Enable Firestore Database:**
   - Go to **Firebase Console** → **Firestore Database** → **Create database**.
   - Choose production mode or test mode.
3. **Deploy Security Rules:**
   - In Firestore Database, click the **Rules** tab.
   - Copy and paste the contents of `firestore.rules` (included in this repository) and click **Publish**.
4. **Create Admin User:**
   - In Authentication → **Users** tab, click **Add user**.
   - Email: `admin@campus.edu`
   - Password: `admin123` (or your preferred password set in `AdminConfig.kt`).

### Step 4: Open in Android Studio
1. Launch **Android Studio**.
2. Select **File → Open...** and choose the `CampusFix` root folder.
3. Let Gradle sync automatically. (Ensure you have an active internet connection on the first sync to download dependencies).

### Step 5: Build the APK
1. In Android Studio, click **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. Once the build completes, a popup appears: click **locate** to find the APK file:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
3. Transfer this `.apk` to any Android phone or test directly on an emulator!

---

## 🛠️ Application Architecture

```
CampusFix/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/exampe/campusfix/
│   │       │   ├── MainActivity.kt        # All UI screens (Login, Dashboard, Form, List, Admin)
│   │       │   ├── FirebaseRepository.kt  # Clean Auth & Firestore operations
│   │       │   ├── Models.kt              # Complaint, Categories, and Status models
│   │       │   └── AdminConfig.kt         # Admin credentials and verification logic
│   │       ├── res/                       # Theme colors, strings, and vector icons
│   │       └── AndroidManifest.xml        # Permissions and Activity declaration
│   ├── build.gradle.kts                   # App-level dependencies (Compose, Firebase)
│   └── proguard-rules.pro                 # Firestore ProGuard preservation rules
├── build.gradle.kts                       # Root build configuration
├── settings.gradle.kts                    # Repositories & project include
├── gradle.properties                      # JVM & AndroidX settings
├── firestore.rules                        # Firebase Firestore security rules
├── .gitignore                             # Git exclusion for build artifacts and credentials
└── README.md
```

---

## 👥 User Roles & Demo Credentials

### 1. Student Role
- **Register / Sign In:** Use any student email (e.g. `student@campus.edu`) with a password of at least 6 characters.
- **Features:**
  - View real-time personal complaint statistics.
  - Submit issues with Category (Classroom, Electricity, Water, Cleanliness, Other), Location, and Description.
  - View personal complaints list with live status badges (`Pending`, `In Progress`, `Resolved`).
  - Protected by security rules: Students cannot see other students' complaints or edit status.

### 2. Admin Role
- **Credentials:**
  - Email: `admin@campus.edu`
  - Password: `admin123`
  - *(Click the "Fill Admin Demo Credentials" button on the login screen for instant autofill!)*
- **Features:**
  - View all complaints submitted across the entire college.
  - Filter by status (`All`, `Pending`, `In Progress`, `Resolved`).
  - One-tap status update dropdown (updates Firestore directly).
