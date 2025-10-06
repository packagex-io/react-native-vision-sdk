import { ReactNode } from 'react';
import { StyleProp, ViewStyle } from 'react-native';

/**
 * Camera scan mode types
 */
export type VisionCameraScanMode =
  | 'photo'
  | 'barcode'
  | 'qrcode'
  | 'barcodeorqrcode'
  | 'ocr'
  | 'barcodesinglecapture';

/**
 * Event triggered when an image is captured by the camera.
 */
export interface VisionCameraCaptureEvent {
  /**
   * @type {string}
   * @description Path to the captured image.
   */
  image: string;

  /**
   * @type {string | undefined}
   * @description Optional native image URI if available.
   */
  nativeImage?: string;
}

/**
 * Represents an error event in the Vision Camera.
 */
export interface VisionCameraErrorResult {
  /**
   * @type {string}
   * @description A description of the error that occurred.
   */
  message: string;
}

/**
 * Event triggered continuously with recognition updates from the camera feed.
 */
export interface VisionCameraRecognitionUpdateEvent {
  /**
   * @type {boolean}
   * @description Whether text is detected in the viewfinder.
   */
  text: boolean;

  /**
   * @type {boolean}
   * @description Whether a barcode is detected in the viewfinder.
   */
  barcode: boolean;

  /**
   * @type {boolean}
   * @description Whether a QR code is detected in the viewfinder.
   */
  qrcode: boolean;

  /**
   * @type {boolean}
   * @description Whether a document is detected in the viewfinder.
   */
  document: boolean;
}

/**
 * Event triggered with image sharpness score from the camera feed.
 */
export interface VisionCameraSharpnessScoreEvent {
  /**
   * @type {number}
   * @description The sharpness score of the current camera feed image.
   */
  sharpnessScore: number;
}

/**
 * Represents a single detected barcode result.
 */
export interface VisionCameraBarcodeResult {
  /**
   * @type {string}
   * @description The scanned barcode value.
   */
  scannedCode: string;

  /**
   * @type {string}
   * @description The barcode symbology type (e.g., QR, EAN, UPC, Code128).
   */
  symbology: string;

  /**
   * @type {object}
   * @description Bounding box coordinates of the detected barcode.
   */
  boundingBox: {
    x: number;
    y: number;
    width: number;
    height: number;
  };

  /**
   * @type {Record<string, string> | undefined}
   * @description Additional GS1 extracted information as key-value pairs (if available).
   */
  gs1ExtractedInfo?: Record<string, string>;
}

/**
 * Event triggered when barcodes (including QR codes) are detected.
 */
export interface VisionCameraBarcodeDetectedEvent {
  /**
   * @type {VisionCameraBarcodeResult[]}
   * @description Array of detected barcode results.
   */
  codes: VisionCameraBarcodeResult[];
}

/**
 * Props for the Vision Camera view component.
 */
export interface VisionCameraViewProps {
  /**
   * @optional
   * @type {ReactNode}
   * @description Optional children elements to render inside the camera view.
   */
  children?: ReactNode;

  /**
   * @optional
   * @type {StyleProp<ViewStyle>}
   * @description Optional style to apply to the container.
   */
  style?: StyleProp<ViewStyle>;

  /**
   * @optional
   * @type {boolean}
   * @description Optional flag to enable or disable flash for capturing.
   */
  enableFlash?: boolean;

  /**
   * @optional
   * @type {number}
   * @description Optional zoom level for the camera.
   */
  zoomLevel?: number;

  /**
   * @optional
   * @type {VisionCameraScanMode}
   * @description Camera scan mode: 'photo', 'barcode', 'qrcode', 'barcodeorqrcode', 'ocr', 'barcodesinglecapture'.
   * @default 'photo'
   */
  scanMode?: VisionCameraScanMode;

  /**
   * @optional
   * @type {boolean}
   * @description Enable automatic capture when document is detected (mainly used with OCR mode).
   * @default false
   */
  autoCapture?: boolean;

  /**
   * @optional
   * @param {VisionCameraCaptureEvent} event
   * @type {(event: VisionCameraCaptureEvent) => void | undefined}
   * @description Event handler for image capture events.
   */
  onCapture?: (event: VisionCameraCaptureEvent) => void;

  /**
   * @optional
   * @param {VisionCameraErrorResult} event
   * @type {(event: VisionCameraErrorResult) => void | undefined}
   * @description Event handler for error events.
   */
  onError?: (event: VisionCameraErrorResult) => void;

  /**
   * @optional
   * @param {VisionCameraRecognitionUpdateEvent} event
   * @type {(event: VisionCameraRecognitionUpdateEvent) => void | undefined}
   * @description Event handler for continuous recognition updates from the camera feed.
   * Reports what objects (text, barcode, qrcode, document) are detected in the viewfinder.
   */
  onRecognitionUpdate?: (event: VisionCameraRecognitionUpdateEvent) => void;

