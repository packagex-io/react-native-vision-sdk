require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-vision-sdk"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  # Dimensioning subspec requires iOS 17; set deployment target accordingly.
  # The core scanner supports iOS 16 but all devices that run iOS 17 also
  # support running the scanner path, so bumping to 17 is acceptable for
  # the example app. The production podspec can remain at 16 by excluding
  # the Dimensioning files via subspecs (see README).
  s.platforms    = { :ios => "17.0" }
  s.source       = { :git => "https://github.com/packagex-io/react-native-vision-sdk.git", :tag => "#{s.version}" }

  # Explicitly set module name to ensure Swift bridging header matches
  s.module_name  = "react_native_vision_sdk"

  # Static framework configuration for Swift/ObjC interop
  s.static_framework = true

  # All iOS source files including Dimensioning bridge
  s.source_files = "ios/**/*.{h,m,mm,swift}"

  # Compiler flags to ensure Swift bridging header is found
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_OBJC_INTERFACE_HEADER_NAME' => 'react_native_vision_sdk-Swift.h',
    'SWIFT_COMPILATION_MODE' => 'wholemodule',
    'CLANG_ENABLE_MODULES' => 'YES'
  }

  # Preserve paths for tarball installation
  s.preserve_paths = [
    'ios/**/*',
    'src/**/*'
  ]

  s.frameworks = "CoreMotion"

  s.dependency "React-Core"
  # Core scanner + OCR
  s.dependency "VisionSDK", "= 2.3.0"
  # 3-D box measurement — brings MVDimensioningCore.xcframework, ARKit, RealityKit, PostHog
  s.dependency "VisionSDK/Dimensioning", "= 2.3.0"

  # New Architecture dependencies
  install_modules_dependencies(s)
end
