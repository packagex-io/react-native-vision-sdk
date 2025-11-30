#ifdef RCT_NEW_ARCH_ENABLED

#import "VisionSdkViewComponentView.h"

#import <react/renderer/components/VisionSdkView/ComponentDescriptors.h>
#import <react/renderer/components/VisionSdkView/EventEmitters.h>
#import <react/renderer/components/VisionSdkView/Props.h>
#import <react/renderer/components/VisionSdkView/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"
#import <objc/message.h>

using namespace facebook::react;

@interface VisionSdkViewComponentView () <RCTVisionSdkViewViewProtocol>
@end

@implementation VisionSdkViewComponentView {
  UIView *_codeScannerView;
}

// MARK: - Initialization

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const VisionSdkViewProps>();
    _props = defaultProps;

    // Create the actual VisionSDK code scanner view using dynamic class loading
    // Try multiple possible class names for Swift-ObjC interop
    Class RNCodeScannerViewClass = NSClassFromString(@"RNCodeScannerView");
    if (!RNCodeScannerViewClass) {
      RNCodeScannerViewClass = NSClassFromString(@"react_native_vision_sdk.RNCodeScannerView");
    }
    if (!RNCodeScannerViewClass) {
      RNCodeScannerViewClass = NSClassFromString(@"VisionSdkExample.RNCodeScannerView");
    }
    NSLog(@"[VisionSdkViewComponentView] RNCodeScannerView class: %@", RNCodeScannerViewClass);
    if (RNCodeScannerViewClass) {
      NSLog(@"[VisionSdkViewComponentView] Creating RNCodeScannerView");
      _codeScannerView = [[RNCodeScannerViewClass alloc] init];
      NSLog(@"[VisionSdkViewComponentView] Created view: %@", _codeScannerView);

      // Set up event blocks to bridge Swift events to Fabric event emitters
      __weak VisionSdkViewComponentView *weakSelf = self;

      // Use direct property assignment with objc_msgSend instead of KVC
      SEL onBarcodeScanSetter = NSSelectorFromString(@"setOnBarcodeScan:");
      if ([_codeScannerView respondsToSelector:onBarcodeScanSetter]) {
        id barcodeScanBlock = ^(NSDictionary *event) {
          [weakSelf emitBarcodeScanEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onBarcodeScanSetter, barcodeScanBlock);
      }

      SEL onModelDownloadProgressSetter = NSSelectorFromString(@"setOnModelDownloadProgress:");
      if ([_codeScannerView respondsToSelector:onModelDownloadProgressSetter]) {
        id progressBlock = ^(NSDictionary *event) {
          [weakSelf emitModelDownloadProgressEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onModelDownloadProgressSetter, progressBlock);
      }

      SEL onImageCapturedSetter = NSSelectorFromString(@"setOnImageCaptured:");
      if ([_codeScannerView respondsToSelector:onImageCapturedSetter]) {
        id imageCapturedBlock = ^(NSDictionary *event) {
          [weakSelf emitImageCapturedEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onImageCapturedSetter, imageCapturedBlock);
      }

      SEL onSharpnessScoreSetter = NSSelectorFromString(@"setOnSharpnessScore:");
      if ([_codeScannerView respondsToSelector:onSharpnessScoreSetter]) {
        id sharpnessBlock = ^(NSDictionary *event) {
          [weakSelf emitSharpnessScoreEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onSharpnessScoreSetter, sharpnessBlock);
      }

      SEL onOCRScanSetter = NSSelectorFromString(@"setOnOCRScan:");
      if ([_codeScannerView respondsToSelector:onOCRScanSetter]) {
        id ocrScanBlock = ^(NSDictionary *event) {
          [weakSelf emitOCRScanEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onOCRScanSetter, ocrScanBlock);
      }

      SEL onDetectedSetter = NSSelectorFromString(@"setOnDetected:");
      if ([_codeScannerView respondsToSelector:onDetectedSetter]) {
        id detectedBlock = ^(NSDictionary *event) {
          [weakSelf emitDetectedEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onDetectedSetter, detectedBlock);
      }

      SEL onErrorSetter = NSSelectorFromString(@"setOnError:");
      if ([_codeScannerView respondsToSelector:onErrorSetter]) {
        id errorBlock = ^(NSDictionary *event) {
          [weakSelf emitErrorEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onErrorSetter, errorBlock);
      }

      SEL onBoundingBoxesDetectedSetter = NSSelectorFromString(@"setOnBoundingBoxesDetected:");
      if ([_codeScannerView respondsToSelector:onBoundingBoxesDetectedSetter]) {
        id boundingBoxesBlock = ^(NSDictionary *event) {
          [weakSelf emitBoundingBoxesDetectedEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onBoundingBoxesDetectedSetter, boundingBoxesBlock);
      }

      SEL onPriceTagDetectedSetter = NSSelectorFromString(@"setOnPriceTagDetected:");
      if ([_codeScannerView respondsToSelector:onPriceTagDetectedSetter]) {
        id priceTagBlock = ^(NSDictionary *event) {
          [weakSelf emitPriceTagDetectedEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onPriceTagDetectedSetter, priceTagBlock);
      }

      SEL onCreateTemplateSetter = NSSelectorFromString(@"setOnCreateTemplate:");
      if ([_codeScannerView respondsToSelector:onCreateTemplateSetter]) {
        id createTemplateBlock = ^(NSDictionary *event) {
          [weakSelf emitCreateTemplateEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onCreateTemplateSetter, createTemplateBlock);
      }

      SEL onGetTemplatesSetter = NSSelectorFromString(@"setOnGetTemplates:");
      if ([_codeScannerView respondsToSelector:onGetTemplatesSetter]) {
        id getTemplatesBlock = ^(NSDictionary *event) {
          [weakSelf emitGetTemplatesEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onGetTemplatesSetter, getTemplatesBlock);
      }

      SEL onDeleteTemplateByIdSetter = NSSelectorFromString(@"setOnDeleteTemplateById:");
      if ([_codeScannerView respondsToSelector:onDeleteTemplateByIdSetter]) {
        id deleteTemplateByIdBlock = ^(NSDictionary *event) {
          [weakSelf emitDeleteTemplateByIdEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onDeleteTemplateByIdSetter, deleteTemplateByIdBlock);
      }

      SEL onDeleteTemplatesSetter = NSSelectorFromString(@"setOnDeleteTemplates:");
      if ([_codeScannerView respondsToSelector:onDeleteTemplatesSetter]) {
        id deleteTemplatesBlock = ^(NSDictionary *event) {
          [weakSelf emitDeleteTemplatesEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, onDeleteTemplatesSetter, deleteTemplatesBlock);
      }
    } else {
      // Fallback to placeholder if Swift class not available
      _codeScannerView = [[UIView alloc] initWithFrame:self.bounds];
      _codeScannerView.backgroundColor = [UIColor blackColor];
    }

    self.contentView = _codeScannerView;

    // Ensure user interaction is enabled
    self.userInteractionEnabled = YES;
    _codeScannerView.userInteractionEnabled = YES;
  }

  return self;
}

// MARK: - Touch Handling

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event
{
  // Let touches pass through to buttons/controls on top of camera
  UIView *hitView = [super hitTest:point withEvent:event];

  // If hit view is this container, return nil to pass touch through
  if (hitView == self) {
    return nil;
  }

  return hitView;
}

// MARK: - Lifecycle methods

- (void)didMoveToWindow
{
  [super didMoveToWindow];
  NSLog(@"[VisionSdkViewComponentView] didMoveToWindow: %@", self.window);
}

- (void)prepareForRecycle
{
  [super prepareForRecycle];

  NSLog(@"[VisionSdkViewComponentView] prepareForRecycle - stopping camera and clearing touch state");

  // Force resign first responder to clear responder chain
  if ([_codeScannerView isFirstResponder]) {
    [_codeScannerView resignFirstResponder];
  }
  if ([self isFirstResponder]) {
    [self resignFirstResponder];
  }

  // Clear any pending touch/gesture state
  self.userInteractionEnabled = NO;
  self.userInteractionEnabled = YES;

  // Stop camera when view is recycled
  [self stopRunning];
}

- (void)dealloc
{
  NSLog(@"[VisionSdkViewComponentView] dealloc - cleaning up camera");

  // Resign first responder before dealloc
  if ([_codeScannerView isFirstResponder]) {
    [_codeScannerView resignFirstResponder];
  }
  if ([self isFirstResponder]) {
    [self resignFirstResponder];
  }

  // Stop camera on deallocation
  SEL getter = NSSelectorFromString(@"codeScannerView");
  if ([_codeScannerView respondsToSelector:getter]) {
    id codeScannerView = ((id (*)(id, SEL))objc_msgSend)(_codeScannerView, getter);
    if (codeScannerView) {
      SEL stopRunning = NSSelectorFromString(@"stopRunning");
      if ([codeScannerView respondsToSelector:stopRunning]) {
        ((void (*)(id, SEL))objc_msgSend)(codeScannerView, stopRunning);
      }
    }
  }
}

// MARK: - RCTComponentViewProtocol

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<VisionSdkViewComponentDescriptor>();
}

// MARK: - Props handling

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<VisionSdkViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<VisionSdkViewProps const>(props);

  // Update string properties using objc_msgSend
  if (oldViewProps.mode != newViewProps.mode) {
    SEL setter = NSSelectorFromString(@"setMode:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSString *mode = [NSString stringWithUTF8String:newViewProps.mode.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, mode);
    }
  }

  if (oldViewProps.captureMode != newViewProps.captureMode) {
    SEL setter = NSSelectorFromString(@"setCaptureMode:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSString *captureMode = [NSString stringWithUTF8String:newViewProps.captureMode.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, captureMode);
    }
  }

  if (oldViewProps.apiKey != newViewProps.apiKey) {
    SEL setter = NSSelectorFromString(@"setApiKey:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSString *apiKey = [NSString stringWithUTF8String:newViewProps.apiKey.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, apiKey);
    }
  }

  if (oldViewProps.token != newViewProps.token) {
    SEL setter = NSSelectorFromString(@"setToken:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSString *token = [NSString stringWithUTF8String:newViewProps.token.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, token);
    }
  }

  if (oldViewProps.locationId != newViewProps.locationId) {
    SEL setter = NSSelectorFromString(@"setLocationId:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSString *locationId = [NSString stringWithUTF8String:newViewProps.locationId.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, locationId);
    }
  }

  if (oldViewProps.environment != newViewProps.environment) {
    SEL setter = NSSelectorFromString(@"setEnvironment:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSString *environment = [NSString stringWithUTF8String:newViewProps.environment.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, environment);
    }
  }

  if (oldViewProps.ocrMode != newViewProps.ocrMode) {
    SEL setter = NSSelectorFromString(@"setOcrMode:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSString *ocrMode = [NSString stringWithUTF8String:newViewProps.ocrMode.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, ocrMode);
    }
  }

  if (oldViewProps.ocrType != newViewProps.ocrType) {
    SEL setter = NSSelectorFromString(@"setOcrType:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSString *ocrType = [NSString stringWithUTF8String:newViewProps.ocrType.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, ocrType);
    }
  }

  // Update boolean properties using BOOL primitives
  if (oldViewProps.shouldResizeImage != newViewProps.shouldResizeImage) {
    SEL setter = NSSelectorFromString(@"setShouldResizeImage:");
    if ([_codeScannerView respondsToSelector:setter]) {
      BOOL value = newViewProps.shouldResizeImage;
      ((void (*)(id, SEL, BOOL))objc_msgSend)(_codeScannerView, setter, value);
    }
  }

  if (oldViewProps.flash != newViewProps.flash) {
    SEL setter = NSSelectorFromString(@"setFlash:");
    if ([_codeScannerView respondsToSelector:setter]) {
      BOOL value = newViewProps.flash;
      ((void (*)(id, SEL, BOOL))objc_msgSend)(_codeScannerView, setter, value);
    }
  }

  if (oldViewProps.isEnableAutoOcrResponseWithImage != newViewProps.isEnableAutoOcrResponseWithImage) {
    SEL setter = NSSelectorFromString(@"setIsEnableAutoOcrResponseWithImage:");
    if ([_codeScannerView respondsToSelector:setter]) {
      BOOL value = newViewProps.isEnableAutoOcrResponseWithImage;
      ((void (*)(id, SEL, BOOL))objc_msgSend)(_codeScannerView, setter, value);
    }
  }

  if (oldViewProps.isMultipleScanEnabled != newViewProps.isMultipleScanEnabled) {
    SEL setter = NSSelectorFromString(@"setIsMultipleScanEnabled:");
    if ([_codeScannerView respondsToSelector:setter]) {
      BOOL value = newViewProps.isMultipleScanEnabled;
      ((void (*)(id, SEL, BOOL))objc_msgSend)(_codeScannerView, setter, value);
    }
  }

  // Update number properties
  if (oldViewProps.zoomLevel != newViewProps.zoomLevel) {
    SEL setter = NSSelectorFromString(@"setZoomLevel:");
    if ([_codeScannerView respondsToSelector:setter]) {
      NSNumber *value = @(newViewProps.zoomLevel);
      ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, value);
    }
  }

  // Update object properties (passed as JSON strings)
  if (oldViewProps.optionsJson != newViewProps.optionsJson) {
    SEL setter = NSSelectorFromString(@"setOptions:");
    if ([_codeScannerView respondsToSelector:setter]) {
      if (!newViewProps.optionsJson.empty()) {
        NSString *optionsJson = [NSString stringWithUTF8String:newViewProps.optionsJson.c_str()];
        NSData *data = [optionsJson dataUsingEncoding:NSUTF8StringEncoding];
        if (data) {
          NSDictionary *options = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
          if (options) {
            ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, options);
          }
        }
      } else {
        // Clear options by setting to nil
        ((void (*)(id, SEL, id))objc_msgSend)(_codeScannerView, setter, nil);
      }
    }
  }

  [super updateProps:props oldProps:oldProps];
}

// MARK: - Commands (handleCommand method for imperative calls)

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args
{
  // Handle both string command names and numeric command IDs
  // Cast away const to work with the string
  NSString *actualCommandName = (NSString *)commandName;

  // Check if commandName is a numeric string
  NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
  NSNumber *commandId = [formatter numberFromString:actualCommandName];

  if (commandId != nil) {
    // Map command IDs to command names based on the order in VisionSdkViewManager.m
    // Note: Only the first 4 commands are implemented in Fabric spec currently
    // TODO: Add remaining commands to the Fabric spec
    NSArray *commandNames = @[
      @"captureImage",        // 0
      @"stopRunning",         // 1
      @"startRunning",        // 2
      @"setMetaData",         // 3
      @"setRecipient",        // 4
      @"setSender",           // 5
      @"configureOnDeviceModel", // 6
      @"restartScanning",     // 7
      @"setFocusSettings",    // 8
      @"setObjectDetectionSettings", // 9
      @"setCameraSettings",   // 10
      @"getPrediction",       // 11
      @"getPredictionWithCloudTransformations", // 12
      @"getPredictionShippingLabelCloud", // 13
      @"getPredictionBillOfLadingCloud", // 14
      @"getPredictionItemLabelCloud", // 15
      @"getPredictionDocumentClassificationCloud", // 16
      @"reportError",         // 17
      @"createTemplate",      // 18
      @"getAllTemplates",     // 19
      @"deleteTemplateWithId", // 20
      @"deleteAllTemplates"   // 21
    ];

    NSInteger cmdId = [commandId integerValue];

    if (cmdId >= 0 && cmdId < commandNames.count) {
      actualCommandName = commandNames[cmdId];
      NSLog(@"[VisionSdkViewComponentView] Mapped command ID %ld to '%@'", (long)cmdId, actualCommandName);

      // All commands 0-21 are now implemented in Fabric spec
      // IDs: 0=captureImage, 1=stopRunning, 2=startRunning, 3=setMetaData, 4=setRecipient,
      //      5=setSender, 6=configureOnDeviceModel, 7=restartScanning, 8=setFocusSettings,
      //      9=setObjectDetectionSettings, 10=setCameraSettings, 11=getPrediction,
      //      12=getPredictionWithCloudTransformations, 13=getPredictionShippingLabelCloud,
      //      14=getPredictionBillOfLadingCloud, 15=getPredictionItemLabelCloud,
      //      16=getPredictionDocumentClassificationCloud, 17=reportError,
      //      18=createTemplate, 19=getAllTemplates, 20=deleteTemplateWithId, 21=deleteAllTemplates
      if (cmdId > 21) {
        NSLog(@"[VisionSdkViewComponentView] Command '%@' (ID %ld) not recognized", actualCommandName, (long)cmdId);
        return;
      }
    }
  }

  RCTVisionSdkViewHandleCommand(self, actualCommandName, args);
}

// Implement command handlers using objc_msgSend for dynamic Swift class
- (void)captureImage
{
  // Don't use dispatch_async for user-triggered actions - execute immediately for responsiveness
  SEL getter = NSSelectorFromString(@"codeScannerView");
  if ([_codeScannerView respondsToSelector:getter]) {
    id codeScannerView = ((id (*)(id, SEL))objc_msgSend)(_codeScannerView, getter);
    if (codeScannerView) {
      SEL capturePhoto = NSSelectorFromString(@"capturePhoto");
      if ([codeScannerView respondsToSelector:capturePhoto]) {
        ((void (*)(id, SEL))objc_msgSend)(codeScannerView, capturePhoto);
      }
    }
  }
}

- (void)stopRunning
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ stopRunning: Dispatching to main queue");

  // Dispatch to main queue asynchronously
  dispatch_async(dispatch_get_main_queue(), ^{
    SEL getter = NSSelectorFromString(@"codeScannerView");
    if ([self->_codeScannerView respondsToSelector:getter]) {
      id codeScannerView = ((id (*)(id, SEL))objc_msgSend)(self->_codeScannerView, getter);
      if (codeScannerView) {
        SEL stopRunning = NSSelectorFromString(@"stopRunning");
        if ([codeScannerView respondsToSelector:stopRunning]) {
          ((void (*)(id, SEL))objc_msgSend)(codeScannerView, stopRunning);
          NSLog(@"[VisionSdkViewComponentView] stopRunning completed");
        }
      }
    }
  });
}

- (void)startRunning
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ startRunning called - dispatching to background queue");

  // Dispatch to main queue asynchronously to prevent blocking UI
  dispatch_async(dispatch_get_main_queue(), ^{
    CFAbsoluteTime startTime = CFAbsoluteTimeGetCurrent();
    NSLog(@"[VisionSdkViewComponentView] ⏱️ startRunning executing on main queue");

    SEL getter = NSSelectorFromString(@"codeScannerView");
    if ([self->_codeScannerView respondsToSelector:getter]) {
      id codeScannerView = ((id (*)(id, SEL))objc_msgSend)(self->_codeScannerView, getter);
      if (codeScannerView) {
        SEL startRunning = NSSelectorFromString(@"startRunning");
        if ([codeScannerView respondsToSelector:startRunning]) {
          ((void (*)(id, SEL))objc_msgSend)(codeScannerView, startRunning);

          CFAbsoluteTime endTime = CFAbsoluteTimeGetCurrent();
          NSLog(@"[VisionSdkViewComponentView] ⏱️ startRunning completed in %.3f seconds", endTime - startTime);
        }
      }
    }
  });
}

- (void)restartScanning
{
  // Don't use dispatch_async for user-triggered actions - execute immediately
  SEL getter = NSSelectorFromString(@"codeScannerView");
  if ([_codeScannerView respondsToSelector:getter]) {
    id codeScannerView = ((id (*)(id, SEL))objc_msgSend)(_codeScannerView, getter);
    if (codeScannerView) {
      SEL rescan = NSSelectorFromString(@"rescan");
      if ([codeScannerView respondsToSelector:rescan]) {
        ((void (*)(id, SEL))objc_msgSend)(codeScannerView, rescan);
      }
    }
  }
}

- (void)setMetaData:(NSString *)metaDataJson
{
  NSLog(@"[VisionSdkViewComponentView] setMetaData called with JSON: %@", metaDataJson);

  // Get the nested CodeScannerView
  SEL getter = NSSelectorFromString(@"codeScannerView");
  if (![_codeScannerView respondsToSelector:getter]) return;

  id codeScannerView = ((id (*)(id, SEL))objc_msgSend)(_codeScannerView, getter);
  if (!codeScannerView) return;

  NSData *data = [metaDataJson dataUsingEncoding:NSUTF8StringEncoding];
  if (data) {
    NSDictionary *metaData = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    if (metaData) {
      SEL setter = NSSelectorFromString(@"setMetaData:");
      if ([codeScannerView respondsToSelector:setter]) {
        ((void (*)(id, SEL, id))objc_msgSend)(codeScannerView, setter, metaData);
      }
    }
  }
}

- (void)setRecipient:(NSString *)recipientJson
{
  NSLog(@"[VisionSdkViewComponentView] setRecipient called with JSON: %@", recipientJson);

  // Get the nested CodeScannerView
  SEL getter = NSSelectorFromString(@"codeScannerView");
  if (![_codeScannerView respondsToSelector:getter]) return;

  id codeScannerView = ((id (*)(id, SEL))objc_msgSend)(_codeScannerView, getter);
  if (!codeScannerView) return;

  NSData *data = [recipientJson dataUsingEncoding:NSUTF8StringEncoding];
  if (data) {
    NSDictionary *recipient = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    if (recipient) {
      SEL setter = NSSelectorFromString(@"setRecipient:");
      if ([codeScannerView respondsToSelector:setter]) {
        ((void (*)(id, SEL, id))objc_msgSend)(codeScannerView, setter, recipient);
      }
    }
  }
}

- (void)setSender:(NSString *)senderJson
{
  NSLog(@"[VisionSdkViewComponentView] setSender called with JSON: %@", senderJson);

  // Get the nested CodeScannerView
  SEL getter = NSSelectorFromString(@"codeScannerView");
  if (![_codeScannerView respondsToSelector:getter]) return;

  id codeScannerView = ((id (*)(id, SEL))objc_msgSend)(_codeScannerView, getter);
  if (!codeScannerView) return;

  NSData *data = [senderJson dataUsingEncoding:NSUTF8StringEncoding];
  if (data) {
    NSDictionary *sender = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    if (sender) {
      SEL setter = NSSelectorFromString(@"setSender:");
      if ([codeScannerView respondsToSelector:setter]) {
        ((void (*)(id, SEL, id))objc_msgSend)(codeScannerView, setter, sender);
      }
    }
  }
}

- (void)configureOnDeviceModel:(NSString *)onDeviceConfigsJson token:(NSString *)token apiKey:(NSString *)apiKey
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ configureOnDeviceModel: Dispatching to background queue (heavy operation - model download)");

  // Dispatch to background queue asynchronously - this can take several seconds to download models
  // Using background queue prevents blocking the UI thread
  dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
    CFAbsoluteTime startTime = CFAbsoluteTimeGetCurrent();

    NSData *data = [onDeviceConfigsJson dataUsingEncoding:NSUTF8StringEncoding];
    if (data) {
      NSDictionary *onDeviceConfigs = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];

      if (onDeviceConfigs) {
        NSString *modelType = onDeviceConfigs[@"type"];
        NSString *modelSize = onDeviceConfigs[@"size"];

        SEL configureMethod = NSSelectorFromString(@"configureOnDeviceModelWithModelType:modelSize:token:apiKey:");
        if ([self->_codeScannerView respondsToSelector:configureMethod]) {
          ((void (*)(id, SEL, id, id, id, id))objc_msgSend)(self->_codeScannerView, configureMethod, modelType, modelSize, token, apiKey);

          CFAbsoluteTime endTime = CFAbsoluteTimeGetCurrent();
          NSLog(@"[VisionSdkViewComponentView] ⏱️ configureOnDeviceModel: Completed in %.3f seconds", endTime - startTime);
        }
      }
    }
  });
}

- (void)setFocusSettings:(NSString *)focusSettingsJson
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ setFocusSettings: Dispatching to main queue");

  // Dispatch to main queue
  dispatch_async(dispatch_get_main_queue(), ^{
    NSData *data = [focusSettingsJson dataUsingEncoding:NSUTF8StringEncoding];
    if (data) {
      NSDictionary *focusSettings = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
      if (focusSettings) {
        SEL applyMethod = NSSelectorFromString(@"applyFocusSettings:");
        if ([self->_codeScannerView respondsToSelector:applyMethod]) {
          ((void (*)(id, SEL, id))objc_msgSend)(self->_codeScannerView, applyMethod, focusSettings);
          NSLog(@"[VisionSdkViewComponentView] applyFocusSettings completed");
        }
      }
    }
  });
}

