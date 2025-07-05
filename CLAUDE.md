# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Muse is a text-to-speech Android application built with Kotlin Multiplatform and Jetpack Compose. It integrates with ElevenLabs API for TTS and audio isolation features. The app allows users to create scripts, convert text to speech using various voices, and export audio files.

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug version on device
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing
```bash
# Run all tests
./gradlew test

# Run Android tests
./gradlew connectedAndroidTest
```

### Code Quality
```bash
# Check code style (if configured)
./gradlew ktlintCheck

# Format code (if configured)
./gradlew ktlintFormat
```

## Architecture

### Module Structure
- **composeApp**: Main Android application module containing all UI and business logic
- **elevenlabs**: Separate module for ElevenLabs API integration (TTS and audio isolation)

### Key Components

#### Dependency Injection
- Uses Koin for dependency injection
- Main DI configuration in `App.kt` with `appModule`
- ViewModels, repositories, and providers are registered here

#### Navigation
- Uses Jetpack Navigation Compose with type-safe navigation
- Bottom sheet navigation support via Accompanist
- Deep link support for sharing text content
- Main navigation structure in `MainScreen.kt`

#### Data Layer
- **MuseRepo**: Central repository for data operations
- **SqlDelight**: Database layer for local data persistence
- **AccountManager**: Handles user account and API key management

#### TTS Integration
- **TTSProvider**: Abstract interface for TTS services
- **ElevenLabProvider**: Concrete implementation using ElevenLabs API
- **TTSManager**: Orchestrates TTS operations

#### Audio Processing
- **AudioIsolationProvider**: Interface for audio isolation features
- **Mp3Encoder/Mp3Decoder**: Audio format conversion utilities
- **WavParser**: WAV file parsing and manipulation

### Screen Architecture
Each major screen follows MVVM pattern:
- **Screen**: Composable UI layer
- **ViewModel**: Business logic and state management
- **Args**: Type-safe navigation arguments

Key screens:
- `DashboardScreen`: Main landing page with script management
- `EditorScreen`: Text editing and voice selection
- `ExportScreen`: Audio export with progress tracking
- `AudioIsolationScreen`: Audio isolation processing
- `SettingScreen`: App configuration and voice management

### Build Configuration
- **Kotlin Multiplatform**: Shared code between platforms (currently Android-focused)
- **Compose**: Modern UI toolkit
- **Version Catalogs**: Centralized dependency management in `gradle/libs.versions.toml`
- **ProGuard**: Code obfuscation and optimization for release builds
- **NDK**: Native library support (arm64-v8a for release, x86_64 for debug)

## API Integration

### ElevenLabs
- API key management through AccountManager
- TTS generation with voice selection
- Audio isolation features
- Error handling with custom exception types

### Data Models
- Kotlin serialization for API responses
- Generated models in `elevenlabs/model/` directory
- SqlDelight models for local database

## Key Development Notes

- Debug builds include StrictMode and additional dev tools
- Crash logging integrated with XCrash
- Dynamic shortcuts for crash log access
- Deep link support for text sharing from other apps
- Audio file management with MediaStore integration
- Background export processing with progress updates

## Testing Setup

The project uses standard Android testing approaches:
- Unit tests for business logic
- UI tests for Compose screens
- Integration tests for API interactions