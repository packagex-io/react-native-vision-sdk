import React, { useEffect, useState } from 'react';
import { View, StyleSheet, Platform, Alert, Vibration } from 'react-native';
import VisionSdkView from 'react-native-vision-sdk';
import CameraFooterView from './Components/CameraFooterView';
import DownloadingProgressView from './Components/DownloadingProgressView';
import CameraHeaderView from './Components/CameraHeaderView';
import LoaderView from './Components/LoaderView';
import ResultView from './Components/ResultView';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';

interface downloadingProgress {
  downloadStatus: boolean;
  progress: number;
}
interface detectedDataProps {
  barcode: boolean;
  qrcode: boolean;
  text: boolean;
  document: boolean;
}
export default function App() {
  const visionSdk = React.useRef<any>(null);
  const [captureMode, setCaptureMode] = useState<'manual' | 'auto'>('manual');
  const [isOnDeviceOCR, setIsOnDeviceOCR] = useState<boolean>(false);
  const [modelSize, setModelSize] = useState<string>('large');
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<any>('');
  const [mode, setMode] = useState<any>('ocr');
  const [flash, setFlash] = useState<boolean>(false);
  const [detectedData, setDetectedData] = useState<detectedDataProps>({
    barcode: false,
    qrcode: false,
    text: false,
    document: false,
  });

  const handleCameraPress = async () => {
    try {
      let cameraPermission;
      if (Platform.OS === 'ios') {
        cameraPermission = PERMISSIONS.IOS.CAMERA;
      } else {
        cameraPermission = PERMISSIONS.ANDROID.CAMERA;
      }

      const result = await request(cameraPermission);

      if (result === RESULTS.GRANTED) {
        return true;
      } else {
        console.log('Camera Permission Error');

        Alert.alert(
          'Camera Permission Error',
          'App needs camera permission to take pictures. Please go to app setting and enable camera permission.'
        );
        return false;
      }
    } catch (error) {
      console.log('Error asking for camera permission', error);
    }
  };

  const [modelDownloadingProgress, setModelDownloadingProgress] =
    useState<downloadingProgress>({
      downloadStatus: true,
      progress: 0,
    });

  React.useEffect(() => {
    if (Platform.OS === 'android') {
      handleCameraPress();
    }
  }, []);

  React.useEffect(() => {
    // visionSdk?.current?.setHeight(1);
    visionSdk?.current?.setFocusSettings({
      // focusImage: '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ANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAANCQAgAADSkAh+9s4LlvDyCf3qrPv+p8uJICcPjeDDz39QHk01v1+VedD1dSAA7fycBznx9APr1Vn3/V+TDk/eDRXfX63U9yvkXueZJPDyC/ev3lO/+d/3X5S1e+fuUDLNwc1u/ZFrlPJsquzq9ef/nOf+d/7c9/ycrXr3yAhZvD+h0lebFB5kmSjybKrs6vXn/5zn/nf+3Pf8nK1698gIWby/odJXma629Hnmf1L48pL37V+dXrL9/5X5lfvf7V+UtXvn7lAyzc3NbvXpKvkvyY1UeUzi7/+8tM85rj3PKr11++878yv3r9q/OXrnz9ygf4wHFWLfk0ybvL4zTJ46zecDM3c1u/bqrXX77zv1L1+lfnT23f+0/5+pUPcOlOkm+SXFyTdZ7VG26OJswdNZf166p6/eU7/ytVr391/lSq9p/y9SsfIKvFf7lB5g+ZTwmYw/p1Vr3+8p3/larXvzp/CpX7T/n6lQ+Q5Nstcp9OlD1qDuvXWfX6y3f+V6pe/+r8KVTuP+XrVz3Aca6/7XLVcZ7VG26qVa9fd9XrL9/5X6l6/avzR1XvPztdvyV8FfCjbDfn7SQPJ54FgD4Oev9ZQgH4YuC5DyabAoBuDnr/ubXGY0Zvw6yTcZ2zJHcHnvvxYP6o6vXrrnr95dfmd1e9/tX5o6r3n52u3xLuAGy7+Eny28mmOBzV36NQnU9v1edfdT6bab//VL+Jozp/1Fzmr/4eha6fo5Xv/K/Mr17/6vxR1fNX55cPUJ0/ag7zV3+PQufP0cp3/jv/a3/+I6rnr84vH6A6f9Qc5q/+HoXOn6OV7/x3/tf+/EdUz1+dXz5Adf6o6vmrP8danV+9/vKd/87/uvxR1fPvNH8JbwJkTPXnWKvz6a36/KvOhyspAIev+nOs1fn0Vn3+VefDlZbwPQDV+aOq56/+HGt1fvX6y6/Nrz7/qvOr1786f1T1/DvNVwB2r3p++fLly++aP6p6/p3mewkAABpSAACgIQUAABpSAACgIQUAABpSAACgIQUAABpSAHbvbOC5b+XLly9fPrugAOzem4HnvpYvX758+eyCArB7JwPPfS5fvnz58qly0L8OcQ/uZ/WrPTed+zzJp/Lly5cvv8ymc394LD2/fIDq/Ck8y+ZzP5EvX758+aU2nfvDY+n55QNU50/hKMmLrD/zSZKP5MuXL19+qer9pzq/fIDq/KkcJXma62+HnWfVfHdx8suXL19+1/xtVe8/1fnlA1TnT+1ekq+S/JjVR2TOLv/7y+znNS/58uXL75q/qer9Z6f56/yu4tG/xKx/HzIAXKF6/9lpvo8BAkBDCgAANKQAAEBDCgAANKQAAEBDCgAANKQAAEBDCgAANKQAAEBDCgAANKQAAEBDCgAANKQAAEBDCgAANKQAAEBDHQrA+5kf75KcJnmc5P6O1gBgDo6TPMnqmvcu9dffm472qhew+gTY53Ge5FmSownWDWAu7iT5JslF6q+z+zxG7TT/1poDjFgnY5f5S/QyyR+T/Fo9CMCgO0n+kuQP1YMUqN7/rs3v8BLAEv1Lki+rhwCYwOP03Pxnzx2A+bpI8vskP1UPArCl4ySv0vcfm9X7nzsAC3U7ycPqIQAGPIp9Zrb8YObtQfUAAAO+qB6Aq3kJYN7OknxcPQTAls6S3K0eolD1/rf4lwDOqgco1Ln8AMvX+Rr2tnqAmyyhALypHqDQ6+oBAAa4fs/YEgrASfUAhZ5XDwAwwPV74aq/Cel+Vt+QV/2NTvs+zpN8OsH6AVRx/R4zOsfyB8jq63Grf6D7Pp5MsnIAtVy/tzc6x/IHyOq78V9MMMtSjpMkH02ycgC1XL+3NzrL8ge4dJTkaQ77dtJ5Vs3R5g8cEtfv7ZTvv+UDfOBekq+S/JjVRwSrf+ijx9nl3+XLeM0fOGyu35vZ6f67hC8CAoCOdrr/LuFjgADAxBQAAGhIAQCAhhQAAGhIAQCAhhQAAGhIAQCAhhQAAGhIAQCAhhQAAGhIAQCAhhQAAGhIAQCAhhQAAGhIAQCAhn6zh4zR32cMAEzMHQAAaEgBAICGFAAAaEgBAICGFAAAaEgBAICGFAAAaGidAnC28ykAgCm9vekB6xSANxMMAgDsz+ubHrBOATiZYBAAYH+e3/SAW2v8IfeTvEpye3gcAGDXLpJ8luTn6x60zh2A0yTfTTERALBzX+eGzT9Z7w5Akhwl+XOSz0cmAgB26kWSPyX5200PXPe2/kWS/0nyuyT/HB8fBIA5uUjyLMm/ZY3NP1n/DsD/dS/JwyQPknyS5O4WfwYAMOZdkl+yesPf91njtj8AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPwvfpjZUROZLAIAAAAASUVORK5CYII=',
      // focusImageRect: { x: 50.0, y: 50.0, width: 250.0, height: 100.0 },
      shouldDisplayFocusImage: true,
      shouldScanInFocusImageRect: true,
      showCodeBoundariesInMultipleScan: true,
      validCodeBoundaryBorderColor: '#2abd51',
      validCodeBoundaryBorderWidth: 2,
      validCodeBoundaryFillColor: '#2abd51',
      inValidCodeBoundaryBorderColor: '#cc0829',
      inValidCodeBoundaryBorderWidth: 2,
      inValidCodeBoundaryFillColor: '#cc0829',
      showDocumentBoundaries: true,
      documentBoundaryBorderColor: '#241616',
      documentBoundaryFillColor: '#e30000',
      focusImageTintColor: '#ffffff',
      focusImageHighlightedColor: '#e30000',
    });
    visionSdk?.current?.setObjectDetectionSettings({
      isTextIndicationOn: true,
      isBarCodeOrQRCodeIndicationOn: true,
      isDocumentIndicationOn: true,
      codeDetectionConfidence: 0.5,
      documentDetectionConfidence: 0.5,
      secondsToWaitBeforeDocumentCapture: 2.0,
      // selectedTemplateId: '1',
    });
    visionSdk?.current?.setCameraSettings({
      nthFrameToProcess: 10,
      // shouldAutoSaveCapturedImage: true,
    });

    visionSdk?.current?.startRunningHandler();
    setLoading(false);
  }, [captureMode]);

  const onPressCapture = () => {
    // if (Platform.OS === 'android') {
    setLoading(true);
    // }
    visionSdk?.current?.cameraCaptureHandler();
  };
  const toggleFlash = (val: boolean) => {
    setFlash(val);
  };
  function isMultipleOfTen(number: any) {
    return number % 1 === 0;
  }
  useEffect(() => {
    if (isOnDeviceOCR) {
      onPressOnDeviceOcr();
    }
  }, [isOnDeviceOCR]);

  useEffect(() => {
    if (flash) {
      toggleFlash(flash);
    }
  }, [flash]);

  const onPressOnDeviceOcr = (type = 'shipping_label', size = 'large') => {
    visionSdk?.current?.stopRunningHandler();
    setLoading(true);
    visionSdk?.current?.configureOnDeviceModel({
      type: type,
      size: size,
    });
  };
  return (
    // <View style={styles.mainContainer}>
    //   <VisionSdkView
    //     refProp={visionSdk}
    //     isOnDeviceOCR={isOnDeviceOCR}
    //     // showScanFrame={true}
    //     // showDocumentBoundaries={true}
    //     // captureWithScanFrame={true}
    //     captureMode={captureMode}
    //     mode={mode}
    //     environment="sandbox"
    //     apiKey="key_141b2eda27Z0Cm2y0h0P6waB3Z6pjPgrmGAHNSU62rZelUthBEOOdsVTqZQCRVgPLqI5yMPqpw2ZBy2z"
    //     flash={flash}
    //     onDetected={(e: any) => {
    //       setDetectedData(Platform.OS === 'android' ? e : e.nativeEvent);
    //     }}
    //     onBarcodeScan={(e: any) => {
    //       console.log('BarCodeScanHandler', e);
    //       setLoading(false);
    //       visionSdk?.current?.restartScanningHandler();
    //     }}
    //     onOCRScan={(e: any) => {
    //       let scanRes = Platform.OS === 'ios' ? e.nativeEvent.data.data : e;
    //       if (Platform.OS === 'android') {
    //         const parsedOuterJson = JSON.parse(scanRes.data);
    //         scanRes = parsedOuterJson.data;
    //       }
    //       setResult(scanRes);
    //       setLoading(false);
    //       Vibration.vibrate(100);
    //       // setTimeout(() => {
    //       visionSdk?.current?.restartScanningHandler();
    //       // }, 200);
    //     }}
    //     onImageCaptured={(e: any) => {
    //       console.log('onImageCaptured==------>>', e);
    //     }}
    //     onModelDownloadProgress={(e: any) => {
    //       let response = Platform.OS === 'android' ? e : e.nativeEvent;
    //       console.log(
    //         'ModelDownloadProgress==------>>',
    //         Math.floor(response.progress * 100)
    //       );
    //       if (isMultipleOfTen(Math.floor(response.progress * 100))) {
    //         setModelDownloadingProgress(response);
    //         if (response.downloadStatus) {
    //           visionSdk?.current?.startRunningHandler();
    //         }
    //       }
    //       setLoading(false);
    //     }}
    //     onError={(e: any) => {
    //       let error = Platform.OS === 'android' ? e : e.nativeEvent;
    //       console.log('onError', error);
    //       Alert.alert('ERROR', error?.message);
    //       setLoading(false);
    //     }}
    //   />
    //   <ResultView
    //     visible={result ? true : false}
    //     result={result}
    //     setResult={setResult}
    //   />
    //   <LoaderView visible={loading} />
    //   <CameraHeaderView
    //     detectedData={detectedData}
    //     toggleFlash={toggleFlash}
    //     mode={mode}
    //     setMode={setMode}
    //   />

    //   <DownloadingProgressView
    //     visible={!modelDownloadingProgress.downloadStatus}
    //     progress={modelDownloadingProgress?.progress}
    //   />
    //   <CameraFooterView
    //     setCaptureMode={setCaptureMode}
    //     captureMode={captureMode}
    //     setIsOnDeviceOCR={setIsOnDeviceOCR}
    //     isOnDeviceOCR={isOnDeviceOCR}
    //     onPressCapture={onPressCapture}
    //     onPressOnDeviceOcr={onPressOnDeviceOcr}
    //     setModelSize={setModelSize}
    //     modelSize={modelSize}
    //     mode={mode}
    //   />
    // </View>

    <View>
      <View style={{backgroundColor: 'red', height: '60%'}}></View>
      <View style={{ height: '20%'}}>
      <VisionSdkView
        refProp={visionSdk}
        isOnDeviceOCR={isOnDeviceOCR}
        // showScanFrame={true}
        // showDocumentBoundaries={true}
        // captureWithScanFrame={true}
        captureMode={captureMode}
        mode={mode}
        environment="sandbox"
        apiKey="key_141b2eda27Z0Cm2y0h0P6waB3Z6pjPgrmGAHNSU62rZelUthBEOOdsVTqZQCRVgPLqI5yMPqpw2ZBy2z"
        flash={flash}
        onDetected={(e: any) => {
          setDetectedData(Platform.OS === 'android' ? e : e.nativeEvent);
        }}
        onBarcodeScan={(e: any) => {
          console.log('BarCodeScanHandler', e);
          setLoading(false);
          visionSdk?.current?.restartScanningHandler();
        }}
        onOCRScan={(e: any) => {
          let scanRes = Platform.OS === 'ios' ? e.nativeEvent.data.data : e;
          if (Platform.OS === 'android') {
            const parsedOuterJson = JSON.parse(scanRes.data);
            scanRes = parsedOuterJson.data;
          }
          setResult(scanRes);
          setLoading(false);
          Vibration.vibrate(100);
          // setTimeout(() => {
          visionSdk?.current?.restartScanningHandler();
          // }, 200);
        }}
        onImageCaptured={(e: any) => {
          console.log('onImageCaptured==------>>', e);
        }}
        onModelDownloadProgress={(e: any) => {
          let response = Platform.OS === 'android' ? e : e.nativeEvent;
          console.log(
            'ModelDownloadProgress==------>>',
            Math.floor(response.progress * 100)
          );
          if (isMultipleOfTen(Math.floor(response.progress * 100))) {
            setModelDownloadingProgress(response);
            if (response.downloadStatus) {
              visionSdk?.current?.startRunningHandler();
            }
          }
          setLoading(false);
        }}
        onError={(e: any) => {
          let error = Platform.OS === 'android' ? e : e.nativeEvent;
          console.log('onError', error);
          Alert.alert('ERROR', error?.message);
          setLoading(false);
        }}
      />
      </View>
      
      <View style={{backgroundColor: 'blue', height: '20%'}}></View>
    </View>
  );
}
const styles = StyleSheet.create({
  mainContainer: {
    flex: 1,
  },
});
