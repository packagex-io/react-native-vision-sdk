import React from 'react';
import { create, act, ReactTestRenderer } from 'react-test-renderer';

// Mock the native component before importing anything that uses it
jest.mock('../specs/VisionCameraViewNativeComponent', () => {
  const { forwardRef } = require('react');
  const MockNativeComponent = forwardRef((props: any, ref: any) => {
    const { View } = require('react-native');
    return <View ref={ref} {...props} />;
  });
  MockNativeComponent.displayName = 'MockVisionCameraViewNative';
  return {
    __esModule: true,
    default: MockNativeComponent,
    Commands: {
      capture: jest.fn(),
      stop: jest.fn(),
      start: jest.fn(),
      toggleFlash: jest.fn(),
      setZoom: jest.fn(),
      setFocusSettings: jest.fn(),
    },
  };
});

import { VisionCamera } from '../VisionCamera';
import { VisionCameraView } from '../VisionCameraViewManager';
import { Commands } from '../specs/VisionCameraViewNativeComponent';

function renderInAct(element: React.ReactElement): ReactTestRenderer {
  let tree: ReactTestRenderer;
  act(() => {
    tree = create(element);
  });
  return tree!;
}

describe('VisionCamera', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('prop defaults', () => {
    it('defaults showNativeBoundingBoxes to false', () => {
      const tree = renderInAct(<VisionCamera />);
      const viewProps = tree.root.findByType(VisionCameraView as any).props;
      expect(viewProps.showNativeBoundingBoxes).toBe(false);
    });

    it('defaults scanMode to photo', () => {
      const tree = renderInAct(<VisionCamera />);
      const viewProps = tree.root.findByType(VisionCameraView as any).props;
      expect(viewProps.scanMode).toBe('photo');
    });

    it('defaults enableFlash to false', () => {
      const tree = renderInAct(<VisionCamera />);
      const viewProps = tree.root.findByType(VisionCameraView as any).props;
      expect(viewProps.enableFlash).toBe(false);
    });

    it('defaults cameraFacing to back', () => {
      const tree = renderInAct(<VisionCamera />);
      const viewProps = tree.root.findByType(VisionCameraView as any).props;
      expect(viewProps.cameraFacing).toBe('back');
    });
  });

  describe('showNativeBoundingBoxes prop', () => {
    it('passes showNativeBoundingBoxes=true to native view', () => {
      const tree = renderInAct(<VisionCamera showNativeBoundingBoxes={true} />);
      const viewProps = tree.root.findByType(VisionCameraView as any).props;
      expect(viewProps.showNativeBoundingBoxes).toBe(true);
    });

    it('passes showNativeBoundingBoxes=true regardless of scanMode', () => {
      const modes = ['photo', 'barcode', 'qrcode', 'ocr', 'barcodeorqrcode'] as const;
      for (const scanMode of modes) {
        const tree = renderInAct(
          <VisionCamera showNativeBoundingBoxes={true} scanMode={scanMode} />
        );
        const viewProps = tree.root.findByType(VisionCameraView as any).props;
        expect(viewProps.showNativeBoundingBoxes).toBe(true);
        expect(viewProps.scanMode).toBe(scanMode);
      }
    });
  });

  describe('onBoundingBoxesUpdate event parsing', () => {
    it('parses barcodeBoundingBoxesJson from native event', () => {
      const mockCallback = jest.fn();
      const bboxData = [
        { scannedCode: '123', symbology: 'CODE128', boundingBox: { x: 10, y: 20, width: 100, height: 50 } },
      ];
      const tree = renderInAct(
        <VisionCamera onBoundingBoxesUpdate={mockCallback} />
      );
      const viewProps = tree.root.findByType(VisionCameraView as any).props;

      act(() => {
        viewProps.onBoundingBoxesUpdate({
          nativeEvent: {
            barcodeBoundingBoxesJson: JSON.stringify(bboxData),
            qrCodeBoundingBoxesJson: '[]',
          },
        });
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
      const result = mockCallback.mock.calls[0][0];
      expect(result.barcodeBoundingBoxes).toEqual(bboxData);
      expect(result.barcodeBoundingBoxesJson).toBeUndefined();
    });

    it('parses qrCodeBoundingBoxesJson from native event', () => {
      const mockCallback = jest.fn();
      const qrData = [
        { scannedCode: 'https://example.com', symbology: 'QR', boundingBox: { x: 5, y: 5, width: 200, height: 200 } },
      ];
      const tree = renderInAct(
        <VisionCamera onBoundingBoxesUpdate={mockCallback} />
      );
      const viewProps = tree.root.findByType(VisionCameraView as any).props;

      act(() => {
        viewProps.onBoundingBoxesUpdate({
          nativeEvent: {
            barcodeBoundingBoxesJson: '[]',
            qrCodeBoundingBoxesJson: JSON.stringify(qrData),
          },
        });
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
      const result = mockCallback.mock.calls[0][0];
      expect(result.qrCodeBoundingBoxes).toEqual(qrData);
      expect(result.qrCodeBoundingBoxesJson).toBeUndefined();
    });

    it('handles invalid JSON gracefully without crashing', () => {
      const mockCallback = jest.fn();
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      const tree = renderInAct(
        <VisionCamera onBoundingBoxesUpdate={mockCallback} />
      );
      const viewProps = tree.root.findByType(VisionCameraView as any).props;

      act(() => {
        viewProps.onBoundingBoxesUpdate({
          nativeEvent: {
            barcodeBoundingBoxesJson: 'not valid json{{{',
            qrCodeBoundingBoxesJson: '[]',
          },
        });
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
      consoleSpy.mockRestore();
    });
  });

  describe('onCapture event parsing', () => {
    it('parses barcodesJson from native capture event', () => {
      const mockCallback = jest.fn();
      const barcodes = [{ code: '123', type: 'CODE128' }];
      const tree = renderInAct(<VisionCamera onCapture={mockCallback} />);
      const viewProps = tree.root.findByType(VisionCameraView as any).props;

      act(() => {
        viewProps.onCapture({
          nativeEvent: {
            image: '/path/to/image.jpg',
            barcodesJson: JSON.stringify(barcodes),
          },
        });
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
      const result = mockCallback.mock.calls[0][0];
      expect(result.barcodes).toEqual(barcodes);
      expect(result.barcodesJson).toBeUndefined();
    });
  });

  describe('ref commands', () => {
    it('setFocusSettings serializes settings to JSON', () => {
      const ref = React.createRef<any>();
      renderInAct(<VisionCamera ref={ref} />);

      const settings = {
        showCodeBoundariesInMultipleScan: true,
        validCodeBoundaryBorderColor: '#FFEB3B',
        validCodeBoundaryBorderWidth: 2,
        validCodeBoundaryFillColor: '#00000000',
      };

      act(() => {
        ref.current.setFocusSettings(settings);
      });

      expect(Commands.setFocusSettings).toHaveBeenCalledWith(
        expect.anything(),
        JSON.stringify(settings)
      );
    });
  });
});

describe('VisionCameraViewManager', () => {
  describe('prop conversion for Fabric', () => {
    it('converts scanArea object to JSON string', () => {
      const scanArea = { x: 0, y: 0, width: 300, height: 200 };
      const tree = renderInAct(
        <VisionCameraView scanArea={scanArea} />
      );
      // Find the mock native component (innermost View)
      const views = tree.root.findAllByType('View' as any);
      const nativeView = views[views.length - 1]!;
      expect(nativeView.props.scanAreaJson).toBe(JSON.stringify(scanArea));
      expect(nativeView.props.scanArea).toBeUndefined();
    });

    it('sets empty string for scanAreaJson when scanArea is undefined', () => {
      const tree = renderInAct(<VisionCameraView />);
      const views = tree.root.findAllByType('View' as any);
      const nativeView = views[views.length - 1]!;
      expect(nativeView.props.scanAreaJson).toBe('');
    });

    it('converts detectionConfig object to JSON string', () => {
      const detectionConfig = { text: true, barcode: true, document: false };
      const tree = renderInAct(
        <VisionCameraView detectionConfig={detectionConfig} />
      );
      const views = tree.root.findAllByType('View' as any);
      const nativeView = views[views.length - 1]!;
      expect(nativeView.props.detectionConfigJson).toBe(JSON.stringify(detectionConfig));
      expect(nativeView.props.detectionConfig).toBeUndefined();
    });

    it('passes showNativeBoundingBoxes through without conversion', () => {
      const tree = renderInAct(
        <VisionCameraView showNativeBoundingBoxes={true} />
      );
      const views = tree.root.findAllByType('View' as any);
      const nativeView = views[views.length - 1]!;
      expect(nativeView.props.showNativeBoundingBoxes).toBe(true);
    });

    it('updates when showNativeBoundingBoxes changes', () => {
      let tree: ReactTestRenderer;
      act(() => {
        tree = create(<VisionCameraView showNativeBoundingBoxes={false} />);
      });

      act(() => {
        tree!.update(<VisionCameraView showNativeBoundingBoxes={true} />);
      });

      const views = tree!.root.findAllByType('View' as any);
      const nativeView = views[views.length - 1]!;
      expect(nativeView.props.showNativeBoundingBoxes).toBe(true);
    });
  });
});
