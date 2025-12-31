# IR Blaster - Android Universal Remote App

A comprehensive Android IR Blaster application that turns your phone into a universal remote control for TVs and other IR-controlled devices.

## Features

### 🔬 Samsung IR Tester (Offline)
- Pre-loaded Samsung TV IR codes
- 12 power code variations for different Samsung TV models
- Auto-cycle mode to find the correct code
- Test buttons: Power, Volume, Channel, Mute, Source

### 🌐 Browse All Brands (Online - IRDB API)
- Fetches codes from [IRDB](https://github.com/probonopd/irdb) - open source IR database
- 500+ brands supported (Samsung, LG, Sony, Panasonic, Philips, Vizio, TCL, Hisense, Sharp, etc.)
- Browse by: Brand → Device Type (TV, DVD, AC) → Remote Model
- Download codes on-demand
- Test and add working codes to your custom remote

### 📱 Custom Remote Builder
- Create multiple custom remotes
- Save working IR codes with custom names and emojis
- Room database for offline persistence
- Edit button names and emojis
- Frequency logging for debugging

### 🗂️ Navigation Drawer
- Quick access to IR Tester
- Browse All Brands
- List of saved custom remotes
- Create new remote

## Supported IR Protocols

| Protocol | Brands | Frequency |
|----------|--------|-----------|
| NEC | Samsung, LG, Toshiba, Vizio, TCL, Hisense | 38 kHz |
| Sony SIRC | Sony | 40 kHz |
| RC5/RC6 | Philips | 36 kHz |
| Samsung32 | Samsung | 38 kHz |
| Panasonic | Panasonic | 37 kHz |

## Requirements

- Android device with IR Blaster hardware (e.g., Xiaomi, Huawei, Samsung Galaxy S6 and earlier)
- Android 7.0 (API 24) or higher
- Internet connection (for Browse All Brands feature)

## Permissions

```xml
<uses-permission android:name="android.permission.TRANSMIT_IR" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-feature android:name="android.hardware.consumerir" android:required="false" />
```

## Tech Stack

- **Kotlin** - 100%
- **Jetpack Compose** - Modern UI toolkit
- **Room Database** - Local storage for custom remotes
- **Navigation Compose** - Screen navigation
- **Material 3** - Design system
- **Coroutines** - Async operations

## Project Structure

```
app/src/main/java/com/example/irblaster/
├── MainActivity.kt                 # Entry point
├── MainApp.kt                      # Navigation & drawer
├── data/
│   ├── RemoteDatabase.kt           # Room entities, DAOs, database
│   ├── RemoteRepository.kt         # Data repository
│   ├── IRCodeDatabase.kt           # Local IR codes
│   └── api/
│       └── IRDBApiClient.kt        # IRDB API client
├── navigation/
│   └── Screen.kt                   # Navigation routes
└── ui/
    ├── screens/
    │   ├── IRTesterScreen.kt       # Samsung IR tester
    │   ├── BrandBrowserScreen.kt   # Online brand browser
    │   ├── CustomRemoteScreen.kt   # Custom remote view
    │   ├── CreateEditRemoteScreen.kt
    │   └── EditButtonScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## How to Use

1. **Test Samsung TV**: Use the IR Tester to find a working power code for your Samsung TV
2. **Browse Other Brands**: Use "Browse All Brands" to find codes for other devices
3. **Save Working Codes**: Tap "Add to My Remote" when you find a working code
4. **Create Custom Remote**: Organize your buttons in custom remotes
5. **Use Your Remote**: Open saved remotes from the drawer and tap buttons to control devices

## Building

```bash
./gradlew assembleDebug
```

## API Sources

The app supports multiple IR code databases with automatic fallback:

- **IRDB**: 
  - Primary: https://github.com/simon-weber/irdb (actively maintained)
  - Fallback: https://github.com/probonopd/irdb
  
- **Flipper IRDB**:
  - Primary: https://github.com/Lucaslhm/Flipper-IRDB (Flipper Zero community)
  - Fallback: https://github.com/UberGuidoZ/Flipper-IRDB

- **LIRC**:
  - Primary: https://github.com/probonopd/lirc-remotes
  - Fallback: https://github.com/lirc-remotes/lirc-remotes

Uses GitHub API to fetch brand listings and IR code files with automatic fallback to alternative mirrors if primary fails.

## License

MIT License

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