- (void)setObjectDetectionSettings:(NSString *)objectDetectionSettingsJson
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ setObjectDetectionSettings: Dispatching to main queue");

  // Dispatch to main queue
  dispatch_async(dispatch_get_main_queue(), ^{
    NSData *data = [objectDetectionSettingsJson dataUsingEncoding:NSUTF8StringEncoding];
    if (data) {
      NSDictionary *objectDetectionSettings = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
      if (objectDetectionSettings) {
        SEL applyMethod = NSSelectorFromString(@"applyObjectDetectionSettings:");
        if ([self->_codeScannerView respondsToSelector:applyMethod]) {
          ((void (*)(id, SEL, id))objc_msgSend)(self->_codeScannerView, applyMethod, objectDetectionSettings);
          NSLog(@"[VisionSdkViewComponentView] applyObjectDetectionSettings completed");
        }
      }
    }
  });
}

- (void)setCameraSettings:(NSString *)cameraSettingsJson
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ setCameraSettings: Dispatching to main queue");

  // Dispatch to main queue
  dispatch_async(dispatch_get_main_queue(), ^{
    CFAbsoluteTime startTime = CFAbsoluteTimeGetCurrent();

    NSData *data = [cameraSettingsJson dataUsingEncoding:NSUTF8StringEncoding];
    if (data) {
      NSDictionary *cameraSettings = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
      if (cameraSettings) {
        SEL applyMethod = NSSelectorFromString(@"applyCameraSettings:");
        if ([self->_codeScannerView respondsToSelector:applyMethod]) {
          ((void (*)(id, SEL, id))objc_msgSend)(self->_codeScannerView, applyMethod, cameraSettings);

          CFAbsoluteTime endTime = CFAbsoluteTimeGetCurrent();
          NSLog(@"[VisionSdkViewComponentView] ⏱️ setCameraSettings: Completed in %.3f seconds", endTime - startTime);
        }
      }
    }
  });
}

