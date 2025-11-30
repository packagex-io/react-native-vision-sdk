#ifdef RCT_NEW_ARCH_ENABLED

#import "VisionCameraViewComponentView.h"

#import <react/renderer/components/VisionSdkView/ComponentDescriptors.h>
#import <react/renderer/components/VisionSdkView/EventEmitters.h>
#import <react/renderer/components/VisionSdkView/Props.h>
#import <react/renderer/components/VisionSdkView/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"
#import <objc/message.h>

using namespace facebook::react;

@interface VisionCameraViewComponentView () <RCTVisionCameraViewViewProtocol>
@end

@implementation VisionCameraViewComponentView {
  UIView *_visionCameraView;
}

// MARK: - Initialization

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const VisionCameraViewProps>();
    _props = defaultProps;

    // Create the actual VisionSDK camera view
    Class RNVisionCameraViewClass = NSClassFromString(@"RNVisionCameraView");
    NSLog(@"[VisionCameraViewComponentView] RNVisionCameraView class: %@", RNVisionCameraViewClass);
    if (RNVisionCameraViewClass) {
      NSLog(@"[VisionCameraViewComponentView] Creating RNVisionCameraView with bounds: %@", NSStringFromCGRect(self.bounds));
      _visionCameraView = [[RNVisionCameraViewClass alloc] initWithFrame:self.bounds];
      NSLog(@"[VisionCameraViewComponentView] Created view: %@", _visionCameraView);

      // Set up event blocks to bridge Swift events to Fabric event emitters
      // These blocks will be called by the Swift view when events occur
      __weak VisionCameraViewComponentView *weakSelf = self;

      // Use direct property assignment with performSelector to avoid KVC issues
      SEL onCaptureSetter = NSSelectorFromString(@"setOnCapture:");
      if ([_visionCameraView respondsToSelector:onCaptureSetter]) {
        id captureBlock = ^(NSDictionary *event) {
          [weakSelf emitCaptureEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, onCaptureSetter, captureBlock);
      }

      SEL onErrorSetter = NSSelectorFromString(@"setOnError:");
      if ([_visionCameraView respondsToSelector:onErrorSetter]) {
        id errorBlock = ^(NSDictionary *event) {
          [weakSelf emitErrorEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, onErrorSetter, errorBlock);
      }

      SEL onRecognitionUpdateSetter = NSSelectorFromString(@"setOnRecognitionUpdate:");
      if ([_visionCameraView respondsToSelector:onRecognitionUpdateSetter]) {
        id recognitionBlock = ^(NSDictionary *event) {
          [weakSelf emitRecognitionUpdateEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, onRecognitionUpdateSetter, recognitionBlock);
      }

      SEL onSharpnessScoreUpdateSetter = NSSelectorFromString(@"setOnSharpnessScoreUpdate:");
      if ([_visionCameraView respondsToSelector:onSharpnessScoreUpdateSetter]) {
        id sharpnessBlock = ^(NSDictionary *event) {
          [weakSelf emitSharpnessScoreUpdateEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, onSharpnessScoreUpdateSetter, sharpnessBlock);
      }

      SEL onBarcodeDetectedSetter = NSSelectorFromString(@"setOnBarcodeDetected:");
      if ([_visionCameraView respondsToSelector:onBarcodeDetectedSetter]) {
        id barcodeBlock = ^(NSDictionary *event) {
          [weakSelf emitBarcodeDetectedEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, onBarcodeDetectedSetter, barcodeBlock);
      }

      SEL onBoundingBoxesUpdateSetter = NSSelectorFromString(@"setOnBoundingBoxesUpdate:");
      if ([_visionCameraView respondsToSelector:onBoundingBoxesUpdateSetter]) {
        id boundingBoxBlock = ^(NSDictionary *event) {
          [weakSelf emitBoundingBoxesUpdateEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, onBoundingBoxesUpdateSetter, boundingBoxBlock);
      }
    } else {
      // Fallback to placeholder if Swift class not available
      _visionCameraView = [[UIView alloc] initWithFrame:self.bounds];
      _visionCameraView.backgroundColor = [UIColor darkGrayColor];
    }

    _visionCameraView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    self.contentView = _visionCameraView;
  }

  return self;
}

- (void)layoutSubviews
{
  [super layoutSubviews];

  NSLog(@"[VisionCameraViewComponentView] layoutSubviews called with bounds: %@", NSStringFromCGRect(self.bounds));

  // Update frame before calling layoutSubviews
  _visionCameraView.frame = self.bounds;
  NSLog(@"[VisionCameraViewComponentView] Set camera view frame to: %@", NSStringFromCGRect(_visionCameraView.frame));

  // Trigger layoutSubviews on the Swift view to start camera
  // The Swift view auto-starts the camera in its layoutSubviews when bounds are valid
  if ([_visionCameraView respondsToSelector:@selector(layoutSubviews)]) {
    [_visionCameraView performSelector:@selector(layoutSubviews)];
    NSLog(@"[VisionCameraViewComponentView] Called layoutSubviews on camera view");
  }
}

- (void)didMoveToWindow
{
  [super didMoveToWindow];

  NSLog(@"[VisionCameraViewComponentView] didMoveToWindow, window: %@", self.window);

  if (self.window) {
    // Added to window - trigger layout which will auto-start camera
    [self setNeedsLayout];
    [self layoutIfNeeded];
  } else {
    // Removed from window - stop camera
    if ([_visionCameraView respondsToSelector:@selector(stop)]) {
      NSLog(@"[VisionCameraViewComponentView] Calling stop() on camera view");
      [_visionCameraView performSelector:@selector(stop)];
    }
  }
}

- (void)prepareForRecycle
{
  [super prepareForRecycle];

  NSLog(@"[VisionCameraViewComponentView] prepareForRecycle - stopping camera");

  // Stop camera when view is recycled
  if ([_visionCameraView respondsToSelector:@selector(stop)]) {
    [_visionCameraView performSelector:@selector(stop)];
  }
}

- (void)dealloc
{
  NSLog(@"[VisionCameraViewComponentView] dealloc - cleaning up camera");

  // Stop camera on deallocation
  if ([_visionCameraView respondsToSelector:@selector(stop)]) {
    [_visionCameraView performSelector:@selector(stop)];
  }
}

// MARK: - RCTComponentViewProtocol

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<VisionCameraViewComponentDescriptor>();
}

// MARK: - Props handling

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<VisionCameraViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<VisionCameraViewProps const>(props);

  // Update boolean properties using direct setter calls
  if (oldViewProps.enableFlash != newViewProps.enableFlash) {
    SEL setter = NSSelectorFromString(@"setEnableFlash:");
    if ([_visionCameraView respondsToSelector:setter]) {
      BOOL value = newViewProps.enableFlash;
      NSLog(@"[VisionCameraViewComponentView] Setting enableFlash to: %d", value);
      // Use the correct signature for BOOL parameter
      ((void (*)(id, SEL, BOOL))objc_msgSend)(_visionCameraView, setter, value);
    }
  }

  if (oldViewProps.autoCapture != newViewProps.autoCapture) {
    SEL setter = NSSelectorFromString(@"setAutoCapture:");
    if ([_visionCameraView respondsToSelector:setter]) {
      BOOL value = newViewProps.autoCapture;
      NSLog(@"[VisionCameraViewComponentView] Setting autoCapture to: %d", value);
      // Use the correct signature for BOOL parameter
      ((void (*)(id, SEL, BOOL))objc_msgSend)(_visionCameraView, setter, value);
    }
  }

  // Update number properties
  if (oldViewProps.zoomLevel != newViewProps.zoomLevel) {
    SEL setter = NSSelectorFromString(@"setZoomLevel:");
    if ([_visionCameraView respondsToSelector:setter]) {
      NSNumber *value = @(newViewProps.zoomLevel);
      ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, setter, value);
    }
  }

  if (oldViewProps.frameSkip != newViewProps.frameSkip) {
    SEL setter = NSSelectorFromString(@"setFrameSkip:");
    if ([_visionCameraView respondsToSelector:setter]) {
      NSNumber *value = @(newViewProps.frameSkip);
      ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, setter, value);
    }
  }

  // Update string properties
  if (oldViewProps.scanMode != newViewProps.scanMode) {
    SEL setter = NSSelectorFromString(@"setScanMode:");
    if ([_visionCameraView respondsToSelector:setter]) {
      NSString *scanMode = [NSString stringWithUTF8String:newViewProps.scanMode.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, setter, scanMode);
    }
  }

  if (oldViewProps.cameraFacing != newViewProps.cameraFacing) {
    SEL setter = NSSelectorFromString(@"setCameraFacing:");
    if ([_visionCameraView respondsToSelector:setter]) {
      NSString *cameraFacing = [NSString stringWithUTF8String:newViewProps.cameraFacing.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, setter, cameraFacing);
    }
  }

  // Update object properties (passed as JSON strings)
  if (oldViewProps.scanAreaJson != newViewProps.scanAreaJson) {
    SEL setter = NSSelectorFromString(@"setScanArea:");
    if ([_visionCameraView respondsToSelector:setter]) {
      if (!newViewProps.scanAreaJson.empty()) {
        NSString *scanAreaJson = [NSString stringWithUTF8String:newViewProps.scanAreaJson.c_str()];
        NSData *data = [scanAreaJson dataUsingEncoding:NSUTF8StringEncoding];
        if (data) {
          NSDictionary *scanArea = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
          if (scanArea) {
            NSLog(@"[VisionCameraViewComponentView] Setting scanArea: %@", scanArea);
            ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, setter, scanArea);
          }
        }
      } else {
        // Clear scanArea by setting it to nil
        NSLog(@"[VisionCameraViewComponentView] Clearing scanArea (setting to nil)");
        ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, setter, nil);
      }
    }
  }

  if (oldViewProps.detectionConfigJson != newViewProps.detectionConfigJson) {
    if (!newViewProps.detectionConfigJson.empty()) {
      SEL setter = NSSelectorFromString(@"setDetectionConfig:");
      if ([_visionCameraView respondsToSelector:setter]) {
        NSString *configJson = [NSString stringWithUTF8String:newViewProps.detectionConfigJson.c_str()];
        NSData *data = [configJson dataUsingEncoding:NSUTF8StringEncoding];
        if (data) {
          NSDictionary *config = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
          if (config) {
            ((void (*)(id, SEL, id))objc_msgSend)(_visionCameraView, setter, config);
          }
        }
      }
    }
  }

  [super updateProps:props oldProps:oldProps];
}

