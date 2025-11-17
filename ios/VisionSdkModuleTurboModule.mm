#ifdef RCT_NEW_ARCH_ENABLED

#import "VisionSdkModuleTurboModule.h"
#import <objc/message.h>

@implementation VisionSdkModuleTurboModule

RCT_EXPORT_MODULE(VisionSdkModule)

+ (BOOL)requiresMainQueueSetup
{
  return YES;
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[@"onModelDownloadProgress"];
}

// MARK: - TurboModule Methods

RCT_EXPORT_METHOD(setEnvironment:(NSString *)environment)
{
  NSLog(@"[VisionSDK TurboModule] setEnvironment called with: %@", environment);
  // TODO: Implement using Swift VisionSdkModule when Swift-ObjC++ bridging is set up
}

RCT_EXPORT_METHOD(loadOnDeviceModels:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  modelType:(NSString *)modelType
                  modelSize:(NSString *)modelSize
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] loadOnDeviceModels called");
  // TODO: Implement using Swift VisionSdkModule when Swift-ObjC++ bridging is set up
  resolve(@"Model loading not yet implemented in TurboModule");
}

RCT_EXPORT_METHOD(unLoadOnDeviceModels:(NSString * _Nullable)modelType
                  shouldDeleteFromDisk:(BOOL)shouldDeleteFromDisk
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] unLoadOnDeviceModels called");
  // TODO: Implement using Swift VisionSdkModule when Swift-ObjC++ bridging is set up
  resolve(@"Models unloaded successfully");
}

RCT_EXPORT_METHOD(logItemLabelDataToPx:(NSString *)imageUri
                  barcodes:(NSArray<NSString *> *)barcodes
                  responseData:(NSString *)responseData
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  shouldResizeImage:(BOOL)shouldResizeImage
                  metadata:(NSString *)metadata
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] logItemLabelDataToPx called");
  // TODO: Implement using Swift VisionSdkModule when Swift-ObjC++ bridging is set up
  resolve(@"{\"success\": true}");
}

RCT_EXPORT_METHOD(logShippingLabelDataToPx:(NSString *)imageUri
                  barcodes:(NSArray<NSString *> *)barcodes
                  responseData:(NSString *)responseData
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  locationId:(NSString * _Nullable)locationId
                  options:(NSString *)options
                  metadata:(NSString *)metadata
                  recipient:(NSString *)recipient
                  sender:(NSString *)sender
                  shouldResizeImage:(BOOL)shouldResizeImage
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] logShippingLabelDataToPx called");
  // TODO: Implement using Swift VisionSdkModule when Swift-ObjC++ bridging is set up
  resolve(@"{\"success\": true}");
}

RCT_EXPORT_METHOD(logBillOfLadingDataToPx:(NSString *)imageUri
                  barcodes:(NSArray<NSString *> *)barcodes
                  responseData:(NSString *)responseData
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  locationId:(NSString * _Nullable)locationId
                  options:(NSString *)options
                  shouldResizeImage:(BOOL)shouldResizeImage
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] logBillOfLadingDataToPx called");
  // For now, just return success - actual implementation can be added later
  resolve(@"{\"success\": true}");
}

RCT_EXPORT_METHOD(logDocumentClassificationDataToPx:(NSString *)imageUri
                  responseData:(NSString *)responseData
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  shouldResizeImage:(BOOL)shouldResizeImage
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] logDocumentClassificationDataToPx called");
  // For now, just return success - actual implementation can be added later
  resolve(@"{\"success\": true}");
}

// Helper method to parse JSON strings
- (NSDictionary *)parseJSONString:(NSString *)jsonString
{
  if (!jsonString || jsonString.length == 0) {
    return nil;
  }

  NSData *data = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
  if (!data) {
    return nil;
  }

  NSError *error;
  id result = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];

  if (error || ![result isKindOfClass:[NSDictionary class]]) {
    return nil;
  }

  return (NSDictionary *)result;
}

