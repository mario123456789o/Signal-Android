package org.thoughtcrime.securesms.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.SurfaceView;

import java.io.IOException;

public interface SignalCamera {

  @SuppressLint("MissingPermission")
  static SignalCamera get(@NonNull Context context, @NonNull EventListener eventListener) {
//    if (camera2Supported()) {
//      try {
//        return new SignalCamera2(context);
//      } catch (CameraUnavailableException e) {
//        e.printStackTrace();
//      }
//    }
    return new SignalCamera1(eventListener);
  }

  void initialize();

  void release();

  void linkSurface(@NonNull SurfaceView surface) throws IOException;

  void setScreenRotation(int rotation);

  void capture(@NonNull CaptureCompleteCallback callback);

  void flip();

  static boolean camera2Supported() {
    return Build.VERSION.SDK_INT >= 21;
  }

  interface CaptureCompleteCallback {
    void onComplete(@NonNull byte[] data, int rotation);
  }

  class CameraUnavailableException extends Exception {
    CameraUnavailableException(Exception e) {
      super(e);
    }
  }

  class Capabilities {

    final int cameraCount;

    public Capabilities(int cameraCount) {
      this.cameraCount = cameraCount;
    }

    int getCameraCount() {
      return cameraCount;
    }
  }

  interface EventListener {
    void onCameraUnavailable();
    void onCapabilitiesAvailable(@NonNull Capabilities capabilities);
  }
}