- (void)createTemplate
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ createTemplate: Dispatching to main queue");

  // Dispatch to main queue asynchronously - involves file I/O
  dispatch_async(dispatch_get_main_queue(), ^{
    SEL createTemplateMethod = NSSelectorFromString(@"createTemplate");
    if ([self->_codeScannerView respondsToSelector:createTemplateMethod]) {
      ((void (*)(id, SEL))objc_msgSend)(self->_codeScannerView, createTemplateMethod);
      NSLog(@"[VisionSdkViewComponentView] createTemplate completed");
    }
  });
}

- (void)getAllTemplates
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ getAllTemplates: Dispatching to background queue");

  // Dispatch to main queue asynchronously to prevent blocking UI
  dispatch_async(dispatch_get_main_queue(), ^{
    CFAbsoluteTime startTime = CFAbsoluteTimeGetCurrent();

    SEL getAllTemplatesMethod = NSSelectorFromString(@"getAllTemplates");
    if ([self->_codeScannerView respondsToSelector:getAllTemplatesMethod]) {
      ((void (*)(id, SEL))objc_msgSend)(self->_codeScannerView, getAllTemplatesMethod);

      CFAbsoluteTime endTime = CFAbsoluteTimeGetCurrent();
      NSLog(@"[VisionSdkViewComponentView] ⏱️ getAllTemplates: Completed in %.3f seconds", endTime - startTime);
    } else {
      NSLog(@"[VisionSdkViewComponentView] ERROR: _codeScannerView does not respond to getAllTemplates");
    }
  });
}