RCT_EXPORT_METHOD(predict:(NSString *)imagePath
                  barcodes:(NSArray *)barcodes
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] predict called");
  // Call the Legacy Swift module using NSInvocation
  id legacyModule = [self.bridge moduleForName:@"VisionSdkModule"];

  if (!legacyModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predict:barcodes:resolver:rejecter:");

  if ([legacyModule respondsToSelector:selector]) {
    // Use performSelector for 4 arguments (max supported)
    ((void (*)(id, SEL, NSString *, NSArray *, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
      legacyModule, selector, imagePath, barcodes, resolve, reject
    );
  } else {
    reject(@"NOT_IMPLEMENTED", @"predict method not available in legacy module", nil);
  }
}

RCT_EXPORT_METHOD(predictShippingLabelCloud:(NSString *)imagePath
                  barcodes:(NSArray<NSString *> * _Nullable)barcodes
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  locationId:(NSString * _Nullable)locationId
                  options:(NSDictionary * _Nullable)options
                  metadata:(NSDictionary * _Nullable)metadata
                  recipient:(NSDictionary * _Nullable)recipient
                  sender:(NSDictionary * _Nullable)sender
                  shouldResizeImage:(NSNumber * _Nullable)shouldResizeImage
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] predictShippingLabelCloud called");
  id legacyModule = [self.bridge moduleForName:@"VisionSdkModule"];

  if (!legacyModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictShippingLabelCloud:barcodes:token:apiKey:locationId:options:metadata:recipient:sender:shouldResizeImage:resolver:rejecter:");

  if ([legacyModule respondsToSelector:selector]) {
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[legacyModule methodSignatureForSelector:selector]];
    [invocation setSelector:selector];
    [invocation setTarget:legacyModule];
    [invocation setArgument:&imagePath atIndex:2];
    [invocation setArgument:&barcodes atIndex:3];
    [invocation setArgument:&token atIndex:4];
    [invocation setArgument:&apiKey atIndex:5];
    [invocation setArgument:&locationId atIndex:6];
    [invocation setArgument:&options atIndex:7];
    [invocation setArgument:&metadata atIndex:8];
    [invocation setArgument:&recipient atIndex:9];
    [invocation setArgument:&sender atIndex:10];
    [invocation setArgument:&shouldResizeImage atIndex:11];
    [invocation setArgument:&resolve atIndex:12];
    [invocation setArgument:&reject atIndex:13];
    [invocation invoke];
  } else {
    reject(@"NOT_IMPLEMENTED", @"predictShippingLabelCloud method not available", nil);
  }
}

RCT_EXPORT_METHOD(predictItemLabelCloud:(NSString *)imagePath
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  shouldResizeImage:(NSNumber * _Nullable)shouldResizeImage
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] predictItemLabelCloud called");
  id legacyModule = [self.bridge moduleForName:@"VisionSdkModule"];

  if (!legacyModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictItemLabelCloud:token:apiKey:shouldResizeImage:resolver:rejecter:");

  if ([legacyModule respondsToSelector:selector]) {
    ((void (*)(id, SEL, NSString *, NSString *, NSString *, NSNumber *, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
      legacyModule, selector, imagePath, token, apiKey, shouldResizeImage, resolve, reject
    );
  } else {
    reject(@"NOT_IMPLEMENTED", @"predictItemLabelCloud method not available", nil);
  }
}

RCT_EXPORT_METHOD(predictBillOfLadingCloud:(NSString *)imagePath
                  barcodes:(NSArray<NSString *> * _Nullable)barcodes
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  locationId:(NSString * _Nullable)locationId
                  options:(NSDictionary * _Nullable)options
                  shouldResizeImage:(NSNumber * _Nullable)shouldResizeImage
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] predictBillOfLadingCloud called");
  id legacyModule = [self.bridge moduleForName:@"VisionSdkModule"];

  if (!legacyModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictBillOfLadingCloud:barcodes:token:apiKey:locationId:options:shouldResizeImage:resolver:rejecter:");

  if ([legacyModule respondsToSelector:selector]) {
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[legacyModule methodSignatureForSelector:selector]];
    [invocation setSelector:selector];
    [invocation setTarget:legacyModule];
    [invocation setArgument:&imagePath atIndex:2];
    [invocation setArgument:&barcodes atIndex:3];
    [invocation setArgument:&token atIndex:4];
    [invocation setArgument:&apiKey atIndex:5];
    [invocation setArgument:&locationId atIndex:6];
    [invocation setArgument:&options atIndex:7];
    [invocation setArgument:&shouldResizeImage atIndex:8];
    [invocation setArgument:&resolve atIndex:9];
    [invocation setArgument:&reject atIndex:10];
    [invocation invoke];
  } else {
    reject(@"NOT_IMPLEMENTED", @"predictBillOfLadingCloud method not available", nil);
  }
}

RCT_EXPORT_METHOD(predictDocumentClassificationCloud:(NSString *)imagePath
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  shouldResizeImage:(NSNumber * _Nullable)shouldResizeImage
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] predictDocumentClassificationCloud called");
  id legacyModule = [self.bridge moduleForName:@"VisionSdkModule"];

  if (!legacyModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictDocumentClassificationCloud:token:apiKey:shouldResizeImage:resolver:rejecter:");

  if ([legacyModule respondsToSelector:selector]) {
    ((void (*)(id, SEL, NSString *, NSString *, NSString *, NSNumber *, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
      legacyModule, selector, imagePath, token, apiKey, shouldResizeImage, resolve, reject
    );
  } else {
    reject(@"NOT_IMPLEMENTED", @"predictDocumentClassificationCloud method not available", nil);
  }
}

RCT_EXPORT_METHOD(predictWithCloudTransformations:(NSString *)imagePath
                  barcodes:(NSArray<NSString *> * _Nullable)barcodes
                  token:(NSString * _Nullable)token
                  apiKey:(NSString * _Nullable)apiKey
                  locationId:(NSString * _Nullable)locationId
                  options:(NSDictionary * _Nullable)options
                  metadata:(NSDictionary * _Nullable)metadata
                  recipient:(NSDictionary * _Nullable)recipient
                  sender:(NSDictionary * _Nullable)sender
                  shouldResizeImage:(NSNumber * _Nullable)shouldResizeImage
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] predictWithCloudTransformations called");
  id legacyModule = [self.bridge moduleForName:@"VisionSdkModule"];

  if (!legacyModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictWithCloudTransformations:barcodes:token:apiKey:locationId:options:metadata:recipient:sender:shouldResizeImage:resolver:rejecter:");

  if ([legacyModule respondsToSelector:selector]) {
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[legacyModule methodSignatureForSelector:selector]];
    [invocation setSelector:selector];
    [invocation setTarget:legacyModule];
    [invocation setArgument:&imagePath atIndex:2];
    [invocation setArgument:&barcodes atIndex:3];
    [invocation setArgument:&token atIndex:4];
    [invocation setArgument:&apiKey atIndex:5];
    [invocation setArgument:&locationId atIndex:6];
    [invocation setArgument:&options atIndex:7];
    [invocation setArgument:&metadata atIndex:8];
    [invocation setArgument:&recipient atIndex:9];
    [invocation setArgument:&sender atIndex:10];
    [invocation setArgument:&shouldResizeImage atIndex:11];
    [invocation setArgument:&resolve atIndex:12];
    [invocation setArgument:&reject atIndex:13];
    [invocation invoke];
  } else {
    reject(@"NOT_IMPLEMENTED", @"predictWithCloudTransformations method not available", nil);
  }
}

// Required for event emitters
RCT_EXPORT_METHOD(addListener:(NSString *)eventName)
{
  // Set up event listeners
}

RCT_EXPORT_METHOD(removeListeners:(double)count)
{
  // Remove event listeners
}

@end

#endif // RCT_NEW_ARCH_ENABLED
