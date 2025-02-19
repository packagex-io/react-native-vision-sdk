#import "React/RCTViewManager.h"
#import <React/RCTViewManager.h>
@interface RCT_EXTERN_MODULE(VisionSdkViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(onBarcodeScan, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onModelDownloadProgress, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onImageCaptured, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onOCRScan, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onDetected, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onCreateTemplate, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onGetTemplates, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onDeleteTemplateById, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onDeleteTemplates, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(mode, NSString)
RCT_EXPORT_VIEW_PROPERTY(captureMode, NSString)
RCT_EXPORT_VIEW_PROPERTY(apiKey, NSString)
RCT_EXPORT_VIEW_PROPERTY(token, NSString)
RCT_EXPORT_VIEW_PROPERTY(locationId, NSString)
RCT_EXPORT_VIEW_PROPERTY(environment, NSString)
RCT_EXPORT_VIEW_PROPERTY(options, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(ocrMode, NSString)
RCT_EXPORT_VIEW_PROPERTY(ocrType, NSString)
RCT_EXPORT_VIEW_PROPERTY(shouldResizeImage, BOOL)
RCT_EXPORT_VIEW_PROPERTY(flash, BOOL)
RCT_EXPORT_VIEW_PROPERTY(isEnableAutoOcrResponseWithImage, BOOL)
RCT_EXPORT_VIEW_PROPERTY(isMultipleScanEnabled, BOOL)
RCT_EXPORT_VIEW_PROPERTY(zoomLevel, NSNumber)
RCT_EXTERN_METHOD(captureImage:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(stopRunning:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(startRunning:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(setMetaData:(nonnull NSNumber *)node  metaData:(nonnull NSDictionary *)metaData)
RCT_EXTERN_METHOD(setRecipient:(nonnull NSNumber *)node  recipient:(nonnull NSDictionary *)recipient)
RCT_EXTERN_METHOD(setSender:(nonnull NSNumber *)node  sender:(nonnull NSDictionary *)sender)
RCT_EXTERN_METHOD(configureOnDeviceModel:(nonnull NSNumber *)node onDeviceConfigs:(nonnull NSDictionary *)type)
RCT_EXTERN_METHOD(restartScanning:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(setFocusSettings:(nonnull NSNumber *)node focusSettings:(nonnull NSDictionary *)type)
RCT_EXTERN_METHOD(setObjectDetectionSettings:(nonnull NSNumber *)node objectDetectionSettings:(nonnull NSDictionary *)type)
RCT_EXTERN_METHOD(setCameraSettings:(nonnull NSNumber *)node cameraSettings:(nonnull NSDictionary *)type)
RCT_EXTERN_METHOD(getPrediction:(nonnull NSNumber *)node
                  image:(nonnull NSString *)imagePath
                  barcode:(nonnull NSArray<NSString *> *)barcodeArray)
RCT_EXTERN_METHOD(getPredictionWithCloudTransformations:(nonnull NSNumber *)node
                  image:(nonnull NSString *)image
                  barcode:(nonnull NSArray<NSString *> *)barcode)
RCT_EXTERN_METHOD(getPredictionShippingLabelCloud:(nonnull NSNumber *)node
                  image:(nonnull NSString *)image
                  barcode:(nonnull NSArray<NSString *> *)barcode
                  token:(nullable NSString *)token
                  apiKey:(nullable NSString *)apiKey
                  locationId:(nullable NSString *)locationId
                  options:(nullable NSDictionary *)options
                  metadata:(nullable NSDictionary *)metadata
                  recipient:(nullable NSDictionary *)recipient
                  sender:(nullable NSDictionary *)sender
                  shouldResizeImage:(nonnull NSNumber *)shouldResizeImage)
RCT_EXTERN_METHOD(getPredictionBillOfLadingCloud:(nonnull NSNumber *)node
                  image:(nonnull NSString *)image
                  barcode:(nonnull NSArray<NSString *> *)barcode
                  token:(nullable NSString *)token
                  apiKey:(nullable NSString *)apiKey
                  locationId:(nullable NSString *)locationId
                  options:(nullable NSDictionary *)options
                  shouldResizeImage:(nonnull NSNumber *)shouldResizeImage)
RCT_EXTERN_METHOD(getPredictionItemLabelCloud:(nonnull NSNumber *)node
                  image:(nonnull NSString *)image
                  withImageResizing:(nonnull BOOL *)withImageResizing)
RCT_EXTERN_METHOD(getPredictionDocumentClassificationCloud:(nonnull NSNumber *)node
                  image:(nonnull NSString *)image)
RCT_EXTERN_METHOD(reportError:(nonnull NSNumber *)node data:(nonnull NSDictionary *)data)
RCT_EXTERN_METHOD(createTemplate:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(getAllTemplates:(nonnull NSNumber *)node)
RCT_EXTERN_METHOD(deleteTemplateWithId:(nonnull NSNumber *)node id:(NSString *)id)
RCT_EXTERN_METHOD(deleteAllTemplates:(nonnull NSNumber *)node)
@end

