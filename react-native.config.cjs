module.exports = {
  dependency: {
    platforms: {
      android: {
        // @react-native-community/cli-config-android's dependencyConfig() falls back to
        // findComponentDescriptors(root), which fast-glob-walks the ENTIRE package root
        // (only node_modules is excluded) looking for codegenNativeComponent() calls.
        // In this repo that root also contains .worktrees/ (gitignored git worktrees used
        // for parallel feature branches), one of which has its own example/node_modules
        // symlink pointing back to its own root — an infinite symlink loop that crashes
        // with ENAMETOOLONG during `pod install` (use_native_modules!) and Xcode's
        // "Bundle React Native code and images" phase, both of which call `react-native config`.
        //
        // Declaring the descriptors explicitly (matching src/specs/*NativeComponent.ts +
        // codegenConfig in package.json) skips that recursive scan entirely.
        componentDescriptors: [
          'VisionCameraViewComponentDescriptor',
          'DimensioningViewComponentDescriptor',
        ],
      },
    },
  },
};
