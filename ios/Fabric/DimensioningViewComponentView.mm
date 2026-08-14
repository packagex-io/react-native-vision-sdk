#ifdef RCT_NEW_ARCH_ENABLED

#import "DimensioningViewComponentView.h"

#import <react/renderer/components/VisionSdkSpec/ComponentDescriptors.h>
#import <react/renderer/components/VisionSdkSpec/EventEmitters.h>
#import <react/renderer/components/VisionSdkSpec/Props.h>
#import <react/renderer/components/VisionSdkSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"
#import <objc/message.h>

using namespace facebook::react;

@interface DimensioningViewComponentView () <RCTDimensioningViewViewProtocol>
@end

@implementation DimensioningViewComponentView {
  UIView *_dimensioningView;
}

// MARK: - Initialization

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const DimensioningViewProps>();
    _props = defaultProps;

    // Instantiate the Swift view at runtime — avoids importing the bridging header
    // (same technique as VisionCameraViewComponentView).
    Class RNDimensioningViewClass = NSClassFromString(@"RNDimensioningView");
    if (RNDimensioningViewClass) {
      _dimensioningView = [[RNDimensioningViewClass alloc] initWithFrame:self.bounds];

      __weak DimensioningViewComponentView *weakSelf = self;

      // Wire onCapture
      SEL onCaptureSetter = NSSelectorFromString(@"setOnCapture:");
      if ([_dimensioningView respondsToSelector:onCaptureSetter]) {
        id captureBlock = ^(NSDictionary *event) {
          [weakSelf emitCaptureEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, onCaptureSetter, captureBlock);
      }

      // Wire onError
      SEL onErrorSetter = NSSelectorFromString(@"setOnError:");
      if ([_dimensioningView respondsToSelector:onErrorSetter]) {
        id errorBlock = ^(NSDictionary *event) {
          [weakSelf emitErrorEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, onErrorSetter, errorBlock);
      }

      // Wire onMeasurementUpdate
      SEL onUpdateSetter = NSSelectorFromString(@"setOnMeasurementUpdate:");
      if ([_dimensioningView respondsToSelector:onUpdateSetter]) {
        id updateBlock = ^(NSDictionary *event) {
          [weakSelf emitMeasurementUpdateEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, onUpdateSetter, updateBlock);
      }

      // Wire onOverlayUpdate
      SEL onOverlaySetter = NSSelectorFromString(@"setOnOverlayUpdate:");
      if ([_dimensioningView respondsToSelector:onOverlaySetter]) {
        id overlayBlock = ^(NSDictionary *event) {
          [weakSelf emitOverlayUpdateEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, onOverlaySetter, overlayBlock);
      }

      // Wire onTelemetry
      SEL onTelemetrySetter = NSSelectorFromString(@"setOnTelemetry:");
      if ([_dimensioningView respondsToSelector:onTelemetrySetter]) {
        id telemetryBlock = ^(NSDictionary *event) {
          [weakSelf emitTelemetryEvent:event];
        };
        ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, onTelemetrySetter, telemetryBlock);
      }
    } else {
      // Fallback placeholder when Swift class not available (simulator without LiDAR)
      _dimensioningView = [[UIView alloc] initWithFrame:self.bounds];
      _dimensioningView.backgroundColor = [UIColor darkGrayColor];
    }

    _dimensioningView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    self.contentView = _dimensioningView;
  }

  return self;
}

- (void)layoutSubviews
{
  [super layoutSubviews];

  _dimensioningView.frame = self.bounds;

  if ([_dimensioningView respondsToSelector:@selector(layoutSubviews)]) {
    [_dimensioningView performSelector:@selector(layoutSubviews)];
  }
}

- (void)didMoveToWindow
{
  [super didMoveToWindow];

  if (self.window) {
    [self setNeedsLayout];
    [self layoutIfNeeded];
  }
}

- (void)prepareForRecycle
{
  [super prepareForRecycle];
}

// MARK: - RCTComponentViewProtocol

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<DimensioningViewComponentDescriptor>();
}