  /**
   * @optional
   * @param {VisionCameraSharpnessScoreEvent} event
   * @type {(event: VisionCameraSharpnessScoreEvent) => void | undefined}
   * @description Event handler for continuous sharpness score updates from the camera feed.
   */
  onSharpnessScoreUpdate?: (event: VisionCameraSharpnessScoreEvent) => void;

  /**
   * @optional
   * @param {VisionCameraBarcodeDetectedEvent} event
   * @type {(event: VisionCameraBarcodeDetectedEvent) => void | undefined}
   * @description Event handler for barcode/QR code detection events.
   * Triggered when barcodes or QR codes are detected in scan modes: barcode, qrcode, barcodeorqrcode, barcodesinglecapture.
   */
  onBarcodeDetected?: (event: VisionCameraBarcodeDetectedEvent) => void;

  /**
   * @optional
   * @type {React.Ref<any>}
   * @description Optional reference to the component.
   */
  ref?: React.Ref<any>;
}

/**
 * Exposes methods to control the Vision Camera from the parent component.
 */
export interface VisionCameraRefProps {
  /**
   * Captures an image using the camera.
   * @description This method triggers the camera to capture an image.
   */
  capture: () => void;

  /**
   * Stops the camera.
   * @description This method stops the camera preview.
   */
  stop: () => void;

  /**
   * Starts the camera.
   * @description This method starts the camera preview.
   */
  start: () => void;

  /**
   * Toggles the flash mode.
   * @param {boolean} enabled - Whether flash should be enabled.
   */
  toggleFlash: (enabled: boolean) => void;

  /**
   * Sets the zoom level.
   * @param {number} level - The zoom level to set.
   */
  setZoom: (level: number) => void;
}

/**
 * Props for the Vision Camera component.
 */
export interface VisionCameraProps {
  /**
   * @optional
   * @type {ReactNode | undefined}
   * @description Optional children elements to render inside the camera component.
   */
  children?: ReactNode;

  /**
   * @optional
   * @type {React.Ref<any> | undefined}
   * @description Optional reference to the component.
   */
  refProp?: React.Ref<any>;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description Optional flag to enable or disable flash for capturing.
   */
  enableFlash?: boolean;

  /**
   * @optional
   * @type {number | undefined}
   * @description Optional zoom level for the camera.
   */
  zoomLevel?: number;

  /**
   * @optional
   * @type {VisionCameraScanMode | undefined}
   * @description Camera scan mode: 'photo', 'barcode', 'qrcode', 'barcodeorqrcode', 'ocr', 'barcodesinglecapture'.
   * @default 'photo'
   */
  scanMode?: VisionCameraScanMode;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description Enable automatic capture when document is detected (mainly used with OCR mode).
   * @default false
   */
  autoCapture?: boolean;

  /**
   * @optional
   * @param {VisionCameraCaptureEvent} event
   * @type {(event: VisionCameraCaptureEvent) => void | undefined}
   * @description Event handler for image capture events.
   */
  onCapture?: (event: VisionCameraCaptureEvent) => void;

  /**
   * @optional
   * @param {VisionCameraErrorResult} event
   * @type {(event: VisionCameraErrorResult) => void | undefined}
   * @description Event handler for error events.
   */
  onError?: (event: VisionCameraErrorResult) => void;

  /**
   * @optional
   * @param {VisionCameraRecognitionUpdateEvent} event
   * @type {(event: VisionCameraRecognitionUpdateEvent) => void | undefined}
   * @description Event handler for continuous recognition updates from the camera feed.
   * Reports what objects (text, barcode, qrcode, document) are detected in the viewfinder.
   */
  onRecognitionUpdate?: (event: VisionCameraRecognitionUpdateEvent) => void;

  /**
   * @optional
   * @param {VisionCameraSharpnessScoreEvent} event
   * @type {(event: VisionCameraSharpnessScoreEvent) => void | undefined}
   * @description Event handler for continuous sharpness score updates from the camera feed.
   */
  onSharpnessScoreUpdate?: (event: VisionCameraSharpnessScoreEvent) => void;

  /**
   * @optional
   * @param {VisionCameraBarcodeDetectedEvent} event
   * @type {(event: VisionCameraBarcodeDetectedEvent) => void | undefined}
   * @description Event handler for barcode/QR code detection events.
   * Triggered when barcodes or QR codes are detected in scan modes: barcode, qrcode, barcodeorqrcode, barcodesinglecapture.
   */
  onBarcodeDetected?: (event: VisionCameraBarcodeDetectedEvent) => void;
}
