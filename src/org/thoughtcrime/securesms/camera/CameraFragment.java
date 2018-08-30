package org.thoughtcrime.securesms.camera;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.logging.Log;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Stopwatch;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.concurrent.LifecycleBoundTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CameraFragment extends Fragment implements TextureView.SurfaceTextureListener,
                                                        SignalCamera.EventListener
{

  private static final String TAG = CameraFragment.class.getSimpleName();

  private TextureView          cameraPreview;
  private ImageButton          flipButton;
  private SignalCamera         camera;
  private Controller           controller;
  private OrderEnforcer<Stage> orderEnforcer;

  public static CameraFragment newInstance() {
    return new CameraFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!(getActivity() instanceof Controller)) {
      throw new IllegalStateException("Parent activity must implement the Controller interface.");
    }

    controller    = (Controller) getActivity();
    camera        = SignalCamera.get(getContext(), TextSecurePreferences.getDirectCaptureCameraId(getContext()), this);
    orderEnforcer = new OrderEnforcer<>(Stage.VIEW_SIZE_AVAILABLE);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.camera_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    cameraPreview = view.findViewById(R.id.camera_preview);
    flipButton    = view.findViewById(R.id.camera_flip_button);

    cameraPreview.setSurfaceTextureListener(this);

    Button captureButton = view.findViewById(R.id.camera_capture_button);
    captureButton.setOnClickListener(v -> {
      Stopwatch fastCaptureTimer = new Stopwatch("Fast Capture");

      captureButton.setEnabled(false);
      orderEnforcer.reset();

      LifecycleBoundTask.run(getLifecycle(), () -> {
        fastCaptureTimer.split("capture");

        Bitmap preview = cameraPreview.getBitmap();
        fastCaptureTimer.split("preview");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        preview.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        fastCaptureTimer.split("compress");

        fastCaptureTimer.stop(TAG);

        return PersistentBlobProvider.getInstance(getContext()).create(getContext(), stream.toByteArray(), MediaUtil.IMAGE_JPEG, null);
      }, uri -> {
        if (uri != null) {
          controller.onFastImageCaptured(uri);
        } else {
          controller.onCameraError();
        }
      });

      camera.capture((data, width, height, rotation) -> {
        LifecycleBoundTask.run(getLifecycle(), () -> {
          try {
            byte[] processed = processImage(data, width, height);
            return PersistentBlobProvider.getInstance(getContext()).create(getContext(), processed, MediaUtil.IMAGE_JPEG, null);
          } catch (IOException e) {
            Log.e(TAG, "Failed to process image.", e);
          }
          return null;
        }, uri -> {
          if (uri != null) {
            controller.onFullImageCaptured(uri);
          } else {
            controller.onCameraError();
          }
        });
      });
    });

    cameraPreview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        orderEnforcer.markCompleted(Stage.VIEW_SIZE_AVAILABLE);
        cameraPreview.getViewTreeObserver().removeGlobalOnLayoutListener(this);
      }
    });

    GestureDetector gestureDetector = new GestureDetector(gestureListener);
    cameraPreview.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
  }

  @Override
  public void onResume() {
    super.onResume();
    camera.initialize();
  }

  @Override
  public void onPause() {
    super.onPause();
    camera.release();
  }

  @Override
  public void onCapabilitiesAvailable(@NonNull SignalCamera.Capabilities capabilities) {
    if (capabilities.getCameraCount() > 1) {
      flipButton.setVisibility(capabilities.getCameraCount() > 1 ? View.VISIBLE : View.GONE);
      flipButton.setOnClickListener(v ->  {
        int newCameraId = camera.flip();
        flipButton.setImageResource(newCameraId == Camera.CameraInfo.CAMERA_FACING_BACK ? R.drawable.quick_camera_front
                                                                                        : R.drawable.quick_camera_rear);

        TextSecurePreferences.setDirectCaptureCameraId(getContext(), newCameraId);
      });
    } else {
      flipButton.setVisibility(View.GONE);
    }

    orderEnforcer.run(Stage.VIEW_SIZE_AVAILABLE, () -> {
      float camWidth   = isPortrait() ? Math.min(capabilities.previewWidth, capabilities.previewHeight) : Math.max(capabilities.previewWidth, capabilities.previewHeight);
      float camHeight  = isPortrait() ? Math.max(capabilities.previewWidth, capabilities.previewHeight) : Math.min(capabilities.previewWidth, capabilities.previewHeight);
      float viewWidth  = cameraPreview.getWidth();
      float viewHeight = cameraPreview.getHeight();

      float scaleX = 1;
      float scaleY = 1;

      if ((camWidth / viewWidth) > (camWidth / viewWidth)) {
        scaleX = camWidth / viewWidth;
      } else {
        scaleY = camHeight / viewHeight;
      }

      Matrix matrix = new Matrix();
      matrix.setScale(scaleX, scaleY);

      cameraPreview.setTransform(matrix);
    });
  }

  @Override
  public void onCameraUnavailable() {
    controller.onCameraError();
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    try {
      camera.linkSurface(surface);
      camera.setScreenRotation(controller.getDisplayRotation());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {

  }



  private byte[] processImage(@NonNull byte[] imgData, int imgWidth, int imgHeight) throws IOException {
    int surfaceWidth  = cameraPreview.getWidth();
    int surfaceHeight = cameraPreview.getHeight();

//    float widthRatio  = (float) imgWidth / surfaceWidth;
//    float heightRatio = (float) imgHeight / surfaceHeight;
//
//    int targetWidth  = imgWidth;
//    int targetHeight = imgHeight;
//
//    if (widthRatio < heightRatio) {
//      float surfaceRatio = (float) surfaceHeight / surfaceWidth;
//      targetHeight = (int) (targetWidth * surfaceRatio);
//    } else {
//      float surfaceRatio = (float) surfaceWidth / surfaceHeight;
//      targetWidth = (int) (targetHeight * surfaceRatio);
//    }
//
//    Rect rect = new Rect();
//    rect.left   = Math.abs(targetWidth - imgWidth) / 2;
//    rect.right  = imgWidth - Math.abs(targetWidth - imgWidth) / 2;
//    rect.top    = Math.abs(targetHeight - imgHeight) / 2;
//    rect.bottom = imgHeight - Math.abs(targetHeight - imgHeight) / 2;
//
//    return BitmapUtil.createFromNV21(nv21Data, imgWidth, imgHeight, 0, rect, false);

    Bitmap bitmap     = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
    int    jpegWidth  = bitmap.getWidth();
    int    jpegHeight = bitmap.getHeight();

    float widthRatio  = (float) jpegWidth  / surfaceWidth;
    float heightRatio = (float) jpegHeight / surfaceHeight;

    int targetWidth  = jpegWidth;
    int targetHeight = jpegHeight;

    if (widthRatio < heightRatio) {
      float surfaceRatio = (float) surfaceHeight / surfaceWidth;
      targetHeight = (int) (targetWidth * surfaceRatio);
    } else {
      float surfaceRatio = (float) surfaceWidth / surfaceHeight;
      targetWidth = (int) (targetHeight * surfaceRatio);
    }

    // TODO: Right now, our bitmap is larger than necessary. We should be targeting the size of the surface, not the original jpeg.
    Bitmap processed = Bitmap.createBitmap(bitmap,
                                           Math.abs(targetWidth - jpegWidth) / 2,
                                           Math.abs(targetHeight - jpegHeight) / 2,
                                           targetWidth,
                                           targetHeight);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    processed.compress(Bitmap.CompressFormat.JPEG, 85, os);
    return os.toByteArray();
  }

  private boolean isPortrait() {
    return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
  }

  private final GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
    @Override
    public boolean onDown(MotionEvent e) {
      return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
      flipButton.performClick();
      return true;
    }
  };

  public interface Controller {
    void onCameraError();
    void onFastImageCaptured(@NonNull Uri uri);
    void onFullImageCaptured(@NonNull Uri uri);
    int getDisplayRotation();
  }

  private enum Stage {
    VIEW_SIZE_AVAILABLE
  }
}
