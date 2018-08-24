package org.thoughtcrime.securesms.camera;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.view.SurfaceView;

import java.util.Collections;

@TargetApi(21)
public class SignalCamera2 implements SignalCamera {

  private CameraDevice camera;

  @SuppressLint("MissingPermission")
  @RequiresPermission(android.Manifest.permission.CAMERA)
  public SignalCamera2(@NonNull Context context) throws CameraUnavailableException {
    CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    try {
      cameraManager.openCamera(getCameraId(cameraManager), new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
          SignalCamera2.this.camera = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
      }, null);
    } catch (CameraAccessException e) {
      throw new CameraUnavailableException(e);
    }
  }

  @Override
  public void initialize() {

  }

  @Override
  public void release() {
    camera.close();
  }

  @Override
  public void linkSurface(@NonNull SurfaceView surface) {
    try {
      camera.createCaptureSession(Collections.singletonList(surface.getHolder().getSurface()), new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
          try {
            session.setRepeatingRequest(camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).build(), null, null);
          } catch (CameraAccessException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
      }, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void setScreenRotation(int rotation) {
  }

  @Override
  public void capture(@NonNull CaptureCompleteCallback callback) {

  }

  @Override
  public void flip() {

  }

  private String getCameraId(@NonNull CameraManager cameraManager) {
    try {
      for (String id : cameraManager.getCameraIdList()) {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
        if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
          return id;
        }
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
      return null;
    }
    return null;
  }
}
