#import "React/RCTViewManager.h"
@interface RCT_EXTERN_MODULE(VisionSdkViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(onBarcodeScanSuccess, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onOCRDataReceived, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onDetected, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(mode, NSString)
RCT_EXPORT_VIEW_PROPERTY(apiKey, NSString)
RCT_EXTERN_METHOD(captureImage:(nonnull NSNumber *)node)

@end
