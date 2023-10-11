import * as React from 'react';
import VisionSdkView from 'react-native-vision-sdk';

export default function App() {
  const visionSdk = React.useRef(null);
  React.useEffect(() => {
    // console.log('ScanMode.BARCODE', ScanMode.BARCODE);
    // console.log('idToken', idToken);
    // console.log('currentLocation.id', location.id);

    visionSdk?.current?.changeModeHandler(
      'auto',
      'barcode',
      'idToken',
      'location.id',
      {
        parse_addresses: 'true',
        match_contacts: 'true',
      },
      'staging'
    );
  }, []);
  return (
    <>
      <VisionSdkView
        refProp={visionSdk}
        BarCodeScanHandler={(e: any) => console.log('BarCodeScanHandler', e)}
        OCRScanHandler={(e: any) => console.log('OCRScanHandler', e)}
        OnDetectedHandler={(e: any) => console.log('OnDetectedHandler', e)}
      />
      {/* <View
        style={{
          zIndex: 1,
          position: 'absolute',
          width: '100%',
          height: 46,
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'blue',
          bottom: 100,
        }}
      >
        <TouchableOpacity
          onPress={() => {
            visionSdk?.current?.onPressToggleTorchHandler();
            // setTimeout(() => {
            //   visionSdk?.current?.startRunningHandler();
            // }, 5000);
          }}
        >
          <Text style={{ color: 'white', fontStyle: 'normal' }}> Button</Text>
        </TouchableOpacity>
      </View> */}
    </>
  );
}
