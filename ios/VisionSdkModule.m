//
//  VisionSdkModule.m
//  VisionSdk
//
//  Created by Intellijust on 27/02/2025.
//  Copyright © 2025 Facebook. All rights reserved.
//

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
@end
