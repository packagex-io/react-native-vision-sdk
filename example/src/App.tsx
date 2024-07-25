import React from 'react';
import VisionSdkView from 'react-native-vision-sdk';

export default function App() {
  const visionSdk = React.useRef<any>(null);

  React.useEffect(() => {
    visionSdk?.current?.changeModeHandler(
      'auto',
      'barcode',
      'eyJhbGciOiJSUzI1NiIsImtpZCI6IjVkNjE3N2E5Mjg2ZDI1Njg0NTI2OWEzMTM2ZDNmNjY0MjZhNGQ2NDIiLCJ0eXAiOiJKV1QifQ.eyJvcmciOiJvcmdfMzExS0V6Z3d1WkE1aDNINmQyMmZkVyIsInJvbGUiOiJyb2xlX293bmVyIiwic2NvcGVzIjp7Im9yZ2FuaXphdGlvbnMiOjIsInNoaXBtZW50cyI6MiwibG9jYXRpb25zIjoyLCJ1c2VycyI6MiwicGF5bWVudHMiOjIsInBheW1lbnRfbWV0aG9kcyI6MiwiZGVsaXZlcmllcyI6Miwid2ViaG9va3MiOjIsImFwaV9rZXlzIjoyLCJpdGVtcyI6MiwiYXNzZXRzIjoyLCJmdWxmaWxsbWVudHMiOjIsImNvbnRhY3RzIjoyLCJhZGRyZXNzZXMiOjIsIm1hbmlmZXN0cyI6MiwiaW5mZXJlbmNlcyI6MiwiYXVkaXRzIjoyLCJzY2FucyI6MiwiZXZlbnRzIjoyLCJjb250YWluZXJzIjoyLCJ0aHJlYWRzIjoyLCJhbmFseXRpY3MiOjIsInRyYWNrZXJzIjoyLCJncm91cHMiOjJ9LCJuYW1lIjoiWmFoZWVyIEFobWVkIiwicHJvZmlsZV9pZCI6InByb2ZfYmRCcmpkUHhIQlNCczM3ZVM0dTVOYSIsImxvZ2luX2NvZGUiOnRydWUsImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9weC1wbGF0Zm9ybS1wcm9kLTRmYWMyIiwiYXVkIjoicHgtcGxhdGZvcm0tcHJvZC00ZmFjMiIsImF1dGhfdGltZSI6MTcxNzA3MTEzOCwidXNlcl9pZCI6InVzZXJfOXc4Rll0czlvTkFTVWVIQzJpTWVwbyIsInN1YiI6InVzZXJfOXc4Rll0czlvTkFTVWVIQzJpTWVwbyIsImlhdCI6MTcxNzEzMzQ0MCwiZXhwIjoxNzE3MTM3MDQwLCJlbWFpbCI6InphaGVlci5yYXNoZWVkQHBhY2thZ2V4LmlvIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZW1haWwiOlsiemFoZWVyLnJhc2hlZWRAcGFja2FnZXguaW8iXX0sInNpZ25faW5fcHJvdmlkZXIiOiJjdXN0b20ifX0.Q7p6ZIdwPv5PK5NC7T3nlD8vqBo3zl9I4hUqieDvQCOBo9tJYD4y2xQ6yXAVa80GzwXS5RZRfADajYJJLQ-WjLb5kiW3kRromzKax180GEmVLfrK0s0RBoZmBP3Pm0eU-TTYKqQAzd8dXugGVvuNOnvu2Cu_CNo_gUrTHkBujXZFdRnDakQoN9zGL6QmUfKvUV5A5ecnQqsM3iIP8Ejyt3wMmw1LujfOpLFZlh3p39lMcNRIPpfVJG4_tfwVhyDFh6j0MBS1g65f8dw_xV6MdSfFwqATtnL0JNV4umKlX5cu2ADB_LXyTCzBjOCFn4eoaFj1DUOTXWTm4G6MKKs2kw',
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
      'staging'
    );
    visionSdk?.current?.setMetadata({ service: 'inbound' });
    visionSdk?.current?.startRunningHandler();

    // setInterval(() => {
    //   setMode(mode === 'manual' ? 'auto' : 'manual');
    // }, 5000);
  }, []);

  return (
    <>
      <VisionSdkView
        refProp={visionSdk}
        BarCodeScanHandler={(e: any) => console.log('BarCodeScanHandler', e)}
        OCRScanHandler={(e: any) => console.log('OCRScanHandler', e)}
        OnDetectedHandler={(e: any) => console.log('OnDetectedHandler', e)}
        showScanFrame={true}
        captureWithScanFrame={true}
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
            visionSdk?.current?.cameraCaptureHandler();
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
