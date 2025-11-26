#ifdef RCT_NEW_ARCH_ENABLED

#import "VisionSdkModuleTurboModule.h"
#import <objc/message.h>
#import <objc/runtime.h>

@implementation VisionSdkModuleTurboModule {
  id _swiftModule;
}

RCT_EXPORT_MODULE(VisionSdkModule)

+ (BOOL)requiresMainQueueSetup
{
  return YES;
}

- (instancetype)init {
  if (self = [super init]) {
    _swiftModule = nil;
  }
  return self;
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[@"onModelDownloadProgress"];
}

// Helper to get Swift module instance (singleton pattern)
- (id)getSwiftModule {
  if (!_swiftModule) {
    Class moduleClass = NSClassFromString(@"VisionSdkModule");
    if (moduleClass) {
      _swiftModule = [[moduleClass alloc] init];
      NSLog(@"[VisionSDK TurboModule] Created Swift module instance");

      // Set event callback to forward events through TurboModule
      __weak VisionSdkModuleTurboModule *weakSelf = self;
      void (^eventCallback)(NSString *, NSDictionary *) = ^(NSString *eventName, NSDictionary *body) {
        VisionSdkModuleTurboModule *strongSelf = weakSelf;
        if (strongSelf) {
          [strongSelf sendEventWithName:eventName body:body];
        }
      };

      SEL setCallbackSelector = NSSelectorFromString(@"setEventCallback:");
      if ([_swiftModule respondsToSelector:setCallbackSelector]) {
        ((void (*)(id, SEL, id))objc_msgSend)(_swiftModule, setCallbackSelector, eventCallback);
        NSLog(@"[VisionSDK TurboModule] Set event callback");
      }
    }
  }
  return _swiftModule;
}

// MARK: - TurboModule Methods

RCT_EXPORT_METHOD(setEnvironment:(NSString *)environment)
{
  NSLog(@"[VisionSDK TurboModule] setEnvironment called with: %@", environment);
  id swiftModule = [self getSwiftModule];
  if (swiftModule) {
    [swiftModule setEnvironment:environment];
  }
}

