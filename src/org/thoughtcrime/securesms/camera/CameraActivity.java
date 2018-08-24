package org.thoughtcrime.securesms.camera;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.MediaUtil;

import java.io.IOException;

// TODO: Extend correct superclass?
public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, SignalCamera.EventListener {

  private SurfaceView  cameraPreview;
  private SignalCamera camera;

  private Button flipButton;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_camera);

    cameraPreview = findViewById(R.id.camera_preview);
    camera        = SignalCamera.get(this, this);

    cameraPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    cameraPreview.getHolder().addCallback(this);

    Button captureButton = findViewById(R.id.camera_capture_button);
    captureButton.setOnClickListener(v -> {
      captureButton.setEnabled(false);
      camera.capture((data, rotation) -> {
        Uri uri = PersistentBlobProvider.getInstance(CameraActivity.this).create(CameraActivity.this, data, MediaUtil.IMAGE_JPEG, null);
        Intent intent = new Intent();
        intent.setData(uri);
        setResult(RESULT_OK, intent);
        finish();
      });
    });

    flipButton = findViewById(R.id.camera_flip_button);
  }

  @Override
  protected void onResume() {
    super.onResume();
    camera.initialize();
  }

  @Override
  protected void onPause() {
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
    Toast.makeText(this, "Camera unavailable.", Toast.LENGTH_SHORT).show();
    setResult(RESULT_CANCELED, new Intent());
    finish();
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    try {
      camera.linkSurface(cameraPreview);
      camera.setScreenRotation(getWindowManager().getDefaultDisplay().getRotation());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) { }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) { }
}
