import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import HomeScreen from './HomeScreen';
import CameraScreen from './CameraScreen';
import VisionCameraExample from './VisionCameraExample';

const Stack = createNativeStackNavigator();

const App = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="HomeScreen">
        <Stack.Screen name="HomeScreen" component={HomeScreen} options={{ title: 'Vision SDK Home' }} />
        <Stack.Screen name="CameraScreen" component={CameraScreen} options={{ title: 'Camera' }} />
        <Stack.Screen name="VisionCameraExample" component={VisionCameraExample} options={{ headerShown: false }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default App;
