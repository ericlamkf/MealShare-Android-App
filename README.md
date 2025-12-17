# 🍱 MealShare - Food Donation App

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat)](https://www.android.com)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg?style=flat)](https://www.java.com)
[![Backend](https://img.shields.io/badge/Backend-Firebase-yellow.svg?style=flat)](https://firebase.google.com/)
[![Course](https://img.shields.io/badge/Course-WIA2007-blue.svg?style=flat)]()

> **"Start Sharing Your Food Now."**

**MealShare** is a mobile application designed to bridge the gap between food donors and those in need. Aligned with **UN Sustainable Development Goal 2 (Zero Hunger)**, this platform allows users to easily list surplus food and allows others to locate nearby donations in real-time.

---

## 📱 App Screenshots

| Home Page | Add Donation | Registration |
|:---:|:---:|:---:|
| <img width="426" height="894" alt="Home Page" src="https://github.com/user-attachments/assets/3b516bb8-3ee4-4246-9247-cf03dc1133da" />
 | <img width="425" height="894" alt="Add Page" src="https://github.com/user-attachments/assets/b2063d1d-cd4a-41d9-b3c6-8dc25ca6d92f" />
 | <img width="447" height="895" alt="Register Page" src="https://github.com/user-attachments/assets/f5c56ac9-e1a5-4628-954b-40348cb36bd0" />
 |
| *View nearby donations* | *Upload food details* | *Create an account* |

*(Note: Please ensure you create a `screenshots` folder in your project root and add images named `home.png`, `add.png`, and `register.png`)*

---

## ✨ Key Features

* **🔐 Secure Authentication:** User registration and login using **Firebase Authentication**.
* **📍 Real-Time Location:** Automatic detection of user address using **Google Fused Location Provider** and Geocoding.
* **📸 Visual Listings:** "Add Food" interface allowing users to upload photos via **Gallery**, select tags (Halal, Vege, etc.), and set expiry times.
* **☁️ Cloud Integration:**
    * **Firestore Database:** Stores user profiles and food listing details.
    * **Firebase Storage:** Securely hosts profile pictures and food images.
* **📱 Modern UI:** Features a custom **Bottom Navigation**, **TabLayout** filters (All, Near Me, Ending Soon), and **Material Design** components.

---

## 🛠️ Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Java |
| **IDE** | Android Studio Ladybug/Koala |
| **Architecture** | MVVM (SharedViewModel for data passing) |
| **Backend** | Firebase (Authentication, Firestore, Storage) |
| **Key Libraries** | Glide, Google Play Services Location, Material Components |

---

## 💾 Database Structure (Firestore)

The app uses a NoSQL document-based structure.

### `users` Collection
Stores user profile information.
```json
{
  "uid": "user_unique_id",
  "name": "Eric Lam",
  "email": "eric@example.com",
  "username": "eric.lam",
  "profileImageUrl": "https://firebasestorage..."
}