- (void)deleteTemplateWithId:(NSString *)templateId
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ deleteTemplateWithId: Dispatching to main queue");

  // Dispatch to main queue asynchronously - involves file I/O
  dispatch_async(dispatch_get_main_queue(), ^{
    SEL deleteTemplateMethod = NSSelectorFromString(@"deleteTemplateWithId:");
    if ([self->_codeScannerView respondsToSelector:deleteTemplateMethod]) {
      ((void (*)(id, SEL, id))objc_msgSend)(self->_codeScannerView, deleteTemplateMethod, templateId);
      NSLog(@"[VisionSdkViewComponentView] deleteTemplateWithId completed");
    }
  });
}

- (void)deleteAllTemplates
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ deleteAllTemplates: Dispatching to main queue");

  // Dispatch to main queue asynchronously - involves file I/O
  dispatch_async(dispatch_get_main_queue(), ^{
    SEL deleteAllTemplatesMethod = NSSelectorFromString(@"deleteAllTemplates");
    if ([self->_codeScannerView respondsToSelector:deleteAllTemplatesMethod]) {
      ((void (*)(id, SEL))objc_msgSend)(self->_codeScannerView, deleteAllTemplatesMethod);
      NSLog(@"[VisionSdkViewComponentView] deleteAllTemplates completed");
    }
  });
}

