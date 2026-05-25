/**
 * Types for the Dimensioning feature.
 * NOTE: Dimensioning is iOS-only. All types are usable cross-platform,
 * but the native view and module only function on iOS 17+ LiDAR devices.
 */

export type DimensioningMode = 'offline' | 'online';

export type DimensioningMeasurementUnit = 'centimeters' | 'inches' | 'meters';

/**
 * Error codes returned by the dimensioning native layer.
 * Values match VSDKDimensioningError ordinals in the iOS SDK.
 */
export enum DimensioningErrorCode {
  MissingCredentials = 0,
  NotConfigured = 1,
  LidarUnavailable = 2,
  ArSessionFailed = 3,
  NoGroundPlane = 4,
  CaptureTimedOut = 5,
  UserCancelled = 6,
  /** Bridge / serialization failure (not from the underlying VSDKDimensioningError). */
  InternalError = 7,
}

/** A single dimension measurement returned by onCapture. */
export interface DimensioningMeasurement {
  id: string;
  timestamp: number; // Unix seconds
  length: number;
  lengthUnit: string;
  width: number;
  widthUnit: string;
  height: number;
  heightUnit: string;
  distanceFromCamera: number;
  distanceFromCameraUnit: string;
  confidence: number; // 0.0 – 1.0
  usedCloudSAM: boolean;
}

export interface DimensioningError {
  code: number;
  message: string;
  reason?: string;
}

/** Returned by VisionDimensioning.deviceCapabilities(). iOS-only. */
export interface DimensioningCapabilities {
  lidar: boolean;
  arWorldTracking: boolean;
  sceneReconstruction: boolean;
}
