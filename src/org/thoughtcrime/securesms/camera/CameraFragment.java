package org.thoughtcrime.securesms.camera;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.MediaUtil;

import java.io.IOException;

public class CameraFragment extends Fragment implements SurfaceHolder.Callback, SignalCamera.EventListener {

  private SurfaceView  cameraPreview;
  private Button       flipButton;
  private SignalCamera camera;
  private Controller   controller;

  public static CameraFragment newInstance() {
    return new CameraFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!(getActivity() instanceof Controller)) {
      throw new IllegalStateException("Parent activity must implement the Controller interface.");
    }

    controller = (Controller) getActivity();
    camera     = SignalCamera.get(getContext(), this);
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

    cameraPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    cameraPreview.getHolder().addCallback(this);

    Button captureButton = view.findViewById(R.id.camera_capture_button);
    captureButton.setOnClickListener(v -> {
      captureButton.setEnabled(false);
      camera.capture((data, rotation) -> {
        Uri uri = PersistentBlobProvider.getInstance(getContext()).create(getContext(), data, MediaUtil.IMAGE_JPEG, null);
        controller.onImageCaptured(uri);
      });
    });
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
      flipButton.setOnClickListener(v -> camera.flip());
    } else {
      flipButton.setVisibility(View.GONE);
    }
  }

  @Override
  public void onCameraUnavailable() {
    controller.onCameraError();
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    try {
      camera.linkSurface(cameraPreview);
      camera.setScreenRotation(controller.getDisplayRotation());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) { }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) { }

  public interface Controller {
    void onCameraError();
    void onImageCaptured(@NonNull Uri uri);
    int getDisplayRotation();
  }
}