RCT_EXPORT_METHOD(loadOnDeviceModels:(NSString *)token
                  apiKey:(NSString *)apiKey
                  modelType:(NSString *)modelType
                  modelSize:(NSString *)modelSize
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] loadOnDeviceModels called");
  id swiftModule = [self getSwiftModule];
  if (swiftModule) {
    SEL selector = NSSelectorFromString(@"loadOnDeviceModels:apiKey:modelType:modelSize:resolver:rejecter:");
    if ([swiftModule respondsToSelector:selector]) {
      NSLog(@"[VisionSDK TurboModule] Calling Swift loadOnDeviceModels");
      ((void (*)(id, SEL, NSString *, NSString *, NSString *, NSString *, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
        swiftModule, selector, token, apiKey, modelType, modelSize, resolve, reject
      );
    } else {
      NSLog(@"[VisionSDK TurboModule] loadOnDeviceModels method not found");
      reject(@"METHOD_NOT_FOUND", @"loadOnDeviceModels method not found on VisionSdkModule", nil);
    }
  } else {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule Swift class not found", nil);
  }
}

RCT_EXPORT_METHOD(unLoadOnDeviceModels:(NSString *)modelType
                  shouldDeleteFromDisk:(BOOL)shouldDeleteFromDisk
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSLog(@"[VisionSDK TurboModule] unLoadOnDeviceModels called with modelType: %@, shouldDeleteFromDisk: %d", modelType, shouldDeleteFromDisk);
  id swiftModule = [self getSwiftModule];
  if (swiftModule) {
    SEL selector = NSSelectorFromString(@"unLoadOnDeviceModels:shouldDeleteFromDisk:resolver:rejecter:");
    if ([swiftModule respondsToSelector:selector]) {
      NSLog(@"[VisionSDK TurboModule] Calling Swift unLoadOnDeviceModels");
      ((void (*)(id, SEL, NSString *, BOOL, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
        swiftModule, selector, modelType, shouldDeleteFromDisk, resolve, reject
      );
    } else {
      NSLog(@"[VisionSDK TurboModule] unLoadOnDeviceModels method not found");
      reject(@"METHOD_NOT_FOUND", @"unLoadOnDeviceModels method not found on VisionSdkModule", nil);
    }
  } else {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule Swift class not found", nil);
  }
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
  id swiftModule = [self getSwiftModule];
  if (swiftModule) {
    // Try different possible selectors
    NSArray *possibleSelectors = @[
      @"predict:barcodes:resolver:rejecter:",
      @"predictWithBarcodes:resolver:rejecter:",
      @"predict:withBarcodes:resolver:rejecter:"
    ];

    SEL foundSelector = nil;
    for (NSString *selectorString in possibleSelectors) {
      SEL selector = NSSelectorFromString(selectorString);
      if ([swiftModule respondsToSelector:selector]) {
        NSLog(@"[VisionSDK TurboModule] Found working selector: %@", selectorString);
        foundSelector = selector;
        break;
      }
    }

    if (foundSelector) {
      ((void (*)(id, SEL, NSString *, NSArray *, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
        swiftModule, foundSelector, imagePath, barcodes, resolve, reject
      );
    } else {
      NSLog(@"[VisionSDK TurboModule] No matching selector found. Tried: %@", possibleSelectors);
      reject(@"METHOD_NOT_FOUND", @"predict method not found on VisionSdkModule", nil);
    }
  } else {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule Swift class not found", nil);
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
  id swiftModule = [self getSwiftModule];

  if (!swiftModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule Swift class not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictShippingLabelCloud:barcodes:token:apiKey:locationId:options:metadata:recipient:sender:shouldResizeImage:resolver:rejecter:");

  if ([swiftModule respondsToSelector:selector]) {
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[swiftModule methodSignatureForSelector:selector]];
    [invocation setSelector:selector];
    [invocation setTarget:swiftModule];
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
  id swiftModule = [self getSwiftModule];

  if (!swiftModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule Swift class not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictItemLabelCloud:token:apiKey:shouldResizeImage:resolver:rejecter:");

  if ([swiftModule respondsToSelector:selector]) {
    ((void (*)(id, SEL, NSString *, NSString *, NSString *, NSNumber *, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
      swiftModule, selector, imagePath, token, apiKey, shouldResizeImage, resolve, reject
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
  id swiftModule = [self getSwiftModule];

  if (!swiftModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule Swift class not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictBillOfLadingCloud:barcodes:token:apiKey:locationId:options:shouldResizeImage:resolver:rejecter:");

  if ([swiftModule respondsToSelector:selector]) {
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[swiftModule methodSignatureForSelector:selector]];
    [invocation setSelector:selector];
    [invocation setTarget:swiftModule];
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
  id swiftModule = [self getSwiftModule];

  if (!swiftModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule Swift class not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictDocumentClassificationCloud:token:apiKey:shouldResizeImage:resolver:rejecter:");

  if ([swiftModule respondsToSelector:selector]) {
    ((void (*)(id, SEL, NSString *, NSString *, NSString *, NSNumber *, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
      swiftModule, selector, imagePath, token, apiKey, shouldResizeImage, resolve, reject
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
  id swiftModule = [self getSwiftModule];

  if (!swiftModule) {
    reject(@"MODULE_NOT_FOUND", @"VisionSdkModule Swift class not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"predictWithCloudTransformations:barcodes:token:apiKey:locationId:options:metadata:recipient:sender:shouldResizeImage:resolver:rejecter:");

  if ([swiftModule respondsToSelector:selector]) {
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[swiftModule methodSignatureForSelector:selector]];
    [invocation setSelector:selector];
    [invocation setTarget:swiftModule];
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
  // Required for RCTEventEmitter - called when JS adds a listener
  [super addListener:eventName];
}

RCT_EXPORT_METHOD(removeListeners:(double)count)
{
  // Required for RCTEventEmitter - called when JS removes listeners
  [super removeListeners:count];
}

@end

#endif // RCT_NEW_ARCH_ENABLED
