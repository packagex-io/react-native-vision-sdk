#import "React/RCTViewManager.h"
@interface RCT_EXTERN_MODULE(VisionSdkViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(onBarcodeScanSuccess, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onOCRDataReceived, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onDetected, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(mode, NSString)
RCT_EXPORT_VIEW_PROPERTY(captureMode, NSString)
RCT_EXPORT_VIEW_PROPERTY(apiKey, NSString)
RCT_EXPORT_VIEW_PROPERTY(token, NSString)
RCT_EXPORT_VIEW_PROPERTY(locationId, NSString)
RCT_EXPORT_VIEW_PROPERTY(environment, NSString)
RCT_EXPORT_VIEW_PROPERTY(options, NSDictionary *) 
RCT_EXTERN_METHOD(captureImage:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(stopRunning:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(startRunning:(nonnull NSNumber *)node)
@end