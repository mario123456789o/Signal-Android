package org.thoughtcrime.securesms.camera;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.TextureView;

import org.thoughtcrime.securesms.logging.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SignalCamera1 implements SignalCamera {

  private static final String TAG = SignalCamera1.class.getSimpleName();

  private Camera               camera;
  private int                  cameraId;
  private OrderEnforcer<Stage> enforcer;
  private EventListener        eventListener;
  private SurfaceTexture       previewSurface;
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

    camera = Camera.open(cameraId);


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
  public void linkSurface(@NonNull SurfaceTexture surfaceTexture) {
    enforcer.run(Stage.INITIALIZED, () -> {
      try {
        previewSurface = surfaceTexture;

        Camera.Parameters params  = camera.getParameters();
        Camera.Size       maxSize = getMaxSupportedPreviewSize(camera);

        params.setPreviewSize(maxSize.width, maxSize.height);
        camera.setParameters(params);

        camera.setPreviewTexture(surfaceTexture);
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
      camera.takePicture(null, null, (bytes, camera) -> {
        Camera.Size       previewSize = camera.getParameters().getPictureSize();
        Camera.CameraInfo info        = new Camera.CameraInfo();

        Camera.getCameraInfo(cameraId, info);

        callback.onComplete(bytes, previewSize.width, previewSize.height, info.orientation);
      });
    });
  }

  @Override
  public void flip() {
    SurfaceTexture surfaceTexture = previewSurface;
    cameraId = (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

    release();
    initialize();
    linkSurface(surfaceTexture);
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
    Camera.Size previewSize = camera.getParameters().getPreviewSize();
    return new Capabilities(Camera.getNumberOfCameras(), previewSize.width, previewSize.height);
  }

  private Camera.Size getMaxSupportedPreviewSize(Camera camera) {
    List<Camera.Size> cameraSizes = camera.getParameters().getSupportedPreviewSizes();
    Collections.sort(cameraSizes, DESC_SIZE_COMPARATOR);
    return cameraSizes.get(0);
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

  private final Comparator<Camera.Size> DESC_SIZE_COMPARATOR = (o1, o2) -> Integer.compare(o2.width * o2.height, o1.width * o1.height);

  private enum Stage {
    INITIALIZED, PREVIEW_STARTED
  }
}
