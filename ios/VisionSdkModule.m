#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(VisionSdkModule, RCTEventEmitter)
RCT_EXTERN_METHOD(setEnvironment:(NSString *)environment)
RCT_EXTERN_METHOD(loadOnDeviceModels:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  modelType:(NSString *)modelType
                  modelSize:(NSString * _Nullable)modelSize
                  resolver:(RCTPromiseResolveBlock)resolver
                  rejecter:(RCTPromiseRejectBlock)rejecter)
RCT_EXTERN_METHOD(logItemLabelDataToPx:(NSString *)imageUri
                  barcodes:(nullable NSArray<NSString *> *)barcode
                  responseData:(nonnull NSDictionary *)responseData
                  token:(nullable NSString *)token
                  apiKey:(nullable NSString *)apiKey
                  shouldResizeImage:(nonnull NSNumber *)shouldResizeImage
                  resolver:(RCTPromiseResolveBlock)resolver
                  rejecter:(RCTPromiseRejectBlock)rejecter)
RCT_EXTERN_METHOD(logShippingLabelDataToPx:(NSString *)imageUri
                  barcodes:(nullable NSArray<NSString *> *)barcodes
                  responseData:(NSDictionary *)responseData
                  token:(nullable NSString *)token
                  apiKey:(nullable NSString *)apiKey
                  locationId:(nullable NSString *)locationId
                  options:(nullable NSDictionary *)options
                  metadata:(nullable NSDictionary *)metaData
                  recipient:(nullable NSDictionary *)recipient
                  sender:(nullable NSDictionary *)sender
                  shouldResizeImage:(nonnull NSNumber *)shouldResizeImage
                  resolver:(RCTPromiseResolveBlock)resolver
                  rejecter:(RCTPromiseRejectBlock)rejecter)
@end