// MARK: - Commands

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args
{
  // Handle both string command names and numeric command IDs
  // Fabric sometimes sends commands as numbers in string format (e.g., @"0", @"1")
  // Cast away const to work with the string
  NSString *actualCommandName = (NSString *)commandName;

  // Check if commandName is a numeric string
  NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
  NSNumber *commandId = [formatter numberFromString:actualCommandName];

  if (commandId != nil) {
    // Map command IDs to command names based on the order in supportedCommands array
    // From VisionCameraViewNativeComponent.ts: ['capture', 'stop', 'start', 'toggleFlash', 'setZoom']
    NSArray *commandNames = @[@"capture", @"stop", @"start", @"toggleFlash", @"setZoom"];

    NSInteger cmdId = [commandId integerValue];
    if (cmdId >= 0 && cmdId < commandNames.count) {
      actualCommandName = commandNames[cmdId];
      NSLog(@"[VisionCameraViewComponentView] Mapped command ID %ld to '%@'", (long)cmdId, actualCommandName);
    }
  }

  RCTVisionCameraViewHandleCommand(self, actualCommandName, args);
}

- (void)capture
{
  if ([_visionCameraView respondsToSelector:@selector(capture)]) {
    [_visionCameraView performSelector:@selector(capture)];
  }
}

