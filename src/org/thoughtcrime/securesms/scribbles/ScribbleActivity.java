package org.thoughtcrime.securesms.scribbles;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ScribbleActivity extends PassphraseRequiredActionBarActivity implements ScribbleFragment.Controller {

  private static final String TAG = ScribbleActivity.class.getName();

  public static final int SCRIBBLE_REQUEST_CODE       = 31424;

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    setContentView(R.layout.scribble_activity);

    if (savedInstanceState == null) {
      ScribbleFragment fragment = ScribbleFragment.newInstance(getIntent().getData());
      getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
    }

    if (Build.VERSION.SDK_INT >= 19) {
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                                                       View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
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
    Toast.makeText(ScribbleActivity.this, R.string.ScribbleActivity_save_failure, Toast.LENGTH_SHORT).show();
    finish();
  }
}
