# Implementation Plan - Global Font Consistency (Inter)

Ensure that the `Inter` font is used consistently across all devices by applying it at the theme level and removing redundant manual applications.

## Proposed Changes

### [Theme Component]

#### [Type.kt](file:///C:/Android/AndroidStudioProjects/spenta1/app/src/main/java/com/abhishekhjs/spenta/ui/theme/Type.kt)
- Verify and ensure all Material3 typography styles (display, headline, title, body, label) are explicitly set to use `Inter`.

#### [Theme.kt](file:///C:/Android/AndroidStudioProjects/spenta1/app/src/main/java/com/abhishekhjs/spenta/ui/theme/Theme.kt)
- Update `SpentaTheme` to provide `LocalTextStyle` with `Inter` as the default font family using `CompositionLocalProvider`. This ensures that even components that don't specify a style (like `BasicTextField` or custom `Text` calls) will use `Inter`.

```kotlin
@Composable
fun SpentaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        // Ensure Inter is the default font for all text
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Inter)
        ) {
            content()
        }
    }
}
```

### [UI Cleanup]

#### All Screens and Components
- Remove redundant `fontFamily = Inter` from all `Text`, `TextField`, `Button`, and other UI components.
- This will be done systematically across:
    - `MainActivity.kt`
    - `HomeScreen.kt`
    - `SpendingsScreen.kt`
    - `SettingsScreen.kt`
    - `OnboardingScreen.kt`
    - `SplitBillScreen.kt`
    - `CategorySettingsScreen.kt`
    - `EditTransactionDialog.kt`
    - `TransactionRow.kt`

## Verification Plan

### Automated Tests
- N/A (UI appearance is hard to test automatically without screenshot testing)

### Manual Verification
- **Visual Audit**: Run the app and check all screens to ensure the `Inter` font is used consistently.
- **System Font Change**: Change the system font on the device/emulator and verify that the app's font remains `Inter`.
- **Check Specific Components**: Pay close attention to components that were previously missing `Inter` (discovered during research), such as:
    - "NEARBY EXPLORERS" header in `SplitBillScreen`.
    - Input text in `TextField`s.
    - Labels in `NavigationBarItem`.
