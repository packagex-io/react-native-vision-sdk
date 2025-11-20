# Vision SDK Example - Old Architecture

This example app tests the **React Native Vision SDK** with the **Old Architecture** (Paper renderer).

## Configuration

- **React Native Version**: 0.76.6
- **Architecture**: Old Architecture (Paper)
- **New Architecture**: Disabled

## Setup

```bash
# Install dependencies
yarn install

# iOS only - Install pods
cd ios && pod install && cd ..

# Run on Android
yarn android

# Run on iOS
yarn ios
```

## Testing

This app uses the same example code as the main `example/` directory but runs on React Native 0.76.6 with the old architecture to ensure backward compatibility.

### Components Tested

1. **VisionCamera** - Simplified camera component
2. **VisionSdk** - Full-featured SDK component

Both components should work identically to the new architecture version.

## Key Differences from New Architecture

- Uses Paper renderer instead of Fabric
- No codegen specs
- Props are passed directly without JSON conversion
- React Native 0.76.6 instead of 0.82.1

The SDK automatically detects which architecture is being used and adapts accordingly.
