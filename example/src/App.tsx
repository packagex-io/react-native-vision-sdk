/**
 * VisionSDK Example App — rebuilt 2026-06-04.
 *
 * Boots straight into ScannerScreen (no home menu), matching the native demos.
 * Dark theme with yellow (#FFD60A) accent throughout.
 *
 * Screens:
 *   ScannerScreen     — main persistent scanner (all modes, OCR results, model management)
 *   DimensioningScreen — iOS-only ARKit/LiDAR dimensioning (navigated from mode picker)
 */
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ScannerScreen } from './screens/ScannerScreen';
import { DimensioningScreen } from './screens/DimensioningScreen';
import { CameraControlsQAScreen } from './screens/CameraControlsQAScreen';

const Stack = createNativeStackNavigator();

const App = () => {
  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <Stack.Navigator
          initialRouteName="ScannerScreen"
          screenOptions={{ headerShown: false }}
        >
          <Stack.Screen name="ScannerScreen" component={ScannerScreen} />
          <Stack.Screen name="DimensioningScreen" component={DimensioningScreen} />
          <Stack.Screen
            name="CameraControlsQAScreen"
            component={CameraControlsQAScreen}
            // The iOS interactive swipe-back competes with the zoom slider's
            // left-edge drags — back out via the header arrow instead.
            options={{ gestureEnabled: false }}
          />
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
};

export default App;
