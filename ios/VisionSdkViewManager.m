#import "React/RCTViewManager.h"
@interface RCT_EXTERN_MODULE(VisionSdkViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(onBarcodeScan, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onModelDownloadProgress, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onImageCaptured, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onOCRScan, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onDetected, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(mode, NSString)
RCT_EXPORT_VIEW_PROPERTY(captureMode, NSString)
RCT_EXPORT_VIEW_PROPERTY(apiKey, NSString)
RCT_EXPORT_VIEW_PROPERTY(token, NSString)
RCT_EXPORT_VIEW_PROPERTY(locationId, NSString)
RCT_EXPORT_VIEW_PROPERTY(environment, NSString)
RCT_EXPORT_VIEW_PROPERTY(options, NSString) 
RCT_EXPORT_VIEW_PROPERTY(delayTime, NSNumber)
RCT_EXPORT_VIEW_PROPERTY(isOnDeviceOCR, BOOL)
RCT_EXPORT_VIEW_PROPERTY(showScanFrame, BOOL)
RCT_EXPORT_VIEW_PROPERTY(captureWithScanFrame, BOOL)
RCT_EXPORT_VIEW_PROPERTY(flash, BOOL)
RCT_EXTERN_METHOD(captureImage:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(stopRunning:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(startRunning:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(setZoomTo:(nonnull NSNumber *)node  zoomValue:(nonnull NSNumber *)zoomValue)
RCT_EXTERN_METHOD(setHeight:(nonnull NSNumber *)node  height:(nonnull NSNumber *)height)
RCT_EXTERN_METHOD(setMetaData:(nonnull NSNumber *)node  metaData:(nonnull NSString *)metaData)
RCT_EXTERN_METHOD(setRecipient:(nonnull NSNumber *)node  recipient:(nonnull NSString *)recipient)
RCT_EXTERN_METHOD(setSender:(nonnull NSNumber *)node  sender:(nonnull NSString *)sender)
RCT_EXTERN_METHOD(configureOnDeviceModel:(nonnull NSNumber *)node onDeviceConfigs:(nonnull NSDictionary *)type)
RCT_EXTERN_METHOD(restartScanning:(nonnull NSNumber *)node)
//RCT_EXTERN_METHOD(setModelType:(nonnull NSNumber *)node  modelType:(nonnull NSString *)modelType)
//RCT_EXTERN_METHOD(setModelSize:(nonnull NSNumber *)node  modelSize:(nonnull NSString *)modelSize)
@end