// MARK: - Cloud Prediction Commands
// Helper to get the manager instance
- (id)getManager
{
  // Get VisionSdkViewManager class
  Class managerClass = NSClassFromString(@"VisionSdkViewManager");
  if (!managerClass) {
    managerClass = NSClassFromString(@"react_native_vision_sdk.VisionSdkViewManager");
  }

  if (managerClass) {
    // Get shared instance or create new one
    SEL sharedInstanceSelector = NSSelectorFromString(@"sharedInstance");
    if ([managerClass respondsToSelector:sharedInstanceSelector]) {
      return ((id (*)(id, SEL))objc_msgSend)(managerClass, sharedInstanceSelector);
    } else {
      // Create a new instance
      return [[managerClass alloc] init];
    }
  }
  return nil;
}

- (void)getPrediction:(NSString *)image barcodeJson:(NSString *)barcodeJson
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ getPrediction: Dispatching to main queue");

  // Dispatch to main queue asynchronously - image loading may be synchronous
  dispatch_async(dispatch_get_main_queue(), ^{
    // Parse barcode JSON to array
    NSArray *barcodeArray = @[];
    if (barcodeJson && barcodeJson.length > 0) {
      NSData *jsonData = [barcodeJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) {
        barcodeArray = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
      }
    }

    // Call the manager method which handles image loading and barcode transformation
    id manager = [self getManager];
    SEL selector = NSSelectorFromString(@"getPrediction:image:barcode:");
    if (manager && [manager respondsToSelector:selector]) {
      NSNumber *tagNumber = @(self.tag);
      ((void (*)(id, SEL, id, id, id))objc_msgSend)(manager, selector, tagNumber, image, barcodeArray);
    }
  });
}

