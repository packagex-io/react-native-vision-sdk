#import "React/RCTViewManager.h"

@interface RCT_EXTERN_MODULE(VisionCameraViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(onCapture, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onRecognitionUpdate, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onSharpnessScoreUpdate, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onBarcodeDetected, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onBoundingBoxesUpdate, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(enableFlash, BOOL)
RCT_EXPORT_VIEW_PROPERTY(zoomLevel, NSNumber)
RCT_EXPORT_VIEW_PROPERTY(scanMode, NSString)
RCT_EXPORT_VIEW_PROPERTY(autoCapture, BOOL)
RCT_EXPORT_VIEW_PROPERTY(scanArea, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(detectionConfig, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(frameSkip, NSNumber)

RCT_EXTERN_METHOD(capture:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(stop:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(start:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(toggleFlash:(nonnull NSNumber *)node enabled:(BOOL)enabled)
RCT_EXTERN_METHOD(setZoom:(nonnull NSNumber *)node level:(nonnull NSNumber *)level)

@end
