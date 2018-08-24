package org.thoughtcrime.securesms.camera;

import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.SurfaceView;

import org.thoughtcrime.securesms.logging.Log;

import java.io.IOException;

public class SignalCamera1 implements SignalCamera {

  private static final String TAG = SignalCamera1.class.getSimpleName();

  private Camera               camera;
  private int                  cameraId;
  private OrderEnforcer<Stage> enforcer;
  private EventListener        eventListener;
  private SurfaceView          previewSurface;
  private int                  screenRotation;

  public SignalCamera1(@NonNull EventListener eventListener) {
    this.eventListener = eventListener;
    this.enforcer      = new OrderEnforcer<>(Stage.INITIALIZED, Stage.PREVIEW_STARTED);
    this.cameraId      = Camera.CameraInfo.CAMERA_FACING_BACK;
  }

  @Override
  public void initialize() {
    if (Camera.getNumberOfCameras() <= 0) {
      onCameraUnavailable();
    }

    camera   = Camera.open(cameraId);

    enforcer.markCompleted(Stage.INITIALIZED);
    eventListener.onCapabilitiesAvailable(getCapabilities());
  }

  @Override
  public void release() {
    enforcer.run(Stage.PREVIEW_STARTED, () -> {
      previewSurface = null;
      camera.stopPreview();
      camera.release();
      enforcer.reset();
    });
  }

  @Override
  public void linkSurface(@NonNull SurfaceView surface) {
    enforcer.run(Stage.INITIALIZED, () -> {
      try {
        previewSurface = surface;

        camera.setPreviewDisplay(surface.getHolder());
        camera.startPreview();
        enforcer.markCompleted(Stage.PREVIEW_STARTED);
      } catch (IOException e) {
        Log.e(TAG, "Failed to start preview.", e);
        eventListener.onCameraUnavailable();
      }
    });
  }

  @Override
  public void capture(@NonNull CaptureCompleteCallback callback) {
    enforcer.run(Stage.PREVIEW_STARTED, () -> {
      camera.takePicture(null, null, null, (bytes, camera) -> {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        callback.onComplete(bytes, info.orientation);
      });
    });
  }

  @Override
  public void flip() {
    SurfaceView surfaceView = previewSurface;
    cameraId = (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

    release();
    initialize();
    linkSurface(surfaceView);
    setScreenRotation(screenRotation);
  }

  @Override
  public void setScreenRotation(int screenRotation) {
    enforcer.run(Stage.PREVIEW_STARTED, () -> {
      this.screenRotation = screenRotation;

      int rotation = getCameraRotationForScreen(screenRotation);
      camera.setDisplayOrientation(rotation);

      Camera.Parameters params = camera.getParameters();
      params.setRotation(rotation);
      camera.setParameters(params);
    });
  }

  private void onCameraUnavailable() {
    enforcer.reset();
    eventListener.onCameraUnavailable();
  }

  private Capabilities getCapabilities() {
    return new Capabilities(Camera.getNumberOfCameras());
  }

  private int getCameraRotationForScreen(int screenRotation) {
    int degrees = 0;

    switch (screenRotation) {
      case Surface.ROTATION_0:   degrees = 0;   break;
      case Surface.ROTATION_90:  degrees = 90;  break;
      case Surface.ROTATION_180: degrees = 180; break;
      case Surface.ROTATION_270: degrees = 270; break;
    }

    Camera.CameraInfo info = new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, info);

    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      return (360 - ((info.orientation + degrees) % 360)) % 360;
    } else {
      return (info.orientation - degrees + 360) % 360;
    }
  }

  private enum Stage {
    INITIALIZED, PREVIEW_STARTED
  }
}