- (void)getPredictionWithCloudTransformations:(NSString *)image
                                 barcodeJson:(NSString *)barcodeJson
                                       token:(NSString *)token
                                      apiKey:(NSString *)apiKey
                                  locationId:(NSString *)locationId
                                 optionsJson:(NSString *)optionsJson
                                metadataJson:(NSString *)metadataJson
                               recipientJson:(NSString *)recipientJson
                                  senderJson:(NSString *)senderJson
                           shouldResizeImage:(BOOL)shouldResizeImage
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ getPredictionWithCloudTransformations: Dispatching to main queue");

  // Dispatch to main queue asynchronously - lots of JSON parsing + image loading
  dispatch_async(dispatch_get_main_queue(), ^{
    // Parse JSON strings
    NSArray *barcodeArray = @[];
    NSDictionary *options = nil;
    NSDictionary *metadata = nil;
    NSDictionary *recipient = nil;
    NSDictionary *sender = nil;

    if (barcodeJson && barcodeJson.length > 0) {
      NSData *jsonData = [barcodeJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) {
        barcodeArray = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
      }
    }

    if (optionsJson && optionsJson.length > 0) {
      NSData *jsonData = [optionsJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) {
        options = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
      }
    }

    if (metadataJson && metadataJson.length > 0) {
      NSData *jsonData = [metadataJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) {
        metadata = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
      }
    }

    if (recipientJson && recipientJson.length > 0) {
      NSData *jsonData = [recipientJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) {
        recipient = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
      }
    }

    if (senderJson && senderJson.length > 0) {
      NSData *jsonData = [senderJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) {
        sender = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
      }
    }

    id manager = [self getManager];
    SEL selector = NSSelectorFromString(@"getPredictionWithCloudTransformations:image:barcode:token:apiKey:locationId:options:metadata:recipient:sender:shouldResizeImage:");
    if (manager && [manager respondsToSelector:selector]) {
      NSNumber *tagNumber = @(self.tag);
      NSNumber *shouldResize = @(shouldResizeImage);
      ((void (*)(id, SEL, id, id, id, id, id, id, id, id, id, id, id))objc_msgSend)(
        manager, selector, tagNumber, image, barcodeArray, token, apiKey, locationId,
        options, metadata, recipient, sender, shouldResize
      );
    }
  });
}

- (void)getPredictionShippingLabelCloud:(NSString *)image
                           barcodeJson:(NSString *)barcodeJson
                                 token:(NSString *)token
                                apiKey:(NSString *)apiKey
                            locationId:(NSString *)locationId
                           optionsJson:(NSString *)optionsJson
                          metadataJson:(NSString *)metadataJson
                         recipientJson:(NSString *)recipientJson
                            senderJson:(NSString *)senderJson
                     shouldResizeImage:(BOOL)shouldResizeImage
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ getPredictionShippingLabelCloud: Dispatching to main queue");

  dispatch_async(dispatch_get_main_queue(), ^{
    // Parse JSON strings
    NSArray *barcodeArray = @[];
    NSDictionary *options = nil;
    NSDictionary *metadata = nil;
    NSDictionary *recipient = nil;
    NSDictionary *sender = nil;

    if (barcodeJson && barcodeJson.length > 0) {
      NSData *jsonData = [barcodeJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) barcodeArray = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    }
    if (optionsJson && optionsJson.length > 0) {
      NSData *jsonData = [optionsJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) options = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    }
    if (metadataJson && metadataJson.length > 0) {
      NSData *jsonData = [metadataJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) metadata = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    }
    if (recipientJson && recipientJson.length > 0) {
      NSData *jsonData = [recipientJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) recipient = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    }
    if (senderJson && senderJson.length > 0) {
      NSData *jsonData = [senderJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) sender = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    }

    id manager = [self getManager];
    SEL selector = NSSelectorFromString(@"getPredictionShippingLabelCloud:image:barcode:token:apiKey:locationId:options:metadata:recipient:sender:shouldResizeImage:");
    if (manager && [manager respondsToSelector:selector]) {
      NSNumber *tagNumber = @(self.tag);
      NSNumber *shouldResize = @(shouldResizeImage);
      ((void (*)(id, SEL, id, id, id, id, id, id, id, id, id, id, id))objc_msgSend)(
        manager, selector, tagNumber, image, barcodeArray, token, apiKey, locationId,
        options, metadata, recipient, sender, shouldResize
      );
    }
  });
}

- (void)getPredictionBillOfLadingCloud:(NSString *)image
                          barcodeJson:(NSString *)barcodeJson
                                token:(NSString *)token
                               apiKey:(NSString *)apiKey
                           locationId:(NSString *)locationId
                          optionsJson:(NSString *)optionsJson
                    shouldResizeImage:(BOOL)shouldResizeImage
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ getPredictionBillOfLadingCloud: Dispatching to main queue");

  dispatch_async(dispatch_get_main_queue(), ^{
    NSArray *barcodeArray = @[];
    NSDictionary *options = nil;

    if (barcodeJson && barcodeJson.length > 0) {
      NSData *jsonData = [barcodeJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) barcodeArray = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    }
    if (optionsJson && optionsJson.length > 0) {
      NSData *jsonData = [optionsJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) options = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    }

    id manager = [self getManager];
    SEL selector = NSSelectorFromString(@"getPredictionBillOfLadingCloud:image:barcode:token:apiKey:locationId:options:shouldResizeImage:");
    if (manager && [manager respondsToSelector:selector]) {
      NSNumber *tagNumber = @(self.tag);
      NSNumber *shouldResize = @(shouldResizeImage);
      ((void (*)(id, SEL, id, id, id, id, id, id, id, id))objc_msgSend)(
        manager, selector, tagNumber, image, barcodeArray, token, apiKey, locationId, options, shouldResize
      );
    }
  });
}

