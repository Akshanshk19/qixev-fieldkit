# Android & Compose Implementation Standards

This document establishes the UI and Android platform development standards for **Qixev FieldKit**.

---

## 1. Material 3 & Material You Theming

1. **Dynamic Color by Default**:
   - On Android 12+ (API 31+), the app automatically extracts user wallpaper color tones via `dynamicLightColorScheme(context)` and `dynamicDarkColorScheme(context)`.
   - On earlier Android versions (API 26?30), the theme seamlessly falls back to the balanced slate-blue neutral palette in `Color.kt`.
2. **Surface Containers & Elevation**:
   - Avoid legacy Material 2 drop shadows and harsh black outlines.
   - Use Material 3 tonal elevation and surface containers (`surfaceContainer`, `surfaceContainerLow`, `surfaceContainerHigh`, `surfaceContainerHighest`).
3. **Card & Dialog Styling**:
   - Dialogs: `RoundedCornerShape(28.dp)`.
   - Cards: `RoundedCornerShape(16.dp)`.
   - Search bars: `RoundedCornerShape(24.dp)` with `surfaceContainerHigh` background and transparent indicator borders.
   - Action Badges / Chips: `RoundedCornerShape(8.dp)` or standard Material 3 `FilterChip`.

---

## 2. Insets & Edge-to-Edge Layout

1. **Root Window Insets**:
   - `MainActivity.kt` executes `enableEdgeToEdge()`.
   - To prevent double status-bar top padding, the root `Scaffold` must define:
     ```kotlin
     Scaffold(
         modifier = Modifier.fillMaxSize(),
         contentWindowInsets = WindowInsets(0, 0, 0, 0),
         bottomBar = { ... }
     )
     ```
   - Pass only `innerPadding.calculateBottomPadding()` to `FieldKitNavHost` so child screens' `TopAppBar`s can handle their own top insets cleanly.
2. **Soft Keyboard Inset Handling**:
   - The main activity must declare:
     ```xml
     android:windowSoftInputMode="adjustResize|stateHidden"
     ```
   - This ensures the virtual keyboard does not automatically pop up on screen entry or launch, while still allowing the UI to resize when text fields are explicitly tapped.

---

## 3. Navigation & Bottom Bar

1. **Single NavHost**:
   - All screens are managed inside `FieldKitNavHost.kt`.
   - Destinations are defined in `Screen.kt`.
2. **Bottom Navigation Bar Visibility**:
   - The bottom bar appears only on root destinations: `Screen.RepairJobs.route`, `Screen.Inventory.route`, `Screen.Settings.route`.
   - Onboarding and detail screens must cleanly hide the bottom navigation bar.
3. **Back Navigation & State Preservation**:
   - Bottom navigation items must pop up to `findStartDestination()` with `saveState = true`, `launchSingleTop = true`, and `restoreState = true`.
   - Onboarding completion must pop the onboarding destination with `popUpTo(Screen.Onboarding.route) { inclusive = true }` so pressing back from the main screen exits the app instead of looping back to onboarding.

---

## 4. Compose Performance Best Practices

1. **Recomposition Hygiene**:
   - Avoid passing unstable lambdas or creating new non-memoized objects inside composable loops.
   - Use `remember { SimpleDateFormat(...) }` for date formatters.
   - Use `key = { it.id }` in `items()` inside `LazyColumn` and `LazyRow`.
2. **State Hoisting**:
   - Keep components stateless by hoisting state up to the screen level or ViewModel.
3. **Touch Targets**:
   - All interactive elements must maintain a minimum touch target size of 48x48 dp (`Modifier.defaultMinSize(minHeight = 48.dp)` or standard Material 3 button dimensions).
