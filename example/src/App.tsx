import React, { useEffect, useState } from 'react';
import { View, StyleSheet, Platform, Alert } from 'react-native';
import VisionSdkView from 'react-native-vision-sdk';
import CameraFooterView from './Components/CameraFooterView';
import DownloadingProgressView from './Components/DownloadingProgressView';
import CameraHeaderView from './Components/CameraHeaderView';

interface downloadingProgress {
  downloadStatus: boolean;
  progress: number;
}
interface detectedDataProps {
  barcode: boolean;
  qrcode: boolean;
  text: boolean;
}
export default function App() {
  const visionSdk = React.useRef<any>(null);
  const [captureMode, setCaptureMode] = useState<string>('manual');
  const [showOcrTypes, setShowOcrTypes] = useState<boolean>(false);
  const [isOnDeviceOCR, setIsOnDeviceOCR] = useState<boolean>(false);
  const [detectedData, setDeectedData] = useState<detectedDataProps>({
    barcode: false,
    qrcode: false,
    text: false,
  });
  const [modelDownloadingProgress, setModelDownloadingProgress] =
    useState<downloadingProgress>({
      downloadStatus: true,
      progress: 0,
    });
  React.useEffect(() => {
    visionSdk?.current?.changeModeHandler(
      captureMode,
      'ocr',
      'eyJhbGciOiJSUzI1NiIsImtpZCI6IjExYzhiMmRmNGM1NTlkMjhjOWRlNWQ0MTAxNDFiMzBkOWUyYmNlM2IiLCJ0eXAiOiJKV1QifQ.eyJvcmciOiJvcmdfd2t0cmc4WFVXVGZpbllybkdORW01SCIsInJvbGUiOiJyb2xlX293bmVyIiwic2NvcGVzIjp7Im9yZ2FuaXphdGlvbnMiOjIsInNoaXBtZW50cyI6MiwibG9jYXRpb25zIjoyLCJ1c2VycyI6MiwicGF5bWVudHMiOjIsInBheW1lbnRfbWV0aG9kcyI6MiwiZGVsaXZlcmllcyI6Miwid2ViaG9va3MiOjIsImFwaV9rZXlzIjoyLCJpdGVtcyI6MiwiYXNzZXRzIjoyLCJmdWxmaWxsbWVudHMiOjIsImNvbnRhY3RzIjoyLCJhZGRyZXNzZXMiOjIsIm1hbmlmZXN0cyI6MiwiaW5mZXJlbmNlcyI6MiwiYXVkaXRzIjoyLCJzY2FucyI6MiwiZXZlbnRzIjoyLCJjb250YWluZXJzIjoyLCJ0aHJlYWRzIjoyLCJhbmFseXRpY3MiOjIsInRyYWNrZXJzIjoyLCJncm91cHMiOjIsImFkbWluX2FpIjowLCJhaSI6MCwic2RrIjowLCJsb3RzIjowfSwibmFtZSI6IkFtZWVyIEhhbXphIiwicHJvZmlsZV9pZCI6InByb2ZfZEFxQmNhanE1TGliWHZKRWhMcERuVSIsImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9weC1wbGF0Zm9ybS1zdGFnaW5nIiwiYXVkIjoicHgtcGxhdGZvcm0tc3RhZ2luZyIsImF1dGhfdGltZSI6MTcyNDQ4MzkwNCwidXNlcl9pZCI6InVzZXJfYmVyVlZveGJBOXRhc0VUaHJKelJadyIsInN1YiI6InVzZXJfYmVyVlZveGJBOXRhc0VUaHJKelJadyIsImlhdCI6MTcyNDQ4MzkxMCwiZXhwIjoxNzI0NDg3NTEwLCJlbWFpbCI6ImFtZWVyLmhhbXphQHBhY2thZ2V4LmlvIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsic2FtbC5zc29fNjJrQzhXOTRITVR1Qkt3aUplc0RhNyI6WyJhbWVlci5oYW16YUBwYWNrYWdleC5pbyJdLCJzYW1sLnNzb19oYVhxbnZ0cW1BeWRld1dRY3J3VTlRIjpbImFtZWVyLmhhbXphQHBhY2thZ2V4LmlvIl0sImVtYWlsIjpbImFtZWVyLmhhbXphQHBhY2thZ2V4LmlvIl19LCJzaWduX2luX3Byb3ZpZGVyIjoic2FtbC5zc29fNjJrQzhXOTRITVR1Qkt3aUplc0RhNyIsInNpZ25faW5fYXR0cmlidXRlcyI6eyJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL2lkZW50aXR5L2NsYWltcy9kaXNwbGF5bmFtZSI6IkFtZWVyIEhhbXphIiwiaHR0cDovL3NjaGVtYXMubWljcm9zb2Z0LmNvbS9pZGVudGl0eS9jbGFpbXMvdGVuYW50aWQiOiJhZTg4YjBjNy1kOGM0LTRiZWUtOWNiZi1lNGNiYTI2NDg0YjQiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL2lkZW50aXR5L2NsYWltcy9pZGVudGl0eXByb3ZpZGVyIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvYWU4OGIwYzctZDhjNC00YmVlLTljYmYtZTRjYmEyNjQ4NGI0LyIsImh0dHA6Ly9zY2hlbWFzLm1pY3Jvc29mdC5jb20vaWRlbnRpdHkvY2xhaW1zL29iamVjdGlkZW50aWZpZXIiOiIzMmM2Y2EwZS04OTJhLTQ2ZDEtOGRkMS01ZDU3NWQyMTJlY2MiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9naXZlbm5hbWUiOiJBbWVlciIsImh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDUvMDUvaWRlbnRpdHkvY2xhaW1zL25hbWUiOiJhbWVlci5oYW16YUBwYWNrYWdleC5pbyIsImh0dHA6Ly9zY2hlbWFzLm1pY3Jvc29mdC5jb20vY2xhaW1zL2F1dGhubWV0aG9kc3JlZmVyZW5jZXMiOlsiaHR0cDovL3NjaGVtYXMubWljcm9zb2Z0LmNvbS93cy8yMDA4LzA2L2lkZW50aXR5L2F1dGhlbnRpY2F0aW9ubWV0aG9kL3Bhc3N3b3JkIiwiaHR0cDovL3NjaGVtYXMubWljcm9zb2Z0LmNvbS9jbGFpbXMvbXVsdGlwbGVhdXRobiJdLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9zdXJuYW1lIjoiSGFtemEiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9lbWFpbGFkZHJlc3MiOiJhbWVlci5oYW16YUBwYWNrYWdleC5pbyJ9fX0.OKM9Uwu1gb6Gr2UunXopBQ77M9msi7UgbDiRDlB9nE-G0jdaFPsCpprdWK8lFc6-77WlVK39QB_hEEQs_F6yQQuxVSczq87CNUw_6_8rStZrM0qQUMPmlgjm5YdEZcosyLQtbqjZ84xKkbfGXU9WQmiSjyxa0CqgtVYp_hEAjAu-CPfdtm7TMuZHQd0f9Wi0lvkDx2rymIpr-wL4SxPu-5vM64nQQaUZj7XW73mVamOxIpvOz1GV6wgrDzVwpaI1W-eF72Yy9ugEgH64LM26AHUyRg4HtOYXFRY2jDx8d87IJ3yM3OcBgiNrYhZMuGFNcdvGD6mc0aTT0hB1J5cEaQ',
      '',
      {
        tracker: {
          type: 'inbound',
          create_automatically: 'false',
          status: 'pickup_available',
        },
        transform: { use_existing_tracking_number: true, tracker: null },
        match: { location: true, use_best_match: true, search: ['recipient'] },
        postprocess: {
          require_unique_hash: true,
          parse_addresses: ['sender', 'recipient'],
        },
      },
      'sandbox'
    );
    visionSdk?.current?.setHeight(1);
    visionSdk?.current?.startRunningHandler();
  }, [captureMode]);
  const onPressCapture = () => {
    visionSdk?.current?.cameraCaptureHandler();
  };
  const toggleTorch = (val: boolean) => {
    visionSdk?.current?.onPressToggleTorchHandler(val);
  };
  function isMultipleOfTen(number: any) {
    return number % 5 === 0;
  }
  useEffect(() => {
    if (isOnDeviceOCR) {
      visionSdk?.current?.configureOnDeviceModel({
        type: 'shipping_label',
        size: 'large',
      });
    }
  }, [isOnDeviceOCR]);
  const onPressOnDeviceOcr = () => {
    visionSdk?.current?.stopRunningHandler();
    setModelDownloadingProgress({
      downloadStatus: false,
      progress: 0,
    });
  };
  return (
    <View style={styles.mainContainer}>
      <VisionSdkView
        refProp={visionSdk}
        isOnDeviceOCR={isOnDeviceOCR}
        showScanFrame={true}
        captureWithScanFrame={true}
        OnDetectedHandler={(e: any) => {
          setDeectedData(Platform.OS === 'android' ? e : e.nativeEvent);
        }}
        apiKey="key_141b2eda27Z0Cm2y0h0P6waB3Z6pjPgrmGAHNSU62rZelUthBEOOdsVTqZQCRVgPLqI5yMPqpw2ZBy2z"
        BarCodeScanHandler={(e: any) => console.log('BarCodeScanHandler', e)}
        OCRScanHandler={(e: any) => {
          console.log('OCRScanHandler', e);
          Alert.alert(
            'Extracted Label Data \n \n Tracking No: ' +
              e.nativeEvent.data.tracking_number +
              '\n Sender: ' +
              e.nativeEvent.data.sender.name +
              '\n Recipient: ' +
              e.nativeEvent.data.recipient.name +
              '\n Provider: ' +
              e.nativeEvent.data.provider_name +
              '\n Type: ' +
              e.nativeEvent.data.type
          );
        }}
        ModelDownloadProgress={(e: any) => {
          let response = Platform.OS === 'android' ? e : e.nativeEvent;
          console.log('ModelDownloadProgress==------>>', response);
          if (isMultipleOfTen(Math.floor(response.progress * 100))) {
            setModelDownloadingProgress(response);
            if (response.downloadStatus) {
              visionSdk?.current?.startRunningHandler();
            }
            // else {
            //   visionSdk?.current?.stopRunningHandler();
            // }
          }
        }}
        onError={(e: any) => {
          console.log('onError', e);
          // Alert.alert(JSON.stringify(e));
        }}
      />
      <CameraHeaderView detectedData={detectedData} toggleTorch={toggleTorch} />
      <DownloadingProgressView
        visible={!modelDownloadingProgress.downloadStatus}
        progress={modelDownloadingProgress?.progress}
      />
      <CameraFooterView
        setCaptureMode={setCaptureMode}
        captureMode={captureMode}
        setShowOcrTypes={setShowOcrTypes}
        showOcrTypes={showOcrTypes}
        setIsOnDeviceOCR={setIsOnDeviceOCR}
        isOnDeviceOCR={isOnDeviceOCR}
        onPressCapture={onPressCapture}
        onPressOnDeviceOcr={onPressOnDeviceOcr}
      />
    </View>
  );
}
const styles = StyleSheet.create({
  mainContainer: {
    flex: 1,
  },
});