- (void)getPredictionItemLabelCloud:(NSString *)image
                              token:(NSString *)token
                             apiKey:(NSString *)apiKey
                  shouldResizeImage:(BOOL)shouldResizeImage
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ getPredictionItemLabelCloud: Dispatching to main queue");

  dispatch_async(dispatch_get_main_queue(), ^{
    id manager = [self getManager];
    SEL selector = NSSelectorFromString(@"getPredictionItemLabelCloud:image:token:apiKey:shouldResizeImage:");
    if (manager && [manager respondsToSelector:selector]) {
      NSNumber *tagNumber = @(self.tag);
      NSNumber *shouldResize = @(shouldResizeImage);
      ((void (*)(id, SEL, id, id, id, id, id))objc_msgSend)(
        manager, selector, tagNumber, image, token, apiKey, shouldResize
      );
    }
  });
}

- (void)getPredictionDocumentClassificationCloud:(NSString *)image
                                           token:(NSString *)token
                                          apiKey:(NSString *)apiKey
                               shouldResizeImage:(BOOL)shouldResizeImage
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ getPredictionDocumentClassificationCloud: Dispatching to main queue");

  dispatch_async(dispatch_get_main_queue(), ^{
    id manager = [self getManager];
    SEL selector = NSSelectorFromString(@"getPredictionDocumentClassificationCloud:image:token:apiKey:shouldResizeImage:");
    if (manager && [manager respondsToSelector:selector]) {
      NSNumber *tagNumber = @(self.tag);
      NSNumber *shouldResize = @(shouldResizeImage);
      ((void (*)(id, SEL, id, id, id, id, id))objc_msgSend)(
        manager, selector, tagNumber, image, token, apiKey, shouldResize
      );
    }
  });
}

- (void)reportError:(NSString *)dataJson token:(NSString *)token apiKey:(NSString *)apiKey
{
  NSLog(@"[VisionSdkViewComponentView] ⏱️ reportError: Dispatching to main queue");

  // Dispatch to main queue asynchronously - network call + image loading
  dispatch_async(dispatch_get_main_queue(), ^{
    if (!self->_codeScannerView) {
      NSLog(@"[VisionSdkViewComponentView] reportError: No code scanner view available");
      return;
    }

    NSDictionary *data = nil;
    if (dataJson && dataJson.length > 0) {
      NSData *jsonData = [dataJson dataUsingEncoding:NSUTF8StringEncoding];
      if (jsonData) {
        data = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
      }
    }

    if (!data) {
      NSLog(@"[VisionSdkViewComponentView] reportError: Failed to parse data JSON");
      return;
    }

    // Get token from view if not provided
    SEL tokenSelector = NSSelectorFromString(@"token");
    NSString *tokenValue = token;
    if ((!tokenValue || tokenValue.length == 0) && [self->_codeScannerView respondsToSelector:tokenSelector]) {
      tokenValue = ((NSString *(*)(id, SEL))objc_msgSend)(self->_codeScannerView, tokenSelector);
    }

    // Get apiKey - use provided or fall back to VSDKConstants
    NSString *apiKeyValue = (apiKey && apiKey.length > 0) ? apiKey : nil;
    if (!apiKeyValue) {
      Class constantsClass = NSClassFromString(@"VSDKConstants");
      if (constantsClass) {
        SEL apiKeySelector = NSSelectorFromString(@"apiKey");
        if ([constantsClass respondsToSelector:apiKeySelector]) {
          apiKeyValue = ((NSString *(*)(id, SEL))objc_msgSend)(constantsClass, apiKeySelector);
        }
      }
    }

    // Call reportError on the code scanner view
    SEL reportErrorSelector = NSSelectorFromString(@"reportErrorWithUIImage:reportText:response:modelType:modelSize:errorFlags:token:apiKey:");
    if ([self->_codeScannerView respondsToSelector:reportErrorSelector]) {
      NSString *imagePath = data[@"image"];
      UIImage *uiImage = nil;

      // Load image if provided (synchronous file I/O, but already in async block)
      if (imagePath && imagePath.length > 0) {
        if ([imagePath hasPrefix:@"file://"]) {
          NSURL *imageURL = [NSURL URLWithString:imagePath];
          NSData *imageData = [NSData dataWithContentsOfURL:imageURL];
          if (imageData) {
            uiImage = [UIImage imageWithData:imageData];
          }
        }
      }

      ((void (*)(id, SEL, id, id, id, id, id, id, id, id))objc_msgSend)(
        self->_codeScannerView,
        reportErrorSelector,
        uiImage,
        data[@"reportText"] ?: @"",
        data[@"response"],
        data[@"type"] ?: @"shipping_label",
        data[@"size"] ?: @"large",
        data[@"errorFlags"],
        tokenValue,
        apiKeyValue
      );
    } else {
      NSLog(@"[VisionSdkViewComponentView] reportError: Scanner view does not respond to reportError selector");
    }
  });
}

// MARK: - Event Emitters
// These methods show how to emit events when needed
// They will be called from the actual VisionSDK integration

- (void)emitBarcodeScanEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnBarcodeScan event = {};

    // Swift sends: {"codes": [{scannedCode, symbology, boundingBox, ...}]}

    // Serialize the entire codes array to JSON for Fabric
    if ([eventData objectForKey:@"codes"]) {
      NSArray *codes = [eventData objectForKey:@"codes"];

      // Convert codes array to JSON string
      if ([NSJSONSerialization isValidJSONObject:codes]) {
        NSError *error = nil;
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:codes options:0 error:&error];
        if (jsonData && !error) {
          NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
          if (jsonString) {
            event.codesJson = std::string([jsonString UTF8String]);
          }
        }
      }

      // Note: Individual fields (scannedCode, symbology, boundingBox) are NOT populated
      // All barcode data is in the codesJson array which gets parsed on the JS side
    }

    emitter->onBarcodeScan(event);
  }
}

- (void)emitModelDownloadProgressEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnModelDownloadProgress event = {};
    event.progress = [[eventData objectForKey:@"progress"] floatValue];

    if ([eventData objectForKey:@"downloadStatus"]) {
      event.downloadStatus = [[eventData objectForKey:@"downloadStatus"] boolValue];
    }
    if ([eventData objectForKey:@"isReady"]) {
      event.isReady = [[eventData objectForKey:@"isReady"] boolValue];
    }

    emitter->onModelDownloadProgress(event);
  }
}

