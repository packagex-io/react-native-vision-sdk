// Mock the native TurboModule before importing anything that uses it
jest.mock('../specs/NativeVisionSdkModule', () => ({
  __esModule: true,
  default: {
    getCameraCapabilities: jest.fn(),
    addListener: jest.fn(),
    removeListeners: jest.fn(),
  },
}));

import { VisionCore } from '../VisionCoreWrapper';
import NativeVisionSdkModule from '../specs/NativeVisionSdkModule';

describe('VisionCore.getCameraCapabilities', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('parses the native JSON response into a typed CameraCapabilities object', async () => {
    const capabilities = {
      lenses: [
        {
          id: 'back-wide',
          kind: 'wide',
          facing: 'back',
          minZoomRatio: 1,
          maxZoomRatio: 10,
          zoomSwitchPoints: [2, 5],
          hasFlash: true,
          isLogical: false,
          isPinnable: true,
        },
      ],
      zoomStops: { back: [1, 2, 5], front: [1] },
      hasTorch: { back: true, front: false },
      supportsFocusPoint: { back: true, front: true },
    };
    (NativeVisionSdkModule.getCameraCapabilities as jest.Mock).mockResolvedValue(
      JSON.stringify(capabilities)
    );

    const result = await VisionCore.getCameraCapabilities();

    expect(result).toEqual(capabilities);
  });

  it('rejects with a clear message when the native response is not valid JSON', async () => {
    (NativeVisionSdkModule.getCameraCapabilities as jest.Mock).mockResolvedValue(
      'not valid json{{{'
    );

    await expect(VisionCore.getCameraCapabilities()).rejects.toThrow(
      'VisionCore.getCameraCapabilities'
    );
  });
});
