🍱 MealShare - Food Donation App
"Start Sharing Your Food Now."

MealShare is a mobile application designed to bridge the gap between food donors and those in need. Aligned with UN Sustainable Development Goal 2 (Zero Hunger), this platform allows users to easily list surplus food and allows others to locate nearby donations in real-time.

✨ Key Features
🔐 Secure Authentication: User registration and login using Firebase Authentication.

📍 Real-Time Location: Automatic detection of user address using Google Fused Location Provider and Geocoding.

📸 Visual Listings: "Add Food" interface allowing users to upload photos via Gallery, select tags (Halal, Vege, etc.), and set expiry times.

☁️ Cloud Integration:

Firestore Database: Stores user profiles and food listing details.

Firebase Storage: Securely hosts profile pictures and food images.

📱 Modern UI: Features a custom Bottom Navigation, TabLayout filters (All, Near Me, Ending Soon), and Material Design components.

🛠️ Tech Stack
Language: Java

IDE: Android Studio Ladybug/Koala

Architecture: MVVM (SharedViewModel for data passing)

Backend: Firebase (Auth, Firestore, Storage)

Key Libraries:

Glide: For image loading and caching.

Google Play Services Location: For GPS functionality.

Material Components: For Chips, Cards, and Bottom Sheets.

💾 Database Structure (Firestore)
The app uses a NoSQL document-based structure:

users Collection
Stores user profile information.

JSON
```
{
  "uid": "user_unique_id",
  "name": "Eric Lam",
  "email": "eric@example.com",
  "username": "eric.lam",
  "profileImageUrl": "https://firebasestorage..."
}
meals Collection
Stores individual food donations.
```
JSON
```
{
  "mealId": "auto_generated_id",
  "foodName": "Nasi Lemak",
  "quantity": "5 packs",
  "location": "17, SS 2/73, Petaling Jaya",
  "tags": ["Halal", "Hot Meal"],
  "expiryTime": "Timestamp",
  "imageUrl": "https://firebasestorage..."
}
```
🚀 Getting Started
To run this project locally, follow these steps:

1. Prerequisites
Android Studio installed.

A physical Android device or Emulator (API 24+ recommended).

2. Installation
Clone the repo:
```
git clone https://github.com/YourUsername/MealShare.git
```
Open in Android Studio.

3. Firebase Setup (Crucial!)
This project relies on Firebase. You must provide your own configuration file.

Create a project on the Firebase Console.

Enable Authentication (Email/Password), Firestore, and Storage.

Download the google-services.json file.

Paste the file into the app/ directory of the project:

```
MealShare/
├── app/
│   ├── google-services.json  <-- Place it here
│   ├── src/
├── build.gradle
```

Sync Gradle and Run the app!

🤝 Contribution
This project was developed for the WIA2007 Mobile Application Development course.

Developer: Eric Lam Kah Fai

Team Members: Aida, Huizhe, Haoyang, Desmond, Wei Shen
