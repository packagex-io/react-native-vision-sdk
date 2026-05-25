#ifdef RCT_NEW_ARCH_ENABLED

#import "DimensioningModuleTurboModule.h"
#import <objc/message.h>

@implementation DimensioningModuleTurboModule {
  id _swiftModule;
}

RCT_EXPORT_MODULE(DimensioningModule)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

- (instancetype)init {
  if (self = [super init]) {
    _swiftModule = nil;
  }
  return self;
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[];
}

- (void)startObserving {}
- (void)stopObserving {}

// Helper: lazy-init Swift DimensioningModule singleton
- (id)getSwiftModule {
  if (!_swiftModule) {
    Class cls = NSClassFromString(@"DimensioningModule");
    if (cls) {
      _swiftModule = [[cls alloc] init];
    }
  }
  return _swiftModule;
}

// MARK: - deviceCapabilities

RCT_EXPORT_METHOD(deviceCapabilities:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  id swiftModule = [self getSwiftModule];
  if (!swiftModule) {
    reject(@"MODULE_NOT_FOUND", @"DimensioningModule Swift class not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"deviceCapabilitiesWithResolver:rejecter:");
  if ([swiftModule respondsToSelector:selector]) {
    ((void (*)(id, SEL, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
      swiftModule, selector, resolve, reject
    );
  } else {
    reject(@"NOT_IMPLEMENTED", @"deviceCapabilities not available", nil);
  }
}

// MARK: - prefetchModels

RCT_EXPORT_METHOD(prefetchModels:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  id swiftModule = [self getSwiftModule];
  if (!swiftModule) {
    reject(@"MODULE_NOT_FOUND", @"DimensioningModule Swift class not found", nil);
    return;
  }

  SEL selector = NSSelectorFromString(@"prefetchModelsWithResolver:rejecter:");
  if ([swiftModule respondsToSelector:selector]) {
    ((void (*)(id, SEL, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(
      swiftModule, selector, resolve, reject
    );
  } else {
    reject(@"NOT_IMPLEMENTED", @"prefetchModels not available", nil);
  }
}

@end

#endif // RCT_NEW_ARCH_ENABLED
