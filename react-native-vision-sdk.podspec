require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-vision-sdk"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  # Core scanner and OCR support iOS 16+. LiDAR Dimensioning (iOS 17+) has
  # been split into the separate `react-native-vision-sdk-dimensioning` pod.
  s.platforms    = { :ios => "16.0" }
  s.source       = { :git => "https://github.com/packagex-io/react-native-vision-sdk.git", :tag => "v#{s.version}" }

  # Explicitly set module name to ensure Swift bridging header matches
  s.module_name  = "react_native_vision_sdk"

  # Static framework configuration for Swift/ObjC interop
  s.static_framework = true

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
  # Core scanner + OCR. Kept in lockstep with react-native-vision-sdk-dimensioning's
  # VisionSDK pin so the two packages can be co-installed without a version conflict.
  s.dependency "VisionSDK", "= 2.3.2"

  # New Architecture dependencies
  install_modules_dependencies(s)
end