// MARK: - Props handling

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<DimensioningViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<DimensioningViewProps const>(props);

  if (oldViewProps.mode != newViewProps.mode) {
    SEL setter = NSSelectorFromString(@"setMode:");
    if ([_dimensioningView respondsToSelector:setter]) {
      NSString *value = [NSString stringWithUTF8String:newViewProps.mode.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, setter, value);
    }
  }

  if (oldViewProps.measurementUnit != newViewProps.measurementUnit) {
    SEL setter = NSSelectorFromString(@"setMeasurementUnit:");
    if ([_dimensioningView respondsToSelector:setter]) {
      NSString *value = [NSString stringWithUTF8String:newViewProps.measurementUnit.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, setter, value);
    }
  }

  if (oldViewProps.overlayMode != newViewProps.overlayMode) {
    SEL setter = NSSelectorFromString(@"setOverlayMode:");
    if ([_dimensioningView respondsToSelector:setter]) {
      NSString *value = [NSString stringWithUTF8String:newViewProps.overlayMode.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, setter, value);
    }
  }

  if (oldViewProps.cloudUrl != newViewProps.cloudUrl) {
    SEL setter = NSSelectorFromString(@"setCloudUrl:");
    if ([_dimensioningView respondsToSelector:setter]) {
      NSString *value = [NSString stringWithUTF8String:newViewProps.cloudUrl.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, setter, value);
    }
  }

  if (oldViewProps.cloudApiKey != newViewProps.cloudApiKey) {
    SEL setter = NSSelectorFromString(@"setCloudApiKey:");
    if ([_dimensioningView respondsToSelector:setter]) {
      NSString *value = [NSString stringWithUTF8String:newViewProps.cloudApiKey.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, setter, value);
    }
  }

  if (oldViewProps.cloudSdkId != newViewProps.cloudSdkId) {
    SEL setter = NSSelectorFromString(@"setCloudSdkId:");
    if ([_dimensioningView respondsToSelector:setter]) {
      NSString *value = [NSString stringWithUTF8String:newViewProps.cloudSdkId.c_str()];
      ((void (*)(id, SEL, id))objc_msgSend)(_dimensioningView, setter, value);
    }
  }

  if (oldViewProps.enableTelemetry != newViewProps.enableTelemetry) {
    SEL setter = NSSelectorFromString(@"setEnableTelemetry:");
    if ([_dimensioningView respondsToSelector:setter]) {
      BOOL value = newViewProps.enableTelemetry;
      NSInvocation *inv = [NSInvocation invocationWithMethodSignature:
        [_dimensioningView methodSignatureForSelector:setter]];
      [inv setSelector:setter];
      [inv setTarget:_dimensioningView];
      [inv setArgument:&value atIndex:2];
      [inv invoke];
    }
  }

  if (oldViewProps.maximumTrackCount != newViewProps.maximumTrackCount) {
    SEL setter = NSSelectorFromString(@"setMaximumTrackCount:");
    if ([_dimensioningView respondsToSelector:setter]) {
      NSInteger value = (NSInteger)newViewProps.maximumTrackCount;
      NSInvocation *inv = [NSInvocation invocationWithMethodSignature:
        [_dimensioningView methodSignatureForSelector:setter]];
      [inv setSelector:setter];
      [inv setTarget:_dimensioningView];
      [inv setArgument:&value atIndex:2];
      [inv invoke];
    }
  }

  [super updateProps:props oldProps:oldProps];
}

// MARK: - Commands

// `stop` releases the rear camera (ARKit and AVCaptureSession cannot share it);
// `start` re-creates the AR view. The vendor's DimensioningSession.shutdown() is
// not reachable from here -- DimensioningView owns its session privately -- so
// these drop and rebuild the hosted view instead.
- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args
{
  NSString *actualCommandName = (NSString *)commandName;

  // Fabric normally delivers the literal command name; the numeric fallback
  // mirrors VisionCameraViewComponentView. Order matches supportedCommands in
  // src/specs/DimensioningViewNativeComponent.ts and is APPEND-ONLY -- an id
  // resolved against a different JS bundle's ordering dispatches the wrong command.
  NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
  NSNumber *commandId = [formatter numberFromString:actualCommandName];
  if (commandId != nil) {
    NSArray *commandNames = @[@"stop", @"start"];
    NSInteger cmdId = [commandId integerValue];
    if (cmdId >= 0 && cmdId < commandNames.count) {
      actualCommandName = commandNames[cmdId];
    }
  }

  RCTDimensioningViewHandleCommand(self, actualCommandName, args);
}

- (void)stop
{
  SEL selector = NSSelectorFromString(@"stopDimensioning");
  if ([_dimensioningView respondsToSelector:selector]) {
    ((void (*)(id, SEL))objc_msgSend)(_dimensioningView, selector);
  }
}

- (void)start
{
  SEL selector = NSSelectorFromString(@"startDimensioning");
  if ([_dimensioningView respondsToSelector:selector]) {
    ((void (*)(id, SEL))objc_msgSend)(_dimensioningView, selector);
  }
}

// MARK: - Event Emitters

- (void)emitCaptureEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<DimensioningViewEventEmitter const>(_eventEmitter);

    DimensioningViewEventEmitter::OnCapture event = {};
    if (NSString *measurementJson = [eventData objectForKey:@"measurementJson"]) {
      event.measurementJson = std::string([measurementJson UTF8String]);
    }
    emitter->onCapture(event);
  }
}

- (void)emitMeasurementUpdateEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<DimensioningViewEventEmitter const>(_eventEmitter);

    DimensioningViewEventEmitter::OnMeasurementUpdate event = {};
    if (NSString *updateJson = [eventData objectForKey:@"updateJson"]) {
      event.updateJson = std::string([updateJson UTF8String]);
    }
    emitter->onMeasurementUpdate(event);
  }
}

- (void)emitOverlayUpdateEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<DimensioningViewEventEmitter const>(_eventEmitter);

    DimensioningViewEventEmitter::OnOverlayUpdate event = {};
    if (NSString *overlayJson = [eventData objectForKey:@"overlayJson"]) {
      event.overlayJson = std::string([overlayJson UTF8String]);
    }
    emitter->onOverlayUpdate(event);
  }
}

- (void)emitTelemetryEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<DimensioningViewEventEmitter const>(_eventEmitter);

    DimensioningViewEventEmitter::OnTelemetry event = {};
    if (NSString *telemetryJson = [eventData objectForKey:@"telemetryJson"]) {
      event.telemetryJson = std::string([telemetryJson UTF8String]);
    }
    emitter->onTelemetry(event);
  }
}

- (void)emitErrorEvent:(NSDictionary *)eventData
{
  if (_eventEmitter != nullptr) {
    auto emitter = std::static_pointer_cast<DimensioningViewEventEmitter const>(_eventEmitter);

    DimensioningViewEventEmitter::OnError event = {};
    event.code = [[eventData objectForKey:@"code"] intValue];
    event.message = std::string([[eventData objectForKey:@"message"] UTF8String] ?: "");
    if (NSString *reason = [eventData objectForKey:@"reason"]) {
      event.reason = std::string([reason UTF8String]);
    }
    emitter->onError(event);
  }
}

@end

Class<RCTComponentViewProtocol> DimensioningViewCls(void)
{
  return DimensioningViewComponentView.class;
}

#endif // RCT_NEW_ARCH_ENABLED
