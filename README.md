# KitchenRatio 🍳⚖️

**KitchenRatio** is a modern, feature-rich Android application built with **Jetpack Compose** and **Kotlin**, designed to help home cooks, bakers, and culinary enthusiasts effortlessly scale recipes, convert ingredient quantities, and manage their favorite culinary creations.

---

## 🌟 Key Features

- **Smart Recipe Scaling**: 
  - Dynamically scale ingredient quantities up or down based on desired servings, yields, or specific ingredient scaling factors.
  - Automatic parsing and formatting of quantities (fractions, decimals, and units).
- **Saved Recipes Manager**:
  - Save, organize, and quickly load your custom recipes.
  - Persistent storage powered by **Jetpack DataStore** and **kotlinx.serialization**.
- **Intuitive Calculator & Tools**:
  - Quick calculators for ingredient ratios and conversions.
  - Clean, distraction-free interface tailored for kitchen environments.
- **Modern Material 3 Design**:
  - Fully built with **Jetpack Compose** and **Material 3** guidelines.
  - Seamless light and dark mode support with dynamic color styling.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with unidirectional data flow (UDF)
- **Navigation**: Jetpack Navigation Compose
- **State & Lifecycle**: ViewModel, StateFlow, Lifecycle Runtime Compose
- **Persistence**: Jetpack DataStore (Preferences) + `kotlinx.serialization`
- **Testing**: JUnit 4, Kotlin Coroutines Test, Espresso, Compose UI Testing

---

## 📱 Screenshots & UI

*(Add screenshots of your app here)*

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Koala / Ladybug or newer recommended)
- Android SDK (compileSdk & targetSdk 37, minSdk 24)
- JDK 11 or higher

### Installation & Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/KitchenRatio.git
   ```
2. Open the project in **Android Studio**.
3. Sync project with Gradle files.
4. Run the app on an Android emulator or physical device.

---

## 🧪 Testing

The project includes unit tests for core domain logic (ratio calculations, quantity parsing/formatting) and data persistence:

- Run unit tests:
  ```bash
   ./gradlew test
   ```
- Run instrumented tests:
  ```bash
   ./gradlew connectedAndroidTest
   ```

---

## 📄 License

This project is developed by **Abdullah Ateeq**. 

---
*Happy Cooking & Baking! 🥖🍰*