- (void)stop
{
  if ([_visionCameraView respondsToSelector:@selector(stop)]) {
    [_visionCameraView performSelector:@selector(stop)];
  }
}

- (void)start
{
  if ([_visionCameraView respondsToSelector:@selector(start)]) {
    [_visionCameraView performSelector:@selector(start)];
  }
}

- (void)toggleFlash:(BOOL)enabled
{
  SEL selector = NSSelectorFromString(@"toggleFlashWithEnabled:");
  if ([_visionCameraView respondsToSelector:selector]) {
    NSMethodSignature *signature = [_visionCameraView methodSignatureForSelector:selector];
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:signature];
    [invocation setSelector:selector];
    [invocation setTarget:_visionCameraView];
    [invocation setArgument:&enabled atIndex:2];
    [invocation invoke];
  }
}

- (void)setZoom:(CGFloat)level
{
  SEL selector = NSSelectorFromString(@"setZoomWithLevel:");
  if ([_visionCameraView respondsToSelector:selector]) {
    NSMethodSignature *signature = [_visionCameraView methodSignatureForSelector:selector];
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:signature];
    [invocation setSelector:selector];
    [invocation setTarget:_visionCameraView];
    [invocation setArgument:&level atIndex:2];
    [invocation invoke];
  }
}

// MARK: - Event Emitters
// Convert Swift/ObjC events to Fabric C++ events

