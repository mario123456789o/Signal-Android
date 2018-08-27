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
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.GlideDrawableListeningTarget;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.scribbles.ScribbleFragment;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.concurrent.AssertedSuccessListener;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;

import java.io.IOException;

// TODO: Extend correct superclass?
public class CameraActivity extends AppCompatActivity implements CameraFragment.Controller,
                                                                 ScribbleFragment.Controller {

  private ImageView snapshot;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.camera_activity);

    snapshot = findViewById(R.id.camera_snapshot);

    if (savedInstanceState == null) {
      CameraFragment fragment = CameraFragment.newInstance();
      getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
    }
  }

  @Override
  public void onCameraError() {
    // TODO: Localize string
    Toast.makeText(this, "Camera unavailable.", Toast.LENGTH_SHORT).show();
    setResult(RESULT_CANCELED, new Intent());
    finish();
  }

  @Override
  public void onImageCaptured(@NonNull Uri uri) {
    GlideApp.with(this).load(new DecryptableStreamUriLoader.DecryptableUri(uri)).into(snapshot);
    ScribbleFragment fragment = ScribbleFragment.newInstance(uri);
    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
  }

  @Override
  public int getDisplayRotation() {
    return getWindowManager().getDefaultDisplay().getRotation();
  }

  @Override
  public void onImageSaveSuccess(@NonNull Uri uri) {
    Intent intent = new Intent();
    intent.setData(uri);
    setResult(RESULT_OK, intent);
    finish();
  }

  @Override
  public void onImageSaveFailure() {
    // TODO: Localize string
    Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show();
    finish();
  }
}
