import Foundation
import AVFoundation

@available(iOS 13.0, *)
@objc(VisionCameraViewManager)
class VisionCameraViewManager: RCTViewManager {

    @objc func getComponent(_ node: NSNumber, completion: @escaping (RNVisionCameraView?) -> Void) {
        DispatchQueue.main.async {
            if let component = self.bridge.uiManager.view(forReactTag: node) as? RNVisionCameraView {
                completion(component)
            } else {
                completion(nil)
            }
        }
    }

    @objc func capture(_ node: NSNumber) {
        getComponent(node) { component in
            component?.capture()
        }
    }

    @objc func stop(_ node: NSNumber) {
        getComponent(node) { component in
            component?.stop()
        }
    }

    @objc func start(_ node: NSNumber) {
        getComponent(node) { component in
            component?.start()
        }
    }

    @objc func toggleFlash(_ node: NSNumber, enabled: Bool) {
        getComponent(node) { component in
            component?.toggleFlash(enabled: enabled)
        }
    }

    @objc func setZoom(_ node: NSNumber, level: NSNumber) {
        getComponent(node) { component in
            component?.setZoom(level: CGFloat(level.floatValue))
        }
    }

    override func view() -> UIView! {
        return RNVisionCameraView()
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}
