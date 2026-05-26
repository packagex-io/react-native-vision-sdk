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
