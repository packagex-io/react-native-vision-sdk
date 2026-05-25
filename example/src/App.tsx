import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import HomeScreen from './HomeScreen';
import VisionCameraExample from './VisionCameraExample';
import ModelManagementScreen from './ModelManagementScreen';
import DimensioningScreen from './DimensioningScreen';

const Stack = createNativeStackNavigator();

const App = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="HomeScreen">
        <Stack.Screen name="HomeScreen" component={HomeScreen} options={{ title: 'Vision SDK Home' }} />
        <Stack.Screen name="VisionCameraExample" component={VisionCameraExample} options={{ headerShown: false }} />
        <Stack.Screen name="ModelManagementScreen" component={ModelManagementScreen} options={{ title: 'Model Management API' }} />
        <Stack.Screen name="DimensioningScreen" component={DimensioningScreen} options={{ title: 'Dimensioning (iOS)' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default App;
