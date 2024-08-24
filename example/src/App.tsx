import React from 'react';
import { Text, TouchableOpacity, View, StyleSheet } from 'react-native';
import VisionSdkView from 'react-native-vision-sdk';

export default function App() {
  const visionSdk = React.useRef<any>(null);

  React.useEffect(() => {
    visionSdk?.current?.changeModeHandler(
      'manual',
      'ocr',
      'eyJhbGciOiJSUzI1NiIsImtpZCI6IjExYzhiMmRmNGM1NTlkMjhjOWRlNWQ0MTAxNDFiMzBkOWUyYmNlM2IiLCJ0eXAiOiJKV1QifQ.eyJvcmciOiJvcmdfd2t0cmc4WFVXVGZpbllybkdORW01SCIsInJvbGUiOiJyb2xlX293bmVyIiwic2NvcGVzIjp7Im9yZ2FuaXphdGlvbnMiOjIsInNoaXBtZW50cyI6MiwibG9jYXRpb25zIjoyLCJ1c2VycyI6MiwicGF5bWVudHMiOjIsInBheW1lbnRfbWV0aG9kcyI6MiwiZGVsaXZlcmllcyI6Miwid2ViaG9va3MiOjIsImFwaV9rZXlzIjoyLCJpdGVtcyI6MiwiYXNzZXRzIjoyLCJmdWxmaWxsbWVudHMiOjIsImNvbnRhY3RzIjoyLCJhZGRyZXNzZXMiOjIsIm1hbmlmZXN0cyI6MiwiaW5mZXJlbmNlcyI6MiwiYXVkaXRzIjoyLCJzY2FucyI6MiwiZXZlbnRzIjoyLCJjb250YWluZXJzIjoyLCJ0aHJlYWRzIjoyLCJhbmFseXRpY3MiOjIsInRyYWNrZXJzIjoyLCJncm91cHMiOjIsImFkbWluX2FpIjowLCJhaSI6MCwic2RrIjowLCJsb3RzIjowfSwibmFtZSI6IkFtZWVyIEhhbXphIiwicHJvZmlsZV9pZCI6InByb2ZfZEFxQmNhanE1TGliWHZKRWhMcERuVSIsImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9weC1wbGF0Zm9ybS1zdGFnaW5nIiwiYXVkIjoicHgtcGxhdGZvcm0tc3RhZ2luZyIsImF1dGhfdGltZSI6MTcyNDQ4MzkwNCwidXNlcl9pZCI6InVzZXJfYmVyVlZveGJBOXRhc0VUaHJKelJadyIsInN1YiI6InVzZXJfYmVyVlZveGJBOXRhc0VUaHJKelJadyIsImlhdCI6MTcyNDQ4MzkxMCwiZXhwIjoxNzI0NDg3NTEwLCJlbWFpbCI6ImFtZWVyLmhhbXphQHBhY2thZ2V4LmlvIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsic2FtbC5zc29fNjJrQzhXOTRITVR1Qkt3aUplc0RhNyI6WyJhbWVlci5oYW16YUBwYWNrYWdleC5pbyJdLCJzYW1sLnNzb19oYVhxbnZ0cW1BeWRld1dRY3J3VTlRIjpbImFtZWVyLmhhbXphQHBhY2thZ2V4LmlvIl0sImVtYWlsIjpbImFtZWVyLmhhbXphQHBhY2thZ2V4LmlvIl19LCJzaWduX2luX3Byb3ZpZGVyIjoic2FtbC5zc29fNjJrQzhXOTRITVR1Qkt3aUplc0RhNyIsInNpZ25faW5fYXR0cmlidXRlcyI6eyJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL2lkZW50aXR5L2NsYWltcy9kaXNwbGF5bmFtZSI6IkFtZWVyIEhhbXphIiwiaHR0cDovL3NjaGVtYXMubWljcm9zb2Z0LmNvbS9pZGVudGl0eS9jbGFpbXMvdGVuYW50aWQiOiJhZTg4YjBjNy1kOGM0LTRiZWUtOWNiZi1lNGNiYTI2NDg0YjQiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL2lkZW50aXR5L2NsYWltcy9pZGVudGl0eXByb3ZpZGVyIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvYWU4OGIwYzctZDhjNC00YmVlLTljYmYtZTRjYmEyNjQ4NGI0LyIsImh0dHA6Ly9zY2hlbWFzLm1pY3Jvc29mdC5jb20vaWRlbnRpdHkvY2xhaW1zL29iamVjdGlkZW50aWZpZXIiOiIzMmM2Y2EwZS04OTJhLTQ2ZDEtOGRkMS01ZDU3NWQyMTJlY2MiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9naXZlbm5hbWUiOiJBbWVlciIsImh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDUvMDUvaWRlbnRpdHkvY2xhaW1zL25hbWUiOiJhbWVlci5oYW16YUBwYWNrYWdleC5pbyIsImh0dHA6Ly9zY2hlbWFzLm1pY3Jvc29mdC5jb20vY2xhaW1zL2F1dGhubWV0aG9kc3JlZmVyZW5jZXMiOlsiaHR0cDovL3NjaGVtYXMubWljcm9zb2Z0LmNvbS93cy8yMDA4LzA2L2lkZW50aXR5L2F1dGhlbnRpY2F0aW9ubWV0aG9kL3Bhc3N3b3JkIiwiaHR0cDovL3NjaGVtYXMubWljcm9zb2Z0LmNvbS9jbGFpbXMvbXVsdGlwbGVhdXRobiJdLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9zdXJuYW1lIjoiSGFtemEiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9lbWFpbGFkZHJlc3MiOiJhbWVlci5oYW16YUBwYWNrYWdleC5pbyJ9fX0.OKM9Uwu1gb6Gr2UunXopBQ77M9msi7UgbDiRDlB9nE-G0jdaFPsCpprdWK8lFc6-77WlVK39QB_hEEQs_F6yQQuxVSczq87CNUw_6_8rStZrM0qQUMPmlgjm5YdEZcosyLQtbqjZ84xKkbfGXU9WQmiSjyxa0CqgtVYp_hEAjAu-CPfdtm7TMuZHQd0f9Wi0lvkDx2rymIpr-wL4SxPu-5vM64nQQaUZj7XW73mVamOxIpvOz1GV6wgrDzVwpaI1W-eF72Yy9ugEgH64LM26AHUyRg4HtOYXFRY2jDx8d87IJ3yM3OcBgiNrYhZMuGFNcdvGD6mc0aTT0hB1J5cEaQ',
      'loc_3pbd1BthcKBXBWvC1AqNeX',
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
    // visionSdk?.current?.setMetadata({ service: 'inbound' });
    visionSdk?.current?.setHeight(1);
    visionSdk?.current?.startRunningHandler();
    visionSdk?.current?.setModelType('shipping_label');
    visionSdk?.current?.setModelSize('micro');
  }, []);

  return (
    <>
      <VisionSdkView
        refProp={visionSdk}
        isOnDeviceOCR={true}
        showScanFrame={true}
        captureWithScanFrame={true}
        apiKey='key_141b2eda27Z0Cm2y0h0P6waB3Z6pjPgrmGAHNSU62rZelUthBEOOdsVTqZQCRVgPLqI5yMPqpw2ZBy2z'
        // OnDetectedHandler={(e: any) => console.log('OnDetectedHandler', e)}
        BarCodeScanHandler={(e: any) => console.log('BarCodeScanHandler', e.nativeEvent)}
        OCRScanHandler={(e: any) => console.log('OCRScanHandler', e)}
        ModelDownloadProgress={(e: any) =>
          console.log('ModelDownloadProgress', e.nativeEvent)
        }
        onError={(e: any) => console.log('onError', e)}
      />
      <View style={styles.buttonContainer}>
        <TouchableOpacity
          onPress={() => {
            visionSdk?.current?.cameraCaptureHandler();
          }}
        >
          <Text style={styles.buttonTextStyle}> Button</Text>
        </TouchableOpacity>
      </View>
    </>
  );
}
const styles = StyleSheet.create({
  buttonContainer: {
    zIndex: 1,
    position: 'absolute',
    width: '100%',
    height: 46,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'blue',
    bottom: 100,
  },
  buttonTextStyle: {
    color: 'white',
    fontStyle: 'normal',
  },
});
