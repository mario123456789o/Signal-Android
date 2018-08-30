package org.thoughtcrime.securesms.scribbles;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.TransportOption;
import org.thoughtcrime.securesms.components.ComposeText;
import org.thoughtcrime.securesms.components.SendButton;
import org.thoughtcrime.securesms.scribbles.widget.ColorPaletteAdapter;
import org.thoughtcrime.securesms.scribbles.widget.VerticalSlideColorPicker;
import org.thoughtcrime.securesms.util.CharacterCalculator.CharacterState;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.Locale;
import java.util.Set;

/**
 * The HUD (heads-up display) that contains all of the tools for interacting with
 * {@link org.thoughtcrime.securesms.scribbles.widget.ScribbleView}
 */
public class ScribbleHud extends RelativeLayout {

  private View                     drawButton;
  private View                     highlightButton;
  private View                     textButton;
  private View                     stickerButton;
  private View                     undoButton;
  private View                     deleteButton;
  private View                     saveButton;
  private VerticalSlideColorPicker colorPicker;
  private RecyclerView             colorPalette;
  private ViewGroup                inputContainer;
  private ComposeText              composeText;
  private SendButton               sendButton;
  private ViewGroup                sendButtonBkg;
  private TextView                 charactersLeft;

  private EventListener       eventListener;
  private ColorPaletteAdapter colorPaletteAdapter;

  public ScribbleHud(@NonNull Context context) {
    super(context);
    initialize();
  }

