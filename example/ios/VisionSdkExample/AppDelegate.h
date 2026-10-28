#import <UIKit/UIKit.h>

@class RCTReactNativeFactory;

@interface AppDelegate : UIResponder <UIApplicationDelegate>

// React Native 0.82 (RCTDeviceInfo) reads application.delegate.window.
@property (nonatomic, strong) UIWindow *window;
@property (nonatomic, strong) RCTReactNativeFactory *reactNativeFactory;

@end