- (void)emitCaptureEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionCameraViewEventEmitter const>(_eventEmitter);

    VisionCameraViewEventEmitter::OnCapture event = {};
    event.image = std::string([[eventData objectForKey:@"image"] UTF8String] ?: "");

    if ([eventData objectForKey:@"nativeImage"]) {
      event.nativeImage = std::string([[eventData objectForKey:@"nativeImage"] UTF8String]);
    }

    if ([eventData objectForKey:@"sharpnessScore"]) {
      event.sharpnessScore = [[eventData objectForKey:@"sharpnessScore"] floatValue];
    }

    // Convert barcodes array to JSON string
    if ([eventData objectForKey:@"barcodes"]) {
      NSArray *barcodes = [eventData objectForKey:@"barcodes"];
      NSData *jsonData = [NSJSONSerialization dataWithJSONObject:barcodes options:0 error:nil];
      if (jsonData) {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        event.barcodesJson = std::string([jsonString UTF8String]);
      }
    }

    emitter->onCapture(event);
  }
}

- (void)emitErrorEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionCameraViewEventEmitter const>(_eventEmitter);

    VisionCameraViewEventEmitter::OnError event = {};
    event.message = std::string([[eventData objectForKey:@"message"] UTF8String] ?: "");

    emitter->onError(event);
  }
}

- (void)emitRecognitionUpdateEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionCameraViewEventEmitter const>(_eventEmitter);

    VisionCameraViewEventEmitter::OnRecognitionUpdate event = {};
    event.text = [[eventData objectForKey:@"text"] boolValue];
    event.barcode = [[eventData objectForKey:@"barcode"] boolValue];
    event.qrcode = [[eventData objectForKey:@"qrcode"] boolValue];
    event.document = [[eventData objectForKey:@"document"] boolValue];

    emitter->onRecognitionUpdate(event);
  }
}

- (void)emitSharpnessScoreUpdateEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionCameraViewEventEmitter const>(_eventEmitter);

    VisionCameraViewEventEmitter::OnSharpnessScoreUpdate event = {};
    event.sharpnessScore = [[eventData objectForKey:@"sharpnessScore"] floatValue];

    emitter->onSharpnessScoreUpdate(event);
  }
}

- (void)emitBarcodeDetectedEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionCameraViewEventEmitter const>(_eventEmitter);

    VisionCameraViewEventEmitter::OnBarcodeDetected event = {};

    // Convert codes array to JSON string
    if ([eventData objectForKey:@"codes"]) {
      NSArray *codes = [eventData objectForKey:@"codes"];
      NSData *jsonData = [NSJSONSerialization dataWithJSONObject:codes options:0 error:nil];
      if (jsonData) {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        event.codesJson = std::string([jsonString UTF8String]);
      }
    }

    emitter->onBarcodeDetected(event);
  }
}

- (void)emitBoundingBoxesUpdateEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<VisionCameraViewEventEmitter const>(_eventEmitter);

    VisionCameraViewEventEmitter::OnBoundingBoxesUpdate event = {};

    // Convert barcodeBoundingBoxes array to JSON string
    if ([eventData objectForKey:@"barcodeBoundingBoxes"]) {
      NSArray *boxes = [eventData objectForKey:@"barcodeBoundingBoxes"];
      NSData *jsonData = [NSJSONSerialization dataWithJSONObject:boxes options:0 error:nil];
      if (jsonData) {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        event.barcodeBoundingBoxesJson = std::string([jsonString UTF8String]);
      }
    }

    // Convert qrCodeBoundingBoxes array to JSON string
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
      VisionCameraViewEventEmitter::OnBoundingBoxesUpdateDocumentBoundingBox docBox = {};
      docBox.x = [[box objectForKey:@"x"] floatValue];
      docBox.y = [[box objectForKey:@"y"] floatValue];
      docBox.width = [[box objectForKey:@"width"] floatValue];
      docBox.height = [[box objectForKey:@"height"] floatValue];
      event.documentBoundingBox = docBox;
    }

    emitter->onBoundingBoxesUpdate(event);
  }
}

@end

Class<RCTComponentViewProtocol> VisionCameraViewCls(void)
{
  return VisionCameraViewComponentView.class;
}

#endif // RCT_NEW_ARCH_ENABLED
