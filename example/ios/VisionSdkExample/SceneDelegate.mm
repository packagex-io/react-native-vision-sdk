#import "SceneDelegate.h"

#import "AppDelegate.h"

#import <RCTReactNativeFactory.h>

@implementation SceneDelegate

- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session options:(UISceneConnectionOptions *)connectionOptions
{
  if (![scene isKindOfClass:[UIWindowScene class]]) {
    return;
  }
  AppDelegate *appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
  self.window = [[UIWindow alloc] initWithWindowScene:(UIWindowScene *)scene];
  appDelegate.window = self.window;
  [appDelegate.reactNativeFactory startReactNativeWithModuleName:@"VisionSdkExample"
                                                        inWindow:self.window
                                               initialProperties:@{}
                                                   launchOptions:nil];
}

@end