- (void)emitImageCapturedEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnImageCaptured event = {};

    if ([eventData objectForKey:@"image"]) {
      event.uri = std::string([[eventData objectForKey:@"image"] UTF8String]);
    }

    emitter->onImageCaptured(event);
  }
}

- (void)emitSharpnessScoreEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnSharpnessScore event = {};
    event.sharpnessScore = [[eventData objectForKey:@"sharpnessScore"] floatValue];

    emitter->onSharpnessScore(event);
  }
}

- (void)emitOCRScanEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnOCRScan event = {};

    // Convert entire data dict to JSON string
    if ([eventData objectForKey:@"data"]) {
      NSDictionary *data = [eventData objectForKey:@"data"];
      NSData *jsonData = [NSJSONSerialization dataWithJSONObject:data options:0 error:nil];
      if (jsonData) {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        event.dataJson = std::string([jsonString UTF8String]);
      }
    }

    emitter->onOCRScan(event);
  }
}

- (void)emitErrorEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnError event = {};
    event.message = std::string([[eventData objectForKey:@"message"] UTF8String] ?: "");

    emitter->onError(event);
  }
}

- (void)emitBoundingBoxesDetectedEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnBoundingBoxesDetected event = {};

    // Convert arrays to JSON strings
    if ([eventData objectForKey:@"barcodeBoundingBoxes"]) {
      NSArray *boxes = [eventData objectForKey:@"barcodeBoundingBoxes"];
      NSData *jsonData = [NSJSONSerialization dataWithJSONObject:boxes options:0 error:nil];
      if (jsonData) {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        event.barcodeBoundingBoxesJson = std::string([jsonString UTF8String]);
      }
    }

    if ([eventData objectForKey:@"qrCodeBoundingBoxes"]) {
      NSArray *boxes = [eventData objectForKey:@"qrCodeBoundingBoxes"];
      NSData *jsonData = [NSJSONSerialization dataWithJSONObject:boxes options:0 error:nil];
      if (jsonData) {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        event.qrCodeBoundingBoxesJson = std::string([jsonString UTF8String]);
      }
    }

    // documentBoundingBox is a single object
    if ([eventData objectForKey:@"documentBoundingBox"]) {
      NSDictionary *box = [eventData objectForKey:@"documentBoundingBox"];
      VisionSdkViewEventEmitter::OnBoundingBoxesDetectedDocumentBoundingBox docBox = {};
      docBox.x = [[box objectForKey:@"x"] floatValue];
      docBox.y = [[box objectForKey:@"y"] floatValue];
      docBox.width = [[box objectForKey:@"width"] floatValue];
      docBox.height = [[box objectForKey:@"height"] floatValue];
      event.documentBoundingBox = docBox;
    }

    emitter->onBoundingBoxesDetected(event);
  }
}

- (void)emitPriceTagDetectedEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnPriceTagDetected event = {};

    if ([eventData objectForKey:@"price"]) {
      event.price = std::string([[eventData objectForKey:@"price"] UTF8String]);
    }
    if ([eventData objectForKey:@"sku"]) {
      event.sku = std::string([[eventData objectForKey:@"sku"] UTF8String]);
    }

    if ([eventData objectForKey:@"boundingBox"]) {
      NSDictionary *box = [eventData objectForKey:@"boundingBox"];
      VisionSdkViewEventEmitter::OnPriceTagDetectedBoundingBox bbox = {};
      bbox.x = [[box objectForKey:@"x"] floatValue];
      bbox.y = [[box objectForKey:@"y"] floatValue];
      bbox.width = [[box objectForKey:@"width"] floatValue];
      bbox.height = [[box objectForKey:@"height"] floatValue];
      event.boundingBox = bbox;
    }

    emitter->onPriceTagDetected(event);
  }
}

- (void)emitDetectedEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnDetected event = {};

    // Convert data to JSON string
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:eventData options:0 error:nil];
    if (jsonData) {
      NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
      event.dataJson = std::string([jsonString UTF8String]);
    }

    emitter->onDetected(event);
  }
}

- (void)emitCreateTemplateEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnCreateTemplate event = {};
    event.success = true;

    // Convert data to JSON string
    if ([eventData objectForKey:@"data"]) {
      id data = [eventData objectForKey:@"data"];
      if ([data isKindOfClass:[NSString class]]) {
        event.dataJson = std::string([data UTF8String]);
      }
    }

    emitter->onCreateTemplate(event);
  }
}

- (void)emitGetTemplatesEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnGetTemplates event = {};
    event.success = true;

    // Convert data to JSON string
    if ([eventData objectForKey:@"data"]) {
      id data = [eventData objectForKey:@"data"];
      NSData *jsonData = [NSJSONSerialization dataWithJSONObject:data options:0 error:nil];
      if (jsonData) {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        event.dataJson = std::string([jsonString UTF8String]);
      }
    }

    emitter->onGetTemplates(event);
  }
}

- (void)emitDeleteTemplateByIdEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnDeleteTemplateById event = {};
    event.success = true;

    // Convert data to JSON string
    if ([eventData objectForKey:@"data"]) {
      id data = [eventData objectForKey:@"data"];
      if ([data isKindOfClass:[NSString class]]) {
        event.dataJson = std::string([data UTF8String]);
      }
    }

    emitter->onDeleteTemplateById(event);
  }
}

- (void)emitDeleteTemplatesEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionSdkViewEventEmitter const>(_eventEmitter);

    VisionSdkViewEventEmitter::OnDeleteTemplates event = {};
    event.success = true;

    // Convert data to JSON string
    if ([eventData objectForKey:@"data"]) {
      id data = [eventData objectForKey:@"data"];
      if ([data isKindOfClass:[NSString class]]) {
        event.dataJson = std::string([data UTF8String]);
      }
    }

    emitter->onDeleteTemplates(event);
  }
}

@end

Class<RCTComponentViewProtocol> VisionSdkViewCls(void)
{
  return VisionSdkViewComponentView.class;
}

#endif // RCT_NEW_ARCH_ENABLED