  public ScribbleHud(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public ScribbleHud(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  private void initialize() {
    inflate(getContext(), R.layout.scribble_hud, this);

    drawButton      = findViewById(R.id.scribble_draw_button);
    highlightButton = findViewById(R.id.scribble_highlight_button);
    textButton      = findViewById(R.id.scribble_text_button);
    stickerButton   = findViewById(R.id.scribble_sticker_button);
    undoButton      = findViewById(R.id.scribble_undo_button);
    deleteButton    = findViewById(R.id.scribble_delete_button);
    saveButton      = findViewById(R.id.scribble_save_button);
    colorPicker     = findViewById(R.id.scribble_color_picker);
    colorPalette    = findViewById(R.id.scribble_color_palette);
    inputContainer  = findViewById(R.id.scribble_compose_container);
    composeText     = findViewById(R.id.scribble_compose_text);
    sendButton      = findViewById(R.id.scribble_send_button);
    sendButtonBkg   = findViewById(R.id.scribble_send_button_bkg);
    charactersLeft  = findViewById(R.id.scribble_characters_left);

    undoButton.setOnClickListener(v -> {
      if (eventListener != null) {
        eventListener.onUndo();
      }
    });

    deleteButton.setOnClickListener(v -> {
      if (eventListener != null) {
        eventListener.onDelete();
      }
      setMode(Mode.NONE);
    });

    saveButton.setOnClickListener(v -> {
      if (eventListener != null) {
        eventListener.onEditComplete(Optional.absent(), false);
      }
      setMode(Mode.NONE);
    });

    colorPaletteAdapter = new ColorPaletteAdapter();
    colorPaletteAdapter.setEventListener(colorPicker::setActiveColor);

    colorPalette.setLayoutManager(new LinearLayoutManager(getContext()));
    colorPalette.setAdapter(colorPaletteAdapter);

    sendButton.addOnTransportChangedListener((newTransport, manuallySelected) -> {
      presentCharactersRemaining();
      composeText.setTransport(newTransport);
      sendButtonBkg.getBackground().setColorFilter(newTransport.getBackgroundColor(), PorterDuff.Mode.MULTIPLY);
      sendButtonBkg.getBackground().invalidateSelf();
//      if (manuallySelected) recordSubscriptionIdPreference(newTransport.getSimSubscriptionId());
    });

    sendButton.setOnClickListener(v -> {
      if (eventListener != null) {
        eventListener.onEditComplete(Optional.of(composeText.getTextTrimmed()), !sendButton.getSelectedTransport().isSms());
      }
      setMode(Mode.NONE);
    });

    setMode(Mode.NONE);
    setIsSendMode(ScribbleComposeMode.NONE);
  }

  public void setIsSendMode(ScribbleComposeMode composeMode) {
    switch (composeMode) {
      case NONE:
    }
    if (composeMode == ScribbleComposeMode.NONE) {
      saveButton.setVisibility(VISIBLE);
      inputContainer.setVisibility(GONE);
    } else {
      saveButton.setVisibility(GONE);
      inputContainer.setVisibility(VISIBLE);

      if (composeMode == ScribbleComposeMode.PUSH) {
        sendButton.setDefaultTransport(TransportOption.Type.TEXTSECURE);
      } else {
        sendButton.setDefaultTransport(TransportOption.Type.SMS);
      }
    }
  }

  public void setColorPalette(@NonNull Set<Integer> colors) {
    colorPaletteAdapter.setColors(colors);
  }

  public void enterMode(@NonNull Mode mode) {
    setMode(mode, false);
  }

  private void setMode(@NonNull Mode mode) {
    setMode(mode, true);
  }

  private void setMode(@NonNull Mode mode, boolean notify) {
    switch (mode) {
      case NONE:      presentModeNone();      break;
      case DRAW:      presentModeDraw();      break;
      case HIGHLIGHT: presentModeHighlight(); break;
      case TEXT:      presentModeText();      break;
      case STICKER:   presentModeSticker();   break;
    }

    if (notify && eventListener != null) {
      eventListener.onModeStarted(mode);
    }
  }

  private void presentModeNone() {
    drawButton.setVisibility(VISIBLE);
    highlightButton.setVisibility(VISIBLE);
    textButton.setVisibility(VISIBLE);
    stickerButton.setVisibility(VISIBLE);

    undoButton.setVisibility(GONE);
    deleteButton.setVisibility(GONE);
    colorPicker.setVisibility(GONE);
    colorPalette.setVisibility(GONE);

    drawButton.setOnClickListener(v -> setMode(Mode.DRAW));
    highlightButton.setOnClickListener(v -> setMode(Mode.HIGHLIGHT));
    textButton.setOnClickListener(v -> setMode(Mode.TEXT));
    stickerButton.setOnClickListener(v -> setMode(Mode.STICKER));
  }

  private void presentModeDraw() {
    drawButton.setVisibility(VISIBLE);
    undoButton.setVisibility(VISIBLE);
    colorPicker.setVisibility(VISIBLE);
    colorPalette.setVisibility(VISIBLE);

    highlightButton.setVisibility(GONE);
    textButton.setVisibility(GONE);
    stickerButton.setVisibility(GONE);
    deleteButton.setVisibility(GONE);

    drawButton.setOnClickListener(v -> setMode(Mode.NONE));

    colorPicker.setOnColorChangeListener(standardOnColorChangeListener);
    colorPicker.setActiveColor(Color.RED);
  }

  private void presentModeHighlight() {
    highlightButton.setVisibility(VISIBLE);
    undoButton.setVisibility(VISIBLE);
    colorPicker.setVisibility(VISIBLE);
    colorPalette.setVisibility(VISIBLE);

    drawButton.setVisibility(GONE);
    textButton.setVisibility(GONE);
    stickerButton.setVisibility(GONE);
    deleteButton.setVisibility(GONE);

    highlightButton.setOnClickListener(v -> setMode(Mode.NONE));

    colorPicker.setOnColorChangeListener(highlightOnColorChangeListener);
    colorPicker.setActiveColor(Color.YELLOW);
  }

  private void presentModeText() {
    textButton.setVisibility(VISIBLE);
    deleteButton.setVisibility(VISIBLE);
    colorPicker.setVisibility(VISIBLE);
    colorPalette.setVisibility(VISIBLE);

    drawButton.setVisibility(GONE);
    highlightButton.setVisibility(GONE);
    stickerButton.setVisibility(GONE);
    undoButton.setVisibility(GONE);

    textButton.setOnClickListener(v -> setMode(Mode.NONE));

    colorPicker.setOnColorChangeListener(standardOnColorChangeListener);
    colorPicker.setActiveColor(Color.WHITE);
  }

  private void presentModeSticker() {
    stickerButton.setVisibility(VISIBLE);
    deleteButton.setVisibility(VISIBLE);

    drawButton.setVisibility(GONE);
    highlightButton.setVisibility(GONE);
    textButton.setVisibility(GONE);
    undoButton.setVisibility(GONE);
    colorPicker.setVisibility(GONE);
    colorPalette.setVisibility(GONE);

    stickerButton.setOnClickListener(v -> setMode(Mode.NONE));
  }

  private void presentCharactersRemaining() {
    String          messageBody     = composeText.getTextTrimmed();
    TransportOption transportOption = sendButton.getSelectedTransport();
    CharacterState  characterState  = transportOption.calculateCharacters(messageBody);

    if (characterState.charactersRemaining <= 15 || characterState.messagesSpent > 1) {
      // TODO: Get actual locale
      charactersLeft.setText(String.format(Locale.getDefault(),
                                           "%d/%d (%d)",
                                           characterState.charactersRemaining,
                                           characterState.maxMessageSize,
                                           characterState.messagesSpent));
      charactersLeft.setVisibility(View.VISIBLE);
    } else {
      charactersLeft.setVisibility(View.GONE);
    }
  }

  public int getActiveColor() {
    return colorPicker.getActiveColor();
  }

  public void setActiveColor(int color) {
    colorPicker.setActiveColor(color);
  }

  public void setEventListener(@Nullable EventListener eventListener) {
    this.eventListener = eventListener;
  }

  private final VerticalSlideColorPicker.OnColorChangeListener standardOnColorChangeListener = new VerticalSlideColorPicker.OnColorChangeListener() {
    @Override
    public void onColorChange(int selectedColor) {
      if (eventListener != null) {
        eventListener.onColorChange(selectedColor);
      }
    }
  };

  private final VerticalSlideColorPicker.OnColorChangeListener highlightOnColorChangeListener = new VerticalSlideColorPicker.OnColorChangeListener() {
    @Override
    public void onColorChange(int selectedColor) {
      if (eventListener != null) {
        int r = Color.red(selectedColor);
        int g = Color.green(selectedColor);
        int b = Color.blue(selectedColor);
        int a = 128;

        eventListener.onColorChange(Color.argb(a, r, g, b));
      }
    }
  };

  public enum Mode {
    NONE, DRAW, HIGHLIGHT, TEXT, STICKER
  }

  public interface EventListener {
    void onModeStarted(@NonNull Mode mode);
    void onColorChange(int color);
    void onUndo();
    void onDelete();
    void onEditComplete(@NonNull Optional<String> message, boolean isPush);
  }
}
